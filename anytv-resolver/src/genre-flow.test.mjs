import test from "node:test";
import assert from "node:assert/strict";
import { catalogRequest } from "./worker.mjs";

// Read-only execution against the actual Worker handler. Synthetic TMDB
// fixtures only: no account, network, provider credentials, or D1 production.
function fixtureEnv() {
  const rows = new Map();
  const DB = {
    prepare(sql) {
      return {
        bind(...args) {
          return {
            async first() {
              if (!sql.startsWith("SELECT payload")) throw Error("unexpected SELECT");
              return rows.get(args[0]) ?? null;
            },
            async run() {
              if (!sql.startsWith("INSERT INTO link_cache")) throw Error("unexpected INSERT");
              rows.set(args[0], { payload: args[1], expires_at: args[2] });
              return { success: true };
            },
          };
        },
      };
    },
  };
  return { DB, TMDB_READ_TOKEN: "fixture-secret-never-in-client", rows };
}

const testRequest = (media, genre, page) => new Request(
  `https://anytv-fixture.invalid/v1/catalog/discover/${media}?language=pt-BR&include_adult=false&sort_by=popularity.desc&with_genres=${genre}&page=${page}`
);
const resultFixture = (media, genre, page, count = 20) => ({
  page,
  total_pages: 5,
  total_results: 100,
  results: Array.from({length: count}, (_, index) => ({
    id: genre * 100000 + (media === "tv" ? 50000 : 0) + page * 100 + index,
    ...(media === "tv" ? {name: `Serie-${genre}-${page}-${index}`} :
      {title: `Filme-${genre}-${page}-${index}`}),
    // First item intentionally has incomplete metadata: Worker must not filter.
    ...(index === 0 ? {} : {genre_ids: [genre], poster_path: "/poster.jpg",
      release_date: "2020-01-01"}),
    original_language: index === 0 ? "ja" : "pt",
  })),
});

test("Drama/Acao/Anime: Worker executes six genre+media combinations, passes 20 source titles without filtering, pages independently, caches exact requests", async () => {
  const env = fixtureEnv();
  const seen = [];
  const deps = {
    now: () => Date.parse("2026-11-14T12:00:00Z"),
    fetch: async (url, options) => {
      const up = new URL(url);
      seen.push(up);
      assert.equal(up.hostname, "api.themoviedb.org");
      assert.equal(up.searchParams.get("language"), "pt-BR");
      assert.equal(up.searchParams.get("watch_region"), "BR");
      assert.equal(options.headers.Authorization, "Bearer fixture-secret-never-in-client");
      const media = up.pathname.split("/")[3];
      const genre = Number(up.searchParams.get("with_genres"));
      const page = Number(up.searchParams.get("page"));
      return new Response(JSON.stringify(resultFixture(media, genre, page)), {
        status: 200, headers: {"content-type":"application/json"} });
    },
  };
  for (const genre of [18, 28, 16]) {
    for (const media of ["movie", "tv"]) {
      for (const page of [1, 2]) {
        const req = testRequest(media, genre, page);
        const first = await catalogRequest(req, env, deps);
        assert.equal(first.status, 200);
        const body = await first.json();
        assert.equal(body.results.length, 20, `${media}/${genre}/${page}`);
        assert.equal(body.results[0].genre_ids, undefined, "incomplete metadata stays in Worker payload");
        assert.equal(body.page, page);
        const repeat = await catalogRequest(req, env, deps);
        assert.equal(repeat.status, 200);
        assert.equal(repeat.headers.get("x-anytv-cache"), "hit");
        assert.deepEqual(await repeat.json(), body);
      }
    }
  }
  assert.equal(seen.length, 12, "each unique media/genre/page goes upstream once");
  assert.equal(env.rows.size, 12);
  assert.equal(new Set(seen.map(u => u.pathname + u.search)).size, 12);
});

test("Worker retry after failed source page does not poison cache or erase a previous successful page", async () => {
  const env = fixtureEnv();
  let pageTwoAttempts = 0;
  const deps = { now: () => Date.parse("2026-11-14T12:00:00Z"),
    fetch: async url => {
      const up = new URL(url);
      const page = Number(up.searchParams.get("page"));
      if (page === 2 && pageTwoAttempts++ === 0) {
        return new Response("temporary outage", {status: 502});
      }
      return new Response(JSON.stringify(resultFixture("movie", 18, page)), {status: 200});
    },
  };
  const good = await catalogRequest(testRequest("movie", 18, 1), env, deps);
  assert.equal((await good.json()).results.length, 20);
  const failed = await catalogRequest(testRequest("movie", 18, 2), env, deps);
  assert.equal(failed.status, 503);
  assert.deepEqual(await failed.json(), {status:"catalog_unavailable"});
  const recovered = await catalogRequest(testRequest("movie", 18, 2), env, deps);
  assert.equal(recovered.status, 200);
  assert.equal((await recovered.json()).results.length, 20);
  assert.equal(env.rows.size, 2);
});

test("Worker refreshes reduced-but-successful TMDB response only after 24h D1 cache TTL", async () => {
  const env = fixtureEnv();
  let now = Date.parse("2026-11-14T12:00:00Z");
  let upstreamCalls = 0;
  const deps = { now: () => now, fetch: async url => {
    upstreamCalls++;
    const up = new URL(url);
    return new Response(JSON.stringify(resultFixture("movie", 18, Number(up.searchParams.get("page")), upstreamCalls === 1 ? 2 : 20)), {status: 200});
  }};
  const req = testRequest("movie", 18, 1);
  assert.equal((await (await catalogRequest(req, env, deps)).json()).results.length, 2);
  now += 23 * 3600 * 1000;
  const cached = await catalogRequest(req, env, deps);
  assert.equal(cached.headers.get("x-anytv-cache"), "hit");
  assert.equal((await cached.json()).results.length, 2);
  assert.equal(upstreamCalls, 1);
  now += 2 * 3600 * 1000;
  assert.equal((await (await catalogRequest(req, env, deps)).json()).results.length, 20);
  assert.equal(upstreamCalls, 2);
});

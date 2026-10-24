import test from "node:test";
import assert from "node:assert/strict";
import { handleRequest, catalogRequest, inspectOffers, validateLink } from "./worker.mjs";

const FIX = Object.freeze({
  disney: "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185",
  netflix: "https://www.netflix.com/title/80175798",
  prime: "https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef",
  apple: "https://tv.apple.com/br/movie/o-parque-dos-dinossauros/umc.cmc.3bp8rj8ab9otj4ug279xhib2f?at=1000l3V2&ct=app_tvplus",
  hbo: "https://play.hbomax.com/show/a0a085b8-6657-46ca-9476-6af0070b9cce?utm_source=universal_search",
});

function makeEnv() {
  const cache = new Map();
  const db = {
    prepare(sql) {
      return {
        bind(...params) {
          return {
            async first() {
              if (sql.startsWith("SELECT payload")) {
                const hit = cache.get(params[0]);
                return hit ? { ...hit } : null;
              }
              throw Error("Unexpected first query: " + sql);
            },
            async run() {
              if (sql.startsWith("INSERT INTO link_cache")) {
                cache.set(params[0], { payload: params[1], expires_at: params[2] });
                return { success: true };
              }
              throw Error("Unexpected run query: " + sql);
            },
          };
        },
      };
    },
  };
  return {
    ANYTV_CLIENT_TOKEN: "private-pilot-access",
    ANYTV_MOVIE_API_KEY: "never-expose-provider-key",
    DB: db,
    _cache: cache,
  };
}

const request = (path, token = "private-pilot-access") => new Request(
  "https://resolver.example.test" + path,
  { headers: token === null ? {} : { Authorization: "Bearer " + token } }
);
const mockOffer = (provider, link, type = "subscription", addon) =>
  ({ service: { id: provider }, link, type, ...(addon ? { addon } : {}) });
const show = (id, offers) => ({ tmdbId: id, streamingOptions: { br: offers } });
const response = (object, status = 200) =>
  new Response(JSON.stringify(object), { status, headers: { "content-type": "application/json" } });

test("identifies authentic Disney, Netflix, Prime, Apple and HBO links", () => {
  for (const provider of Object.keys(FIX)) {
    assert.ok(validateLink(provider, FIX[provider]), provider);
  }
  assert.equal(validateLink("hbo", FIX.hbo),
    "https://play.hbomax.com/show/a0a085b8-6657-46ca-9476-6af0070b9cce");
});

test("normalizes verified Netflix slash and Prime Video catalog links", () => {
  assert.equal(validateLink("netflix", FIX.netflix + "/"), FIX.netflix);
  const primeWeb = "https://www.primevideo.com/detail/0HM3DRN79M0E7QOBLMYTS41IG0";
  assert.equal(validateLink("prime", primeWeb), primeWeb);
  assert.equal(validateLink("prime", primeWeb + "?redirect=https://evil.example"), null);
  assert.equal(validateLink("prime", "https://www.primevideo.com/detail/hi"), null);
  assert.equal(validateLink("prime", "https://primevideo.com/detail/0HM3DRN79M0E7QOBLMYTS41IG0"), null);
});

test("prefers a verified direct video link over the provider offer page", () => {
  const video = "https://www.netflix.com/title/81309354";
  const item = show("movie/329", [
    { ...mockOffer("netflix", FIX.netflix), videoLink: video }
  ]);
  assert.deepEqual(inspectOffers(item, "movie/329", "netflix"),
    { status: "link_candidate", url: video });
  const unsafe = show("movie/329", [
    { ...mockOffer("netflix", FIX.netflix), videoLink: "https://evil.example/watch/329" }
  ]);
  assert.deepEqual(inspectOffers(unsafe, "movie/329", "netflix"),
    { status: "link_candidate", url: FIX.netflix });
});

test("signed device status cannot bypass activation", async () => {
  const env = makeEnv();
  const noMode = await handleRequest(request("/v1/device/status"), env);
  assert.equal(noMode.status, 503);
  env.ANYTV_RESOLVER_AUTH_MODE = "device";
  const denied = await handleRequest(request("/v1/device/status"), env);
  assert.equal(denied.status, 401);
  assert.deepEqual(await denied.json(), { status: "unauthorized" });
});

test("preserves only validated Apple TV+ playableId and drops affiliate params", () => {
  const url = "https://tv.apple.com/br/movie/the-gorge/umc.cmc.26o403koqo2klixc0jtqy6tmc" +
    "?at=1000l3V2&playableId=tvs.sbd.9001%3A271469518";
  assert.equal(validateLink("apple", url),
    "https://tv.apple.com/br/movie/the-gorge/umc.cmc.26o403koqo2klixc0jtqy6tmc" +
    "?playableId=tvs.sbd.9001%3A271469518");
  assert.equal(validateLink("apple", url + "&playableId=another"), null);
  assert.equal(validateLink("apple", url + "&redirect=https://evil.example"), null);
});

test("rejects unsafe hosts, credentials, schemes, parameters and mismatched providers", () => {
  assert.equal(validateLink("netflix", "https://evil.com/title/80175798"), null);
  assert.equal(validateLink("netflix", "https://www.netflix.com@evil.com/title/80175798"), null);
  assert.equal(validateLink("disney", FIX.disney + "?redirect=https://evil.com"), null);
  assert.equal(validateLink("hbo", FIX.hbo + "&evil=1"), null);
  assert.equal(validateLink("netflix", FIX.disney), null);
  assert.equal(validateLink("prime", FIX.prime + "&gti=evil"), null);
  assert.equal(validateLink("apple", FIX.apple + "&redirect=https://evil.com"), null);
  assert.equal(validateLink("prime", "http://app.primevideo.com/detail?gti=anything"), null);
});

test("filters provider-specific basic subscription; rejects addons and rentals", () => {
  const item = show("movie/329", [
    mockOffer("prime", FIX.prime, "rent"),
    mockOffer("prime", FIX.prime, "subscription", { id: "hbomax" }),
  ]);
  assert.deepEqual(inspectOffers(item, "movie/329", "prime"),
    { status: "not_available" });
});

test("does not confuse titles with the same name but different TMDB IDs", () => {
  assert.throws(
    () => inspectOffers(show("movie/331984", [mockOffer("prime", FIX.prime)]),
      "movie/329", "prime"),
    /invalid_upstream_title/
  );
});

test("rejects several distinct destination links for one service", () => {
  const result = inspectOffers(show("movie/329", [
    mockOffer("netflix", FIX.netflix),
    mockOffer("netflix", "https://www.netflix.com/title/81309354"),
  ]), "movie/329", "netflix");
  assert.equal(result.status, "ambiguous");
});

test("HTTP 404/409 retain distinct semantic statuses for Android fallback", async () => {
  const resolve = async (provider, offers) => {
    const env = makeEnv();
    const url = "/v1/resolve/movie/329/" + provider;
    const deps = { fetch: async () => response(show("movie/329", offers)) };
    const answer = await handleRequest(request(url), env, deps);
    return { http: answer.status, json: await answer.json() };
  };
  const absent = await resolve("prime", []);
  assert.equal(absent.http, 404);
  assert.equal(absent.json.status, "not_available");

  const unsupported = await resolve("prime", [
    mockOffer("prime", "https://unknown.example/detail/wrong")
  ]);
  assert.equal(unsupported.http, 404);
  assert.equal(unsupported.json.status, "no_supported_link");

  const ambiguous = await resolve("netflix", [
    mockOffer("netflix", FIX.netflix),
    mockOffer("netflix", "https://www.netflix.com/title/81309354")
  ]);
  assert.equal(ambiguous.http, 409);
  assert.equal(ambiguous.json.status, "ambiguous");
});

test("never fetches provider data before authentication", async () => {
  const env = makeEnv();
  let calls = 0;
  const deps = { fetch: async () => { calls++; throw Error("should not fetch"); } };
  const denied = await handleRequest(request("/v1/resolve/movie/329/prime", null), env, deps);
  assert.equal(denied.status, 401);
  assert.equal(calls, 0);
});

test("configuration must fail closed, without a provider key or DB", async () => {
  const env = makeEnv();
  delete env.ANYTV_MOVIE_API_KEY;
  const denied = await handleRequest(request("/v1/resolve/movie/329/prime"), env);
  assert.equal(denied.status, 503);
});

test("fetches a Brazilian subscription, sends key as header and caches it", async () => {
  const env = makeEnv();
  let fetches = 0;
  const deps = {
    now: () => Date.parse("2026-10-15T12:00:00Z"),
    fetch: async (url, options) => {
      fetches++;
      assert.match(url, /\/shows\/movie%2F329\?country=br/);
      assert.equal(options.headers["X-API-Key"], env.ANYTV_MOVIE_API_KEY);
      assert.equal(options.redirect, "manual");
      return response(show("movie/329", [mockOffer("prime", FIX.prime)]));
    }
  };
  const a = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(a.status, 200);
  let body = await a.json();
  assert.equal(body.url, FIX.prime);
  assert.equal(body.verified_on_tv, false);
  assert.equal(body.cached, false);
  assert.equal(JSON.stringify(body).includes(env.ANYTV_MOVIE_API_KEY), false);
  const b = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(b.status, 200);
  body = await b.json();
  assert.equal(body.cached, true);
  assert.equal(fetches, 1);
  assert.equal(env._cache.size, 1);
});

test("link resolver has no monthly or per-minute usage caps across 225 distinct titles", async () => {
  const env = makeEnv();
  let fetches = 0;
  const deps = { now: () => Date.parse("2026-10-15T12:00:00Z"),
    fetch: async url => {
      fetches++;
      const id = decodeURIComponent(new URL(url).pathname.split("/").pop()).split("/")[1];
      return response(show("movie/" + id, [mockOffer("prime", FIX.prime)]));
    } };
  for (let id = 1; id <= 225; id++) {
    const result = await handleRequest(request("/v1/resolve/movie/" + id + "/prime"), env, deps);
    assert.equal(result.status, 200, "title " + id + " should not be blocked");
  }
  assert.equal(fetches, 225);
  assert.equal(env._cache.size, 225);
});

test("validates query path and blocks arbitrary upstream URLs", async () => {
  const env = makeEnv();
  const testPaths = [
    "/v1/resolve/movie/329/prime?url=https://evil.com",
    "/v1/resolve/movie/329/random",
    "/v1/resolve/movie/../prime/329",
    "/v1/resolve/movie/0/prime",
    "/v1/resolve/movie/329/prime?country=US",
  ];
  for (const path of testPaths) {
    const r = await handleRequest(request(path), env);
    assert.equal(r.status, 400, path);
  }
});

test("no rate-limit binding is required for authenticated link lookups", async () => {
  const env = makeEnv();
  const deps = { fetch: async () =>
    response(show("movie/329", [mockOffer("prime", FIX.prime)])) };
  const result = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(result.status, 200);
});

test("remote errors return generic status without consuming any local usage budget", async () => {
  const env = makeEnv();
  const r = await handleRequest(request("/v1/resolve/movie/329/prime"), env,
    { fetch: async () => response({ error: "provider contains secret" }, 500) });
  const body = await r.json();
  assert.equal(r.status, 503);
  assert.equal(JSON.stringify(body).includes("provider contains secret"), false);
  assert.equal(env._cache.size, 0);
});

test("non-subscription title is a safe, cacheable non-availability response", async () => {
  const env = makeEnv();
  const result = await handleRequest(request("/v1/resolve/movie/329/prime"), env,
    { fetch: async () => response(show("movie/329", [
      mockOffer("prime", FIX.prime, "rent")]))});
  assert.equal(result.status, 404);
  assert.equal((await result.json()).status, "not_available");
});

test("missing BR country means unavailable, not an API transport failure", () => {
  const noBrazil = { tmdbId: "movie/329", streamingOptions: { us: [
    mockOffer("prime", FIX.prime)] } };
  assert.deepEqual(inspectOffers(noBrazil, "movie/329", "prime"),
    { status: "not_available" });
});

test("link cache expiry permits unrestricted refreshed and new queries", async () => {
  const env = makeEnv();
  let calls = 0;
  let timestamp = Date.parse("2026-10-15T12:00:00Z");
  const deps = {
    now: () => timestamp,
    fetch: async () => {
      calls++;
      return response(show("movie/329", [mockOffer("prime", FIX.prime)]));
    }
  };
  const a = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(a.status, 200);
  timestamp += (6 * 3600 + 1) * 1000;
  const b = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(b.status, 200);
  assert.equal(calls, 2);
  const c = await handleRequest(request("/v1/resolve/movie/398978/netflix"), env,
    { ...deps, fetch: async () => {
      calls++;
      return response(show("movie/398978", [mockOffer("netflix", FIX.netflix)]));
    } });
  assert.equal(c.status, 200);
  assert.equal(calls, 3);
});

test("health probe does not reveal private configuration", async () => {
  const r = await handleRequest(request("/health", null), {});
  assert.equal(r.status, 200);
  const text = await r.text();
  assert.equal(text.includes("private-pilot-access"), false);
});

test("catalog proxy refuses to run without Cloudflare secret", async () => {
  const response = await catalogRequest(
    new Request("https://anytv.invalid/v1/catalog/genre/movie/list"),
    { DB: {} }
  );
  assert.equal(response.status, 503);
  assert.equal((await response.json()).status, "catalog_not_configured");
});

test("catalog proxy rejects arbitrary paths and regions without contacting TMDB", async () => {
  const env = { TMDB_READ_TOKEN: "server-only", DB: {} };
  for (const url of [
    "/v1/catalog/evil/host",
    "/v1/catalog/search/movie?language=en-US&query=x",
    "/v1/catalog/discover/movie?watch_region=US",
    "/v1/catalog/movie/0/external_ids",
    "/v1/catalog/search/movie?query=" + "x".repeat(91),
  ]) {
    const response = await catalogRequest(new Request("https://anytv.invalid" + url), env);
    assert.equal(response.status, 400, url);
  }
});

test("catalog proxy keeps TMDB secret on server and caches read-only responses", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "secret-server-only";
  let calls = 0;
  const request = new Request("https://anytv.invalid/v1/catalog/genre/movie/list?language=pt-BR");
  const dependencies = { now: () => Date.parse("2026-10-15T12:00:00Z"), fetch: async (url, init) => {
    calls++;
    assert.equal(new URL(url).hostname, "api.themoviedb.org");
    assert.equal(init.headers.Authorization, "Bearer secret-server-only");
    return new Response(JSON.stringify({ genres: [{ id: 28, name: "Ação" }] }), { status: 200 });
  }};
  for (let i = 0; i < 2; i++) {
    const response = await catalogRequest(request, env, dependencies);
    assert.equal(response.status, 200);
    const body = await response.text();
    assert.equal(body.includes("secret-server-only"), false);
    assert.match(body, /genres/);
  }
  assert.equal(calls, 1);
});

test("catalog requests are not throttled after 225 distinct uncached searches", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "server-only";
  let count = 0;
  const deps = { now: () => Date.parse("2026-10-23T00:00:00Z"),
    fetch: async () => {
      count++;
      return response({ results: [{ id: count, title: "OK" }] });
    } };
  for (let i = 1; i <= 225; i++) {
    const u = new Request("https://anytv.invalid/v1/catalog/search/movie?query=film" + i);
    assert.equal((await catalogRequest(u, env, deps)).status, 200, "search "+i);
  }
  assert.equal(count, 225);
  assert.equal(env._cache.size, 225);
});

test("catalog search remains cached then refreshes without usage cap", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "server-only";
  let calls = 0;
  let now = Date.parse("2026-10-15T12:00:00Z");
  const deps = { now: () => now, fetch: async () => {
    calls++;
    return new Response(JSON.stringify({ results: [{ id: 1, title: "Titanic" }] }),
      { status: 200 });
  }};
  const req = new Request("https://anytv.invalid/v1/catalog/search/movie?query=Titanic");
  assert.equal((await catalogRequest(req, env, deps)).status, 200);
  now += 2 * 3600 * 1000;
  assert.equal((await catalogRequest(req, env, deps)).status, 200);
  assert.equal(calls, 1, "2-hour cache hit must not fetch upstream");
  now += 23 * 3600 * 1000;
  const expired = await catalogRequest(req, env, deps);
  assert.equal(expired.status, 200);
  assert.equal(calls, 2);
  assert.equal(env._cache.size, 1);
});

test("catalog TMDB discovery has no monthly usage cap", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "server-only";
  let calls = 0;
  const dependencies = { now: () => Date.parse("2026-10-15T12:00:00Z"), fetch: async () => {
    calls++;
    return new Response(JSON.stringify({ results: [] }), { status: 200 });
  }};
  const first = await catalogRequest(
    new Request("https://anytv.invalid/v1/catalog/discover/movie?page=1"), env, dependencies);
  const next = await catalogRequest(
    new Request("https://anytv.invalid/v1/catalog/discover/movie?page=2"), env, dependencies);
  assert.equal(first.status, 200);
  assert.equal(next.status, 200);
  assert.equal(calls, 2);
});

test("TMDB v3 key stays server-only as upstream api_key", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "0123456789abcdef0123456789abcdef";
  let seen = false;
  const deps = { now: () => Date.parse("2026-10-15T12:00:00Z"), fetch: async (url, init) => {
    const upstream = new URL(url);
    assert.equal(upstream.searchParams.get("api_key"), env.TMDB_READ_TOKEN);
    assert.equal(init.headers.Authorization, undefined);
    seen = true;
    return new Response(JSON.stringify({ genres: [{id:28,name:"Ação"}] }), {status:200});
  }};
  const response = await catalogRequest(new Request("https://anytv.invalid/v1/catalog/genre/movie/list"),env,deps);
  assert.equal(response.status,200);
  assert.equal((await response.text()).includes(env.TMDB_READ_TOKEN),false);
  assert.equal(seen,true);
});

test("TMDB upstream error reports status without exposing credentials",async()=>{
  const env=makeEnv();
  env.TMDB_READ_TOKEN="server-only";
  const response=await catalogRequest(new Request("https://anytv.invalid/v1/catalog/genre/movie/list"),env,{
    now:()=>Date.parse("2026-10-15T12:00:00Z"),
    fetch:async()=>new Response('unauthorized',{status:401})
  });
  assert.equal(response.status,503);
  assert.equal(response.headers.get("x-anytv-upstream-status"),"401");
  assert.equal((await response.text()).includes(env.TMDB_READ_TOKEN),false);
});


test("cinema BR now-playing uses TMDB region and preserves cache semantics", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "server-only-test";
  const seen = [];
  const deps = { fetch: async url => {
    const parsed = new URL(url);
    seen.push({ route: parsed.pathname, region: parsed.searchParams.get("region"),
      page: parsed.searchParams.get("page") });
    return new Response(JSON.stringify({
      results: [{ id: 456, title: "Filme no cinema" }], total_pages: 3
    }), { status: 200 });
  }};
  for (const page of [1, 2]) {
    const req = new Request("https://anytv.invalid/v1/catalog/movie/now_playing"
      + "?language=pt-BR&region=BR&page=" + page);
    assert.equal((await catalogRequest(req, env, deps)).status, 200);
    assert.equal((await catalogRequest(req, env, deps)).status, 200);
  }
  assert.deepEqual(seen, [
    { route: "/3/movie/now_playing", region: "BR", page: "1" },
    { route: "/3/movie/now_playing", region: "BR", page: "2" }
  ]);
  for (const suffix of [
    "movie/now_playing?region=US", "movie/now_playing?region=BR&region=US",
    "movie/upcoming?region=BR", "discover/movie?region=BR"
  ]) {
    assert.equal((await catalogRequest(
      new Request("https://anytv.invalid/v1/catalog/" + suffix), env, deps)).status,
      400, suffix);
  }
});

test("discovery endpoints are allowlisted and cached without exposing TMDB token", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "secret-server-only";
  let calls = 0;
  const deps = { now: () => Date.parse("2026-10-21T12:00:00Z"),
    fetch: async (url, init) => {
      calls++;
      assert.equal(new URL(url).hostname, "api.themoviedb.org");
      assert.equal(init.headers.Authorization, "Bearer secret-server-only");
      return new Response(JSON.stringify({ results: [{ id: 1, title: "Example" }] }), { status: 200 });
    }
  };
  for (const route of [
    "trending/movie/week", "trending/tv/day", "movie/top_rated",
    "movie/now_playing", "tv/on_the_air", "movie/1/videos",
    "tv/2/similar", "movie/3/recommendations"
  ]) {
    const req = new Request("https://anytv.invalid/v1/catalog/" + route + "?language=pt-BR");
    assert.equal((await catalogRequest(req, env, deps)).status, 200, route);
    assert.equal((await catalogRequest(req, env, deps)).status, 200, route);
  }
  assert.equal(calls, 8);
  for (const route of ["movie/0/videos", "movie/1/credits/all", "trending/person/day"]) {
    assert.equal((await catalogRequest(new Request("https://anytv.invalid/v1/catalog/" + route), env)).status, 400);
  }
});

test("genre sorting is server-side, validated by media and cached per sort key", async () => {
  const env = makeEnv();
  env.TMDB_READ_TOKEN = "secret-server-only";
  const calls = [];
  const deps = { now: () => Date.parse("2026-10-23T15:00:00Z"),
    fetch: async url => {
      calls.push(new URL(url));
      return new Response(JSON.stringify({ results: [{ id: 123, title: "Teste" }],
        total_pages: 9 }), {status: 200});
    }
  };
  for (const [media, sorts] of [
    ["movie", ["popularity.desc", "vote_average.desc", "primary_release_date.desc", "title.asc"]],
    ["tv", ["popularity.desc", "vote_average.desc", "first_air_date.desc", "name.asc"]]
  ]) {
    for (const sort of sorts) {
      const url = "https://anytv.invalid/v1/catalog/discover/" + media
        + "?language=pt-BR&with_genres=28&page=2&sort_by=" + sort;
      assert.equal((await catalogRequest(new Request(url), env, deps)).status, 200, url);
      assert.equal((await catalogRequest(new Request(url), env, deps)).status, 200, url);
    }
  }
  assert.equal(calls.length, 8, "each of eight legal sort keys fetched exactly once");
  assert(calls.every(url => url.searchParams.get("with_genres") === "28"));
  assert(calls.every(url => url.searchParams.get("page") === "2"));
  for (const url of [
    "/v1/catalog/discover/movie?sort_by=first_air_date.desc",
    "/v1/catalog/discover/tv?sort_by=title.asc",
    "/v1/catalog/discover/movie?sort_by=popularity.asc",
    "/v1/catalog/search/movie?sort_by=vote_average.desc&query=test",
    "/v1/catalog/discover/movie?sort_by=popularity.desc&sort_by=title.asc",
  ]) {
    assert.equal((await catalogRequest(new Request("https://anytv.invalid" + url), env)).status,
      400, url);
  }
});

test("English metadata is permitted only for trailer videos", async () => {
  const env = { TMDB_READ_TOKEN: "server-only", DB: {} };
  for (const url of [
    "/v1/catalog/discover/movie?language=en-US",
    "/v1/catalog/trending/movie/week?language=en-US",
    "/v1/catalog/movie/1/recommendations?language=en-US"
  ]) {
    assert.equal((await catalogRequest(new Request("https://anytv.invalid" + url), env)).status, 400);
  }
  const operational = makeEnv();
  operational.TMDB_READ_TOKEN = "secret-server-only";
  let upstream = "";
  const response = await catalogRequest(
    new Request("https://anytv.invalid/v1/catalog/movie/42/videos?language=en-US"),
    operational, { fetch: async url => {
      upstream = String(url);
      return new Response(JSON.stringify({results: []}), {status: 200});
    }});
  assert.equal(response.status, 200);
  assert.equal(new URL(upstream).searchParams.get("language"), "en-US");
});

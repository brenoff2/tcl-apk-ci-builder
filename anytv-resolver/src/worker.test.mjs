import test from "node:test";
import assert from "node:assert/strict";
import { handleRequest, inspectOffers, validateLink } from "./worker.mjs";

const FIX = Object.freeze({
  disney: "https://www.disneyplus.com/browse/entity-8f08a8df-8b2f-49e1-9130-1e842b90f185",
  netflix: "https://www.netflix.com/title/80175798",
  prime: "https://app.primevideo.com/detail?gti=amzn1.dv.gti.7ea9f6d9-bdc8-9b2e-97a9-c341306e36ef",
  apple: "https://tv.apple.com/br/movie/o-parque-dos-dinossauros/umc.cmc.3bp8rj8ab9otj4ug279xhib2f?at=1000l3V2&ct=app_tvplus",
  hbo: "https://play.hbomax.com/show/a0a085b8-6657-46ca-9476-6af0070b9cce?utm_source=universal_search",
});

function makeEnv(cap = 2) {
  const cache = new Map();
  const months = new Map();
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
              if (sql.startsWith("UPDATE monthly_quota")) {
                const old = months.get(params[0]) || 0;
                if (old >= params[1]) return null;
                months.set(params[0], old + 1);
                return { used: old + 1 };
              }
              throw Error("Unexpected first query: " + sql);
            },
            async run() {
              if (sql.startsWith("INSERT OR IGNORE INTO monthly_quota")) {
                if (!months.has(params[0])) months.set(params[0], 0);
                return { success: true };
              }
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
    MAX_UPSTREAM_CALLS_MONTH: String(cap),
    RATE_LIMIT: { limit: async () => ({ success: true }) },
    DB: db,
    _cache: cache,
    _months: months,
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
      assert.equal(options.redirect, "error");
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
  assert.equal(env._months.get("2026-10"), 1);
});

test("respects global monthly upstream cap across different titles", async () => {
  const env = makeEnv(1);
  const deps = { now: () => Date.parse("2026-10-15T12:00:00Z"),
    fetch: async () => response(show("movie/329", [mockOffer("prime", FIX.prime)])) };
  const first = await handleRequest(request("/v1/resolve/movie/329/prime"), env, deps);
  assert.equal(first.status, 200);
  const second = await handleRequest(request("/v1/resolve/movie/398978/netflix"), env, deps);
  assert.equal(second.status, 429);
  assert.equal((await second.json()).status, "monthly_quota_exhausted");
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

test("rate limit fails without spending external API credits", async () => {
  const env = makeEnv();
  env.RATE_LIMIT = { limit: async () => ({ success: false }) };
  const r = await handleRequest(request("/v1/resolve/movie/329/prime"), env);
  assert.equal(r.status, 429);
  assert.equal(env._months.size, 0);
});

test("remote errors return generic status and consume at most reserved credits", async () => {
  const env = makeEnv();
  const r = await handleRequest(request("/v1/resolve/movie/329/prime"), env,
    { fetch: async () => response({ error: "provider contains secret" }, 500) });
  const body = await r.json();
  assert.equal(r.status, 503);
  assert.equal(JSON.stringify(body).includes("provider contains secret"), false);
  assert.equal(env._months.get(new Date().toISOString().slice(0, 7)), 1);
});

test("non-subscription title is a safe, cacheable non-availability response", async () => {
  const env = makeEnv();
  const result = await handleRequest(request("/v1/resolve/movie/329/prime"), env,
    { fetch: async () => response(show("movie/329", [
      mockOffer("prime", FIX.prime, "rent")]))});
  assert.equal(result.status, 404);
  assert.equal((await result.json()).status, "not_available");
});

test("health probe does not reveal private configuration", async () => {
  const r = await handleRequest(request("/health", null), {});
  assert.equal(r.status, 200);
  const text = await r.text();
  assert.equal(text.includes("private-pilot-access"), false);
});

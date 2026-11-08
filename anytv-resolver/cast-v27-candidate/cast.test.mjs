import test from "node:test";
import assert from "node:assert/strict";
import { catalogRequest } from "./worker.mjs";

function envMock() {
  const cached = new Map();
  return {
    TMDB_READ_TOKEN: "fake-test-only-token",
    DB: {
      prepare(sql) {
        return {
          bind(...args) {
            return {
              async first() {
                assert.match(sql, /SELECT payload,expires_at/);
                return cached.get(args[0]) || null;
              },
              async run() {
                assert.match(sql, /INSERT INTO link_cache/);
                cached.set(args[0], { payload: args[1], expires_at: args[2] });
                return { success: true };
              }
            };
          }
        };
      }
    },
    cached
  };
}

test("v27 cast-only enables movie and tv credits while retaining safe catalog contract", async () => {
  const env = envMock();
  let requests = 0;
  const deps = { fetch: async (url, options) => {
    requests++;
    const upstream = new URL(url);
    assert.equal(upstream.hostname, "api.themoviedb.org");
    assert.match(upstream.pathname, /^\/3\/(movie\/550|tv\/1399)\/credits$/);
    assert.equal(upstream.searchParams.get("language"), "pt-BR");
    assert.equal(options.headers.Authorization, "Bearer fake-test-only-token");
    return new Response(JSON.stringify({
      cast: [{ id: 41, name: "Pessoa de Teste", character: "Papel de Teste",
        profile_path: "/poster-teste.jpg" }]
    }), { status: 200 });
  }};
  for (const kind of ["movie/550", "tv/1399"]) {
    const url = new Request("https://anytv.invalid/v1/catalog/" + kind + "/credits");
    for (let attempt = 0; attempt < 2; attempt++) {
      const reply = await catalogRequest(url, env, deps);
      assert.equal(reply.status, 200);
      const body = await reply.json();
      assert.equal(body.cast[0].name, "Pessoa de Teste");
      assert.equal(body.cast[0].character, "Papel de Teste");
      assert.equal(body.cast[0].profile_path, "/poster-teste.jpg");
    }
  }
  assert.equal(requests, 2, "one upstream request per title: cache hit on repeat");
  for (const invalid of ["movie/0/credits", "tv/01399/credits",
    "movie/550/credits?language=en-US", "person/41/credits"]) {
    const reply = await catalogRequest(
      new Request("https://anytv.invalid/v1/catalog/" + invalid), env, deps);
    assert.equal(reply.status, 400, invalid);
  }
  assert.equal(requests, 2, "invalid requests never contact TMDB");
});

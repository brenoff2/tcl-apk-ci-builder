import test from "node:test";
import assert from "node:assert/strict";
import { watchmodeSourcesRequest } from "./watchmode.mjs";
const request = new Request("https://example.test/v1/catalog/watchmode/movie/278/sources");
const base = { ANYTV_WATCHMODE_ENABLED: "true", WATCHMODE_API_KEY: "unit-test-placeholder", ANYTV_RESOLVER_AUTH_MODE: "device" };
test("disabled without credentials and explicit switch", async () => {
  const res = await watchmodeSourcesRequest(request, {}, {});
  assert.equal(res.status, 503);
});
test("requires device authentication before network", async () => {
  const res = await watchmodeSourcesRequest(request, base, { verifyDeviceProof: async () => false,
    fetch: () => { throw Error("must not reach upstream"); } });
  assert.equal(res.status, 401);
});
test("subscription in Brazil only, no provider links or secrets", async () => {
  let called = false;
  const response = await watchmodeSourcesRequest(request, base, {
    verifyDeviceProof: async () => true,
    fetch: async (url, opts) => {
      called = true;
      assert.equal(url, "https://api.watchmode.com/v1/title/movie-278/sources/?regions=BR");
      assert.equal(opts.headers["X-API-Key"], base.WATCHMODE_API_KEY);
      return new Response(JSON.stringify([
        { region: "BR", type: "sub", source_id: 203, name: "Netflix" },
        { region: "US", type: "sub", source_id: 204, name: "Other" },
        { region: "BR", type: "rent", source_id: 205, name: "Rental" },
      ]), { status: 200 });
    },
  });
  assert.ok(called);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.sources.length, 1);
  assert.deepEqual(body.sources[0], { providerId: 203, provider: "Netflix", offer: "subscription" });
  assert.equal(JSON.stringify(body).includes(base.WATCHMODE_API_KEY), false);
});
test("upstream unavailable never pretends subscription", async () => {
  const result = await watchmodeSourcesRequest(request, base, {
    verifyDeviceProof: async () => true, fetch: async () => new Response("error", { status: 429 })
  });
  assert.equal(result.status, 503);
});

test("does not use shared cache for device-protected replies", async () => {
  const cache = { match: () => { throw Error("shared cache read"); },
    put: () => { throw Error("shared cache write"); } };
  const result = await watchmodeSourcesRequest(request, base, {
    cache, verifyDeviceProof: async () => true,
    fetch: async () => new Response(JSON.stringify([]), { status: 200 }),
  });
  assert.equal(result.status, 200);
});

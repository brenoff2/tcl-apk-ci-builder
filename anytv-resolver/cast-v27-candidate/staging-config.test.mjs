import test from "node:test";
import assert from "node:assert/strict";
import { validateStagingConfig, PROD_D1_ID } from "./check-staging-config.mjs";

const STAGING_ID = "11111111-2222-4333-8444-555555555555";
function good() {
  return {
    name: "anytv-cast-staging",
    main: "worker.mjs",
    compatibility_date: "2026-10-15",
    workers_dev: false,
    preview_urls: false,
    observability: { enabled: false },
    d1_databases: [{ binding: "DB", database_name: "anytv-cast-staging",
      database_id: STAGING_ID }],
    vars: { ANYTV_DEVICE_ACTIVATION_ENABLED: "false",
      ANYTV_RESOLVER_AUTH_MODE: "device" }
  };
}
test("separate staging identity passes static contract only", () => {
  assert.deepEqual(validateStagingConfig(good()), []);
});
test("fail-closed on production D1, hidden production references and missing ID", () => {
  for (const mutate of [
    c => { c.d1_databases[0].database_id = PROD_D1_ID; },
    c => { delete c.d1_databases[0].database_id; },
    c => { c.d1_databases[0].database_id = "NOT-YET-PROVISIONED"; },
    c => { c.d1_databases[0].database_name = "anytv-link-resolver"; },
    c => { c.d1_databases[0].binding = "PRODUCTION_DB"; },
    c => { c.d1_databases.push({ binding:"DB2",database_name:"prod",database_id:PROD_D1_ID }); },
    c => { c.hidden_reference = PROD_D1_ID; },
  ]) {
    const c = good(); mutate(c);
    assert.ok(validateStagingConfig(c).length > 0, JSON.stringify(c));
  }
});
test("fail-closed on production Worker, preview exposure or device activation", () => {
  for (const mutate of [
    c => { c.name = "anytv-link-resolver-pilot"; },
    c => { c.main = "../src/worker.mjs"; },
    c => { c.compatibility_date = "2026-10-01"; },
    c => { c.workers_dev = true; },
    c => { c.preview_urls = true; },
    c => { delete c.preview_urls; },
    c => { c.vars.ANYTV_DEVICE_ACTIVATION_ENABLED = "true"; },
    c => { c.vars.ANYTV_RESOLVER_AUTH_MODE = "pilot"; },
    c => { c.vars.TMDB_READ_TOKEN = "placeholder"; },
    c => { c.routes = [{pattern:"production.example"}]; },
  ]) {
    const c = good(); mutate(c);
    assert.ok(validateStagingConfig(c).length > 0, JSON.stringify(c));
  }
});
test("missing / malformed configuration cannot pass", () => {
  for (const c of [null, [], {}, {d1_databases:[]}, "x"]) {
    assert.ok(validateStagingConfig(c).length > 0);
  }
});

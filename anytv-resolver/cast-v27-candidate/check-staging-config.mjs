import { readFileSync } from "node:fs";
import { pathToFileURL } from "node:url";

export const PROD_D1_ID = "9bab6598-d019-4e88-8b29-a327ff0cfd00";
export const STAGING_WORKER = "anytv-cast-staging";
export const STAGING_D1 = "anytv-cast-staging";
const UUID = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i;

export function validateStagingConfig(config) {
  const errors = [];
  const reject = (condition, message) => { if (condition) errors.push(message); };
  reject(!config || typeof config !== "object" || Array.isArray(config),
    "configuration must be an object");
  if (errors.length) return errors;

  const allowedTop = new Set(["name", "main", "compatibility_date",
    "workers_dev", "preview_urls", "d1_databases", "vars", "observability"]);
  reject(Object.keys(config).some(key => !allowedTop.has(key)),
    "unexpected top-level field");
  reject(config.name !== STAGING_WORKER, "worker identity is not staging");
  reject(config.main !== "worker.mjs", "main must use audited staging module");
  reject(config.compatibility_date !== "2026-10-15",
    "compatibility date must match active v27");
  reject(config.workers_dev !== false || config.preview_urls !== false,
    "public routes and previews must remain disabled until separately authorized");
  reject(config.observability !== undefined &&
    (config.observability === null || config.observability.enabled !== false),
    "observability must be explicitly disabled if present");

  const databases = config.d1_databases;
  reject(!Array.isArray(databases) || databases.length !== 1,
    "exactly one dedicated D1 binding required");
  if (Array.isArray(databases) && databases.length === 1) {
    const db = databases[0];
    reject(!db || typeof db !== "object" ||
      Object.keys(db).some(key => !["binding", "database_name", "database_id"].includes(key)),
      "invalid D1 binding structure");
    if (db && typeof db === "object") {
      reject(db.binding !== "DB", "D1 binding name must be DB");
      reject(db.database_name !== STAGING_D1, "D1 database name is not staging");
      reject(typeof db.database_id !== "string" || !UUID.test(db.database_id) ||
        db.database_id.toLowerCase() === PROD_D1_ID,
        "staging D1 UUID missing, invalid, or production");
    }
  }
  const vars = config.vars;
  reject(!vars || typeof vars !== "object" || Array.isArray(vars) ||
    Object.keys(vars).sort().join(",") !==
      "ANYTV_DEVICE_ACTIVATION_ENABLED,ANYTV_RESOLVER_AUTH_MODE" ||
    vars.ANYTV_DEVICE_ACTIVATION_ENABLED !== "false" ||
    vars.ANYTV_RESOLVER_AUTH_MODE !== "device",
    "staging variables must disable activation and fail closed");
  // Inspect all string values as well as typed bindings: forbid hidden production references.
  function inspect(value) {
    if (typeof value === "string") {
      reject(value.toLowerCase().includes(PROD_D1_ID), "production D1 reference detected");
      reject(value.includes("anytv-link-resolver-pilot"),
        "production Worker reference detected");
    } else if (Array.isArray(value)) {
      value.forEach(inspect);
    } else if (value && typeof value === "object") {
      Object.values(value).forEach(inspect);
    }
  }
  inspect(config);
  return [...new Set(errors)];
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  if (process.argv.length !== 3) {
    console.error("Usage: node check-staging-config.mjs <staging-config.json>");
    process.exit(2);
  }
  let config;
  try {
    config = JSON.parse(readFileSync(process.argv[2], "utf8"));
  } catch {
    console.error("STAGING_CONFIG_REJECTED unreadable or invalid JSON");
    process.exit(1);
  }
  const errors = validateStagingConfig(config);
  if (errors.length) {
    errors.forEach(error => console.error("STAGING_CONFIG_REJECTED " + error));
    process.exit(1);
  }
  console.log("STAGING_CONFIG_STATIC_PASS; requires remote binding readback before any deployment");
}

/**
 * Authorized one-time GitHub Actions contract test against the real commercial
 * provider. Deliberately prints only provider, status, counts: never credentials
 * or remote URL payloads. Budget: maximum 5 HTTP calls.
 */
import { inspectOffers } from "../src/worker.mjs";
const secret = process.env.ANYTV_MOVIE_API_KEY;
if (typeof secret !== "string" || secret.length < 8) {
  throw new Error("PROVIDER_SECRET_NOT_CONFIGURED");
}
const checks = [
  ["Zootopia 2", "movie/1084242", "disney"],
  ["Birdman", "movie/194662", "hbo"],
  ["Jurassic Park", "movie/329", "prime"],
  ["O Irlandês", "movie/398978", "netflix"],
  ["The Gorge", "movie/950396", "apple"],
];
let success = 0;
for (const [label, id, provider] of checks) {
  const url = new URL("https://api.movieofthenight.com/v4/shows/" +
    id.replace("/", "%2F"));
  url.searchParams.set("country", "br");
  url.searchParams.set("series_granularity", "show");
  const response = await fetch(url, {
    headers: { "X-API-Key": secret, Accept: "application/json" },
    redirect: "error",
    signal: AbortSignal.timeout(12000),
  });
  if (!response.ok) {
    throw new Error("PROVIDER_HTTP_STATUS_" + response.status + "_FOR_" + provider);
  }
  const result = inspectOffers(await response.json(), id, provider);
  console.log("PROVIDER_CONTRACT", JSON.stringify({
    title: label, provider, status: result.status,
    link_type: result.url ? "validated_https_title_link" : "unavailable",
    physical_tv_test: false,
  }));
  if (result.status !== "link_candidate") {
    throw new Error("PROVIDER_NOT_RESOLVED_" + provider);
  }
  success++;
}
console.log("REAL_SOURCE_CONTRACTS_OK", success, "of", checks.length, "requests", checks.length);

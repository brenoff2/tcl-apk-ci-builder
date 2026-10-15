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
  const document = await response.json();
  const result = inspectOffers(document, id, provider);
  if (provider === "apple" && result.status !== "link_candidate") {
    const options = document?.streamingOptions?.br || [];
    for (const opt of options.filter(x => x?.service?.id === "apple")) {
      let metadata = { type: opt?.type, has_addon: !!opt?.addon };
      try {
        const u = new URL(opt.link);
        metadata.host = u.host;
        metadata.protocol = u.protocol;
        metadata.segments = u.pathname.split("/").filter(Boolean).map((part, n) =>
          n > 2 && part.startsWith("umc.cmc.") ? "{apple-id}" : part.replace(
            /[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}/g, "{uuid}"));
        metadata.queryKeys = [...new Set([...u.searchParams.keys()])];
      } catch {
        metadata.link_type = "not_a_url";
      }
      console.log("APPLE_LINK_DIAGNOSTIC", JSON.stringify(metadata));
    }
  }
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

import { activateDevice } from "./device-activation.mjs";
import { requestDeviceEnrollment } from "./device-request.mjs";
import { verifyDeviceProof } from "./device-proof.mjs";
import { watchmodeSourcesRequest } from "./watchmode.mjs";
/**
 * AnyTV commercial resolver pilot.
 * Never deploy credentials or a permanent client token in an APK.
 * A server-issued pilot token protects this read-only endpoint; real customer
 * identity and authorization still matter for exposing provider API credentials.
 */
const ORIGIN = "https://api.movieofthenight.com/v4";
const ALLOWED = new Set(["netflix", "disney", "prime", "apple", "hbo"]);
const UUID = "[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}";
const LINK_PATTERNS = Object.freeze({
  netflix: new RegExp("^/title/[0-9]{6,9}/?$"),
  disney: new RegExp("^/browse/entity-" + UUID + "$"),
  prime: /^\/detail(?:\/[A-Z0-9]{10,40})?$/,
  apple: /^\/(?:[a-z]{2}\/)?(?:movie|show)\/(?:[^/]{1,120}\/)?umc\.cmc\.[a-z0-9]{15,40}$/,
  hbo: new RegExp("^/(?:movie|show)/" + UUID + "$"),
});
const HOSTS = Object.freeze({
  netflix: "www.netflix.com",
  disney: "www.disneyplus.com",
  prime: "app.primevideo.com",
  apple: "tv.apple.com",
  hbo: "play.hbomax.com",
});

function asJson(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      "x-content-type-options": "nosniff",
      ...extraHeaders,
    },
  });
}

function safeEqual(a, b) {
  if (typeof a !== "string" || typeof b !== "string") return false;
  const aa = new TextEncoder().encode(a);
  const bb = new TextEncoder().encode(b);
  let diff = aa.length ^ bb.length;
  for (let i = 0; i < Math.max(aa.length, bb.length); i++) {
    diff |= (aa[i] || 0) ^ (bb[i] || 0);
  }
  return diff === 0;
}

/** Return canonical URL or null; refuse unknown hosts, params and schemes. */
export function validateLink(provider, value) {
  if (!ALLOWED.has(provider) || typeof value !== "string" || value.length > 500) return null;
  let link;
  try {
    link = new URL(value);
  } catch {
    return null;
  }
  const trustedHost = link.hostname === HOSTS[provider] ||
    (provider === "prime" && link.hostname === "www.primevideo.com");
  if (link.protocol !== "https:" || !trustedHost ||
      link.username || link.password || link.port || link.hash) return null;
  if (!LINK_PATTERNS[provider].test(link.pathname)) return null;
  if (provider === "prime") {
    // Prime uses two official title URL shapes: catalog ASIN and video GTI.
    if (link.hostname === "www.primevideo.com") {
      if (link.search || !/^\/detail\/[A-Z0-9]{10,40}$/.test(link.pathname)) return null;
      return "https://www.primevideo.com" + link.pathname;
    }
    const keys = [...link.searchParams.keys()];
    const id = link.searchParams.get("gti");
    if (keys.length !== 1 || keys[0] !== "gti" ||
        !new RegExp("^amzn[0-9]\\.dv\\.gti\\." + UUID + "$").test(id || "")) return null;
    return "https://" + HOSTS[provider] + "/detail?gti=" + id;
  }
  if (provider === "apple") {
    // A real Apple TV+ subscription response can include playableId. Keep that
    // strictly validated value, but strip tracking/affiliate parameters.
    const allowed = new Set(["at", "ct", "itscg", "itsct", "playableId"]);
    const keys = [...link.searchParams.keys()];
    if (keys.length > 5 || keys.some((key) => !allowed.has(key)) ||
        new Set(keys).size !== keys.length) return null;
    const playableId = link.searchParams.get("playableId");
    if (playableId !== null && !/^[A-Za-z0-9._:-]{5,120}$/.test(playableId)) return null;
    return "https://" + HOSTS[provider] + link.pathname +
      (playableId ? "?playableId=" + encodeURIComponent(playableId) : "");
  }
  if (provider === "hbo") {
    if ([...link.searchParams.keys()].some((key) => key !== "utm_source")) return null;
    return "https://" + HOSTS[provider] + link.pathname;
  }
  if (link.search) return null;
  return "https://" + HOSTS[provider] +
    (provider === "netflix" ? link.pathname.replace(/\/$/, "") : link.pathname);
}

export function inspectOffers(show, expectedId, provider) {
  if (!show || typeof show !== "object" || show.tmdbId !== expectedId ||
      !show.streamingOptions || typeof show.streamingOptions !== "object") {
    throw new Error("invalid_upstream_title");
  }
  const offers = show.streamingOptions.br ?? [];
  if (!Array.isArray(offers)) throw new Error("invalid_upstream_region");
  const eligible = offers.filter((offer) =>
    offer && offer.service && offer.service.id === provider &&
    (offer.type === "subscription" || offer.type === "free") && !offer.addon);
  if (eligible.length === 0) return { status: "not_available" };
  const candidates = new Set();
  for (const item of eligible) {
    // Prefer the provider's video target when safely recognized; its
    // regular offer-page link remains a validated fallback.
    const safe = validateLink(provider, item.videoLink) ||
      validateLink(provider, item.link);
    if (safe) candidates.add(safe);
  }
  if (candidates.size === 0) return { status: "no_supported_link" };
  if (candidates.size > 1) return { status: "ambiguous" };
  return { status: "link_candidate", url: [...candidates][0] };
}

async function useCached(env, key, now) {
  const record = await env.DB.prepare(
    "SELECT payload, expires_at FROM link_cache WHERE cache_key = ? LIMIT 1"
  ).bind(key).first();
  if (!record || record.expires_at <= now) return null;
  const result = JSON.parse(record.payload);
  if (!["link_candidate", "not_available", "no_supported_link", "ambiguous"].includes(result.status)) {
    return null;
  }
  if (result.status === "link_candidate" && !validateLink(key.split(":")[2], result.url)) {
    return null;
  }
  return result;
}

async function remember(env, key, result, now) {
  const lifetime = result.status === "link_candidate" ? 6 * 3600 : 30 * 60;
  await env.DB.prepare(
    "INSERT INTO link_cache(cache_key,payload,expires_at) VALUES (?,?,?) " +
    "ON CONFLICT(cache_key) DO UPDATE SET payload=excluded.payload, expires_at=excluded.expires_at"
  ).bind(key, JSON.stringify(result), now + lifetime).run();
}

function report(result, fromCache) {
  let statusCode = 200;
  if (result.status === "not_available" || result.status === "no_supported_link") statusCode = 404;
  if (result.status === "ambiguous") statusCode = 409;
  return asJson({ ...result, verified_on_tv: false, source: "movieofthenight",
    cached: fromCache,
    attribution: "Streaming Availability API by Movie of the Night",
    attribution_url: "https://www.movieofthenight.com/about/api"
  }, statusCode);
}

/**
 * Read-only TMDB catalog proxy for the AnyTV Android client.
 * The Android package never receives or submits an API credential.
 * This endpoint stays inaccessible until deployed behind a dedicated route
 * with TMDB_READ_TOKEN configured as a Cloudflare Worker Secret.
 */
/**
 * Optional Trakt Standard public discovery signal. Only TMDB IDs and current
 * watcher counts are returned; artwork, user activity and bulk catalog copies
 * are not retained. Requires TRAKT_CLIENT_ID as a Cloudflare Worker secret.
 * Trakt attribution: AnyTV uses Trakt but is not endorsed or certified by Trakt.
 */
export async function traktSignalsRequest(request, env, deps = {}) {
  if (!env?.TRAKT_CLIENT_ID) {
    return asJson({ status: "trakt_not_configured" }, 503);
  }
  const parsed = new URL(request.url);
  if (parsed.search !== "") return asJson({ status: "bad_request" }, 400);
  const match = parsed.pathname.match(/^\/v1\/catalog\/signals\/trakt\/(movie|tv)$/);
  if (!match) return asJson({ status: "bad_request" }, 400);
  const kind = match[1];
  const cache = typeof caches !== "undefined" ? caches.default : null;
  const cached = cache ? await cache.match(request) : null;
  if (cached) return cached;
  const path = kind === "movie" ? "movies/trending" : "shows/trending";
  try {
    const transport = deps.fetch || fetch;
    const upstream = await transport("https://api.trakt.tv/" + path + "?limit=50&page=1", {
      headers: {
        "trakt-api-version": "2",
        "trakt-api-key": env.TRAKT_CLIENT_ID,
        Accept: "application/json",
      },
      redirect: "manual",
      signal: AbortSignal.timeout(6500),
    });
    if (!upstream.ok) return asJson({ status: "trakt_unavailable" }, 503);
    const response = await upstream.json();
    if (!Array.isArray(response)) return asJson({ status: "trakt_invalid_response" }, 503);
    const seen = new Set();
    const results = response.slice(0, 50).map(entry => {
      const id = entry?.[kind === "movie" ? "movie" : "show"]?.ids?.tmdb;
      const watchers = entry?.watchers;
      if (!Number.isSafeInteger(id) || id < 1 || id > 999999999 ||
          !Number.isSafeInteger(watchers) || watchers < 0 || seen.has(id)) return null;
      seen.add(id);
      return { id, watchers: Math.min(watchers, 1000000) };
    }).filter(Boolean);
    const body = asJson({ source: "trakt", kind, results }, 200,
      { "cache-control": "public, max-age=900" });
    if (cache) await cache.put(request, body.clone());
    return body;
  } catch {
    return asJson({ status: "trakt_unavailable" }, 503);
  }
}

export async function catalogRequest(request, env, deps = {}) {
  if (!env?.TMDB_READ_TOKEN || !env?.DB) {
    return asJson({ status: "catalog_not_configured" }, 503);
  }
  const parsed = new URL(request.url);
  const route = parsed.pathname.replace(/^\/v1\/catalog\//, "");
  if (/^signals\/trakt\/(movie|tv)$/.test(route))
    return traktSignalsRequest(request, env, deps);
  const patterns = [
    /^genre\/(movie|tv)\/list$/,
    /^watch\/providers\/(movie|tv)$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/watch\/providers$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/external_ids$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}$/,
    /^tv\/[1-9][0-9]{0,9}\/season\/[0-9]{1,3}$/,
    /^(discover|search)\/(movie|tv)$/,
    /^trending\/(movie|tv)\/(day|week)$/,
    /^(movie|tv)\/(popular|top_rated)$/,
    /^movie\/(now_playing|upcoming)$/,
    /^tv\/(on_the_air|airing_today)$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/(videos|similar|recommendations)$/,
  ];
  if (!patterns.some(p => p.test(route))) return asJson({ status: "bad_request" }, 400);
  const allowed = new Set([
    "language", "watch_region", "with_watch_providers",
    "with_watch_monetization_types", "include_adult",
    "sort_by", "page", "query", "with_genres", "region",
  ]);
  const keys = [...parsed.searchParams.keys()];
  if (keys.length > 9 || keys.some(k => !allowed.has(k)) ||
      new Set(keys).size !== keys.length) {
    return asJson({ status: "bad_request" }, 400);
  }
  // A watched-film ID lookup needs only localized read-only metadata.
  // Keep all other discovery, provider and arbitrary query parameters blocked.
  if (/^movie\/[1-9][0-9]{0,9}$/.test(route) &&
      keys.some(k => k !== "language")) {
    return asJson({ status: "bad_request" }, 400);
  }
  const page = parsed.searchParams.get("page") || "1";
  if (!/^[1-9][0-9]?$/.test(page) || Number(page) > 50) {
    return asJson({ status: "bad_request" }, 400);
  }
  const provider = parsed.searchParams.get("with_watch_providers");
  if (provider !== null && !/^[1-9][0-9]{0,5}$/.test(provider)) {
    return asJson({ status: "bad_request" }, 400);
  }
  const genre = parsed.searchParams.get("with_genres");
  if (genre !== null && !/^[1-9][0-9]{0,5}$/.test(genre)) {
    return asJson({ status: "bad_request" }, 400);
  }
  const query = parsed.searchParams.get("query");
  if (query !== null && (query.length < 1 || query.length > 90)) {
    return asJson({ status: "bad_request" }, 400);
  }
  const region = parsed.searchParams.get("region");
  if (region !== null && (route !== "movie/now_playing" || region !== "BR")) {
    return asJson({ status: "bad_request" }, 400);
  }
  const lang = parsed.searchParams.get("language") || "pt-BR";
  // TMDB sorting is restricted to official discover endpoints; never forward
  // arbitrary query expressions or mutate unrelated search/metadata routes.
  const sort = parsed.searchParams.get("sort_by") || "popularity.desc";
  const discoverSorts = route === "discover/movie"
    ? new Set(["popularity.desc", "vote_average.desc",
        "primary_release_date.desc", "title.asc"])
    : route === "discover/tv"
      ? new Set(["popularity.desc", "vote_average.desc",
          "first_air_date.desc", "name.asc"])
      : new Set(["popularity.desc"]);
  const trailerLanguage = /^(movie|tv)\/[1-9][0-9]{0,9}\/videos$/.test(route);
  if ((parsed.searchParams.get("watch_region") || "BR") !== "BR" ||
      (lang !== "pt-BR" && !(trailerLanguage && lang === "en-US")) ||
      (parsed.searchParams.get("include_adult") || "false") !== "false" ||
      (parsed.searchParams.get("with_watch_monetization_types") || "flatrate") !== "flatrate" ||
      !discoverSorts.has(sort)) {
    return asJson({ status: "bad_request" }, 400);
  }
  let diagnosticStage = "cache_read";
  try {
    const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
    const upstream = new URL("https://api.themoviedb.org/3/" + route);
    for (const [k, v] of parsed.searchParams) upstream.searchParams.set(k, v);
    upstream.searchParams.set("language", lang);
    if (route.startsWith("discover/") || route.startsWith("watch/providers/")) {
      upstream.searchParams.set("watch_region", "BR");
    }
    const key = "tmdb:" + upstream.pathname + upstream.search;
    diagnosticStage = "cache_read";
    const cached = await env.DB.prepare(
      "SELECT payload,expires_at FROM link_cache WHERE cache_key = ? LIMIT 1"
    ).bind(key).first();
    if (cached && cached.expires_at > now) {
      return asJson(JSON.parse(cached.payload), 200, { "x-anytv-cache": "hit" });
    }
    // TMDB metadata is not charged against the separate Movie of the Night link budget.
    // Requests remain path-allowlisted, validated and cached without user-usage caps.
    // TMDB offers either a v4 Read Access Token (Bearer) or a 32-char v3 API key.
    // Both stay exclusively in Cloudflare Secrets; no credential reaches the app.
    const credential = String(env.TMDB_READ_TOKEN).trim().replace(/^Bearer\s+/i, "").replace(/\s+/g, "");
    const v3Key = /^[a-f0-9]{32}$/i.test(credential);
    if (v3Key) upstream.searchParams.set("api_key", credential);
    const transport = deps.fetch || fetch;
    diagnosticStage = "upstream_fetch";
    const reply = await transport(upstream.toString(), {
      headers: { ...(v3Key ? {} : { Authorization: "Bearer " + credential }),
        Accept: "application/json" },
      redirect: "manual",
      signal: AbortSignal.timeout(8000),
    });
    if (!reply.ok) {
      // An unknown movie ID is not an outage: let the Android history restore
      // skip this ID and try the next one, without caching negative responses.
      if (/^movie\/[1-9][0-9]{0,9}$/.test(route) && reply.status === 404)
        return asJson({ status: "catalog_not_found" }, 404);
      return asJson({ status: "catalog_unavailable" }, 503,
        { "x-anytv-upstream-status": String(reply.status) });
    }
    diagnosticStage = "upstream_body";
    const payload = await reply.text();
    if (payload.length > 450000) return asJson({ status: "catalog_unavailable" }, 503);
    const parsedPayload = JSON.parse(payload);
    diagnosticStage = "cache_write";
    // Cache validated read-only TMDB metadata to reduce upstream load.
    // Search and availability have route-specific expiration.
    const cacheTtl = route.startsWith("genre/") || route.startsWith("watch/providers/")
      ? 7 * 86400
      : route.endsWith("/watch/providers") ? 12 * 3600
      : route.startsWith("search/") ? 24 * 3600
      : route.startsWith("trending/") || route.includes("now_playing")
        ? 6 * 3600 : 24 * 3600;
    await env.DB.prepare(
      "INSERT INTO link_cache(cache_key,payload,expires_at) VALUES (?,?,?) " +
      "ON CONFLICT(cache_key) DO UPDATE SET payload=excluded.payload,expires_at=excluded.expires_at"
    ).bind(key, JSON.stringify(parsedPayload), now + cacheTtl).run();
    return asJson(parsedPayload);
  } catch (error) {
    const detail = error instanceof Error ? String(error.message) : "";
    const sanitized = detail.replaceAll(String(env.TMDB_READ_TOKEN || ""), "[REDACTED]")
      .replace(/https?:\/\/\S+/g, "[URL]")
      .replace(/[A-Za-z0-9._+=\/-]{24,}/g, "[LONG]")
      .replace(/[^\x20-\x7e]/g, " ").slice(0,140);
    // Diagnostic code only: never include upstream URLs, credentials or messages.
    const diagnostic = /Invalid URL/i.test(detail) ? "invalid_url"
      : /Fetch API cannot load/i.test(detail) ? "cf_fetch_denied"
      : /outbound|not permitted|not allowed|blocked/i.test(detail) ? "outbound_block"
      : /fetch is not|not a function/i.test(detail) ? "fetch_binding"
      : /Invalid header|ByteString|Invalid character/i.test(detail) ? "bad_header"
      : /network connection lost|network error/i.test(detail) ? "network_lost"
      : /AbortSignal|signal/i.test(detail) ? "bad_signal"
      : "unknown";
    const reason = /header|ByteString|Invalid character/i.test(detail) ? "invalid_header"
      : /timed|abort/i.test(detail) ? "timeout"
      : /fetch|network|DNS|connect/i.test(detail) ? "network"
      : /signal/i.test(detail) ? "signal"
      : "unknown";
    return asJson({ status: "catalog_unavailable" }, 503,
      { "x-anytv-failure-stage": diagnosticStage,
        "x-anytv-failure-type": error instanceof Error ? error.name : "unknown",
        "x-anytv-failure-reason": reason,
        "x-anytv-failure-detail-code": diagnostic,
        "x-anytv-sanitized-error": sanitized });
  }
}

/**
 * Inject clock/fetch for deterministic unit tests.
 * Does NOT authorize replaying arbitrary link strings from callers.
 */
export async function handleRequest(request, env, deps = {}) {
  const parsed = new URL(request.url);
  // Enrollment remains disabled by default, including on existing deployments.
  if (parsed.pathname === "/v1/device/activate" &&
      env?.ANYTV_DEVICE_ACTIVATION_ENABLED === "true") {
    return activateDevice(request, env, deps);
  }
  // An enrollment request proves ownership of its key; it never authorizes
  // streaming links until the existing D1 row is explicitly enabled by an operator.
  if (parsed.pathname === "/v1/device/request" &&
      env?.ANYTV_RESOLVER_AUTH_MODE === "device" &&
      env.ANYTV_DEVICE_ACTIVATION_ENABLED === "true") {
    return requestDeviceEnrollment(request, env, deps);
  }
  if (request.method !== "GET") return asJson({ status: "method_not_allowed" }, 405);
  // A signed status check reveals only whether THIS key is enabled, not keys
  // or enrollment records. No quotas or shared client credentials are involved.
  if (parsed.pathname === "/v1/device/status") {
    if (!env?.DB || env.ANYTV_RESOLVER_AUTH_MODE !== "device")
      return asJson({ status: "not_configured" }, 503);
    const authorized = await verifyDeviceProof(request, env, deps);
    return asJson({ status: authorized ? "activated" : "unauthorized" },
      authorized ? 200 : 401);
  }
  if (parsed.pathname === "/health") return asJson({ status: "ok", phase: "pilot" });
  if (parsed.pathname.startsWith("/v1/catalog/watchmode/"))
    return watchmodeSourcesRequest(request, env, { ...deps, verifyDeviceProof });
  if (parsed.pathname.startsWith("/v1/catalog/")) return catalogRequest(request, env, deps);
  if (!env?.ANYTV_MOVIE_API_KEY || !env?.DB ||
      ((env.ANYTV_RESOLVER_AUTH_MODE || "pilot") === "pilot" &&
       !env.ANYTV_CLIENT_TOKEN)) {
    return asJson({ status: "not_configured" }, 503);
  }
  // Explicit opt-in for preapproved device proofs. Existing pilot-token
  // deployments stay unchanged until an authenticated enrollment is available.
  // An unknown mode must fail closed, not silently select the pilot bypass.
  const authMode = env.ANYTV_RESOLVER_AUTH_MODE || "pilot";
  if (authMode !== "pilot" && authMode !== "device") {
    return asJson({ status: "not_configured" }, 503);
  }
  if (authMode === "device") {
    if (!(await verifyDeviceProof(request, env, deps))) {
      return asJson({ status: "unauthorized" }, 401);
    }
  } else if (!safeEqual(request.headers.get("authorization") || "",
      "Bearer " + env.ANYTV_CLIENT_TOKEN)) {
    return asJson({ status: "unauthorized" }, 401);
  }
  const match = parsed.pathname.match(
    /^\/v1\/resolve\/(movie|tv)\/([1-9][0-9]{0,9})\/(netflix|disney|prime|apple|hbo)$/
  );
  if (!match || [...parsed.searchParams].length !== 0) {
    return asJson({ status: "bad_request" }, 400);
  }
  const [, media, id, provider] = match;
  try {
    const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
    const key = media + ":" + id + ":" + provider + ":br";
    const cached = await useCached(env, key, now);
    if (cached) return report(cached, true);
    const sourceUrl = new URL(ORIGIN + "/shows/" + media + "%2F" + id);
    sourceUrl.searchParams.set("country", "br");
    sourceUrl.searchParams.set("series_granularity", "show");
    const transport = deps.fetch || fetch;
    const reply = await transport(sourceUrl.toString(), {
      headers: {
        "X-API-Key": env.ANYTV_MOVIE_API_KEY,
        Accept: "application/json",
      },
      redirect: "manual",
      signal: AbortSignal.timeout(8000),
    });
    if (!reply.ok) return asJson({ status: "source_unavailable" }, 503);
    const len = Number(reply.headers.get("content-length") || "0");
    if (len > 450000) return asJson({ status: "source_unavailable" }, 503);
    const item = await reply.json();
    const resolved = inspectOffers(item, media + "/" + id, provider);
    await remember(env, key, resolved, now);
    return report(resolved, false);
  } catch {
    // No upstream payload, customer token or other secrets in errors/logs.
    return asJson({ status: "resolver_unavailable" }, 503);
  }
}

export default {
  async fetch(request, env) {
    return handleRequest(request, env);
  },
};

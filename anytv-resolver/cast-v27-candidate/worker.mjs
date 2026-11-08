import { activateDevice } from "./device-activation.mjs";
import { requestDeviceEnrollment } from "./device-request.mjs";
import { verifyDeviceProof } from "./device-proof.mjs";
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
  const parsed = new URL(request.url);
  const route = parsed.pathname.replace(/^\/v1\/catalog\//, "");
  // Trakt public trends require only the server-side Trakt Client ID.
  // TMDB and D1 outages must not disable these independent signals.
  if (/^signals\/trakt\/(movie|tv)$/.test(route))
    return traktSignalsRequest(request, env, deps);
  if (!env?.TMDB_READ_TOKEN || !env?.DB) {
    return asJson({ status: "catalog_not_configured" }, 503);
  }
  const patterns = [
    /^genre\/(movie|tv)\/list$/,
    /^watch\/providers\/(movie|tv)$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/watch\/providers$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/external_ids$/,
    /^(movie|tv)\/[1-9][0-9]{0,9}\/credits$/,
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
  if (provider !== null && !/^[1- --- TRUNCATED --- 21,995 chars
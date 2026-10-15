/**
 * AnyTV commercial resolver pilot.
 * Never deploy credentials or a permanent client token in an APK.
 * A server-issued pilot token protects this read-only endpoint; real customer
 * identity, entitlements and per-account quotas are required before launch.
 */
const ORIGIN = "https://api.movieofthenight.com/v4";
const ALLOWED = new Set(["netflix", "disney", "prime", "apple", "hbo"]);
const UUID = "[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}";
const LINK_PATTERNS = Object.freeze({
  netflix: new RegExp("^/title/[0-9]{6,9}$"),
  disney: new RegExp("^/browse/entity-" + UUID + "$"),
  prime: /^\/detail$/,
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

function monthlyCap(env) {
  const n = Number(env.MAX_UPSTREAM_CALLS_MONTH || "200");
  return Number.isSafeInteger(n) && n > 0 && n <= 10000 ? n : null;
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
  if (link.protocol !== "https:" || link.hostname !== HOSTS[provider] ||
      link.username || link.password || link.port || link.hash) return null;
  if (!LINK_PATTERNS[provider].test(link.pathname)) return null;
  if (provider === "prime") {
    const keys = [...link.searchParams.keys()];
    const id = link.searchParams.get("gti");
    if (keys.length !== 1 || keys[0] !== "gti" ||
        !new RegExp("^amzn[0-9]\\.dv\\.gti\\." + UUID + "$").test(id || "")) return null;
    return "https://" + HOSTS[provider] + "/detail?gti=" + id;
  }
  if (provider === "apple") {
    // The path is the Apple TV title identifier. Omit affiliate/tracking values.
    // playableId is used for a purchase/rental path and is not needed here.
    const allowed = new Set(["at", "ct", "itscg", "itsct"]);
    if ([...link.searchParams.keys()].some((key) => !allowed.has(key))) return null;
    return "https://" + HOSTS[provider] + link.pathname;
  }
  if (provider === "hbo") {
    if ([...link.searchParams.keys()].some((key) => key !== "utm_source")) return null;
    return "https://" + HOSTS[provider] + link.pathname;
  }
  if (link.search) return null;
  return "https://" + HOSTS[provider] + link.pathname;
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
    const safe = validateLink(provider, item.link);
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

async function reserveQuota(env, now) {
  const month = new Date(now * 1000).toISOString().slice(0, 7);
  await env.DB.prepare(
    "INSERT OR IGNORE INTO monthly_quota (month, used) VALUES (?, 0)"
  ).bind(month).run();
  const reserved = await env.DB.prepare(
    "UPDATE monthly_quota SET used = used + 1 WHERE month = ? AND used < ? RETURNING used"
  ).bind(month, monthlyCap(env)).first();
  return reserved !== null;
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
 * Inject clock/fetch for deterministic unit tests.
 * Does NOT authorize replaying arbitrary link strings from callers.
 */
export async function handleRequest(request, env, deps = {}) {
  if (request.method !== "GET") return asJson({ status: "method_not_allowed" }, 405);
  const parsed = new URL(request.url);
  if (parsed.pathname === "/health") return asJson({ status: "ok", phase: "pilot" });
  if (!env?.ANYTV_CLIENT_TOKEN || !env?.ANYTV_MOVIE_API_KEY ||
      !env?.DB || !env?.RATE_LIMIT || monthlyCap(env) === null) {
    return asJson({ status: "not_configured" }, 503);
  }
  if (!safeEqual(request.headers.get("authorization") || "",
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
  const ip = request.headers.get("cf-connecting-ip") || "unknown";
  try {
    const limit = await env.RATE_LIMIT.limit({ key: ip });
    if (!limit.success) return asJson({ status: "rate_limited" }, 429);
    const now = Math.floor((deps.now ? deps.now() : Date.now()) / 1000);
    const key = media + ":" + id + ":" + provider + ":br";
    const cached = await useCached(env, key, now);
    if (cached) return report(cached, true);
    if (!(await reserveQuota(env, now))) {
      return asJson({ status: "monthly_quota_exhausted" }, 429);
    }
    const sourceUrl = new URL(ORIGIN + "/shows/" + media + "%2F" + id);
    sourceUrl.searchParams.set("country", "br");
    sourceUrl.searchParams.set("series_granularity", "show");
    const transport = deps.fetch || fetch;
    const reply = await transport(sourceUrl.toString(), {
      headers: {
        "X-API-Key": env.ANYTV_MOVIE_API_KEY,
        Accept: "application/json",
      },
      redirect: "error",
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

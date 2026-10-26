// Non-commercial Watchmode Developer adapter. Fail-closed, no credentials in APK.
const json = (payload, status) => new Response(JSON.stringify(payload), {
  status, headers: { "content-type": "application/json; charset=utf-8", "cache-control": "no-store" },
});

export async function watchmodeSourcesRequest(request, env, deps = {}) {
  if (env?.ANYTV_WATCHMODE_ENABLED !== "true" || !env?.WATCHMODE_API_KEY)
    return json({ status: "watchmode_not_configured" }, 503);
  if (env?.ANYTV_RESOLVER_AUTH_MODE !== "device")
    return json({ status: "watchmode_device_auth_required" }, 503);
  if (!(await deps.verifyDeviceProof(request, env, deps)))
    return json({ status: "unauthorized" }, 401);
  const uri = new URL(request.url);
  const match = uri.pathname.match(/^\/v1\/catalog\/watchmode\/(movie|tv)\/([1-9][0-9]{0,9})\/sources$/);
  if (!match || uri.search) return json({ status: "bad_request" }, 400);
  const [, kind, tmdbId] = match;
  // Do not cache private device-authenticated responses in shared edge cache.
  // The provider enforces its own monthly allowance; no local credit quota.

  // TMDB-formatted IDs use 'movie-' and 'tv-' on Watchmode.
  const title = kind + "-" + tmdbId;
  const endpoint = "https://api.watchmode.com/v1/title/" + title + "/sources/?regions=BR";
  try {
    const upstream = await (deps.fetch || fetch)(endpoint, {
      headers: { "X-API-Key": env.WATCHMODE_API_KEY, Accept: "application/json" },
      redirect: "manual", signal: AbortSignal.timeout(6000),
    });
    if (!upstream.ok) return json({ status: "watchmode_unavailable" }, 503);
    const data = await upstream.json();
    if (!Array.isArray(data)) return json({ status: "watchmode_invalid_response" }, 503);
    const sources = data.filter(item =>
      item && item.region === "BR" && item.type === "sub" &&
      Number.isSafeInteger(item.source_id) && item.source_id > 0
    ).slice(0, 50).map(item => ({
      providerId: item.source_id, provider: String(item.name || "").slice(0, 100),
      offer: "subscription",
    }));
    const result = new Response(JSON.stringify({
      source: "watchmode", attribution: "Streaming availability by Watchmode",
      region: "BR", kind, tmdbId: Number(tmdbId), sources,
    }), { status: 200, headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "private, max-age=3600",
    } });
    return result;
  } catch {
    return json({ status: "watchmode_unavailable" }, 503);
  }
}

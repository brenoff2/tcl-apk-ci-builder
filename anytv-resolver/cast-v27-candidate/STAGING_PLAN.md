# AnyTV v27 cast-only — staging and rollback specification

Status: **DESIGN_ONLY / NO_CLOUDFLARE_MUTATION / NOT_APPROVED_FOR_DEPLOY**.  
Scope: confirm movie/TV actors for Android details, not Samsung and not an Android release.

## Verified baseline and fault

- Live Worker: `anytv-link-resolver-pilot`, account `48172e80b82e7f0a2100e512831c48d2`.
- Active Cloudflare deployment `e94c3bad-bcac-4b40-bad9-a317d7967563`, version 27 `88779b1b-9dbd-4dc6-9884-eff52769ded2` serving 100% of traffic.
- Public `movie/550/credits` and `tv/1399/credits` returned HTTP 400 `bad_request` in GitHub Actions `42512289845`.
- Active Worker source from authenticated GET `content/v2`: exactly one missing `/credits` allowlist regexp. Four modules were compared against the isolated candidate: `worker.mjs` differs only by the one added regexp line; other three modules are byte-identical.
- Isolated branch `fix/anytv-v27-cast-only-20261108`, test run `42514148321` SUCCESS, **offline only**.
- Cloudflare inventory (authenticated GET): exactly **one** D1 (production `9bab6598-d019-4e88-8b29-a327ff0cfd00`) and **no** dedicated AnyTV staging Worker/D1.

## Minimum isolated staging resources — requires explicit separate approval

| Resource | Proposed identity | Invariant |
| --- | --- | --- |
| D1 | `anytv-cast-staging` | New ID; MUST NOT equal production D1 `9bab6598-d019-4e88-8b29-a327ff0cfd00` |
| Worker | `anytv-cast-staging` | Never update `anytv-link-resolver-pilot` during staging |
| Worker source | Four audited modules under `cast-v27-candidate/` | No whole-DEV Worker substitution |
| Compatibility date | `2026-10-15` | Match active production version |
| `DB` binding | Dedicated staging D1 ID | No reads/writes to production D1 |
| `TMDB_READ_TOKEN` | Staging-scoped authorized test credential | Never copy or print existing production secret |
| `ANYTV_DEVICE_ACTIVATION_ENABLED` | `false` | Never enable enrollment while staging |
| `ANYTV_RESOLVER_AUTH_MODE` | `device` with no provisioned keys | Fail closed for link resolution |
| Other provider credentials | Omit in staging | Out-of-scope provider/Trakt requests should fail closed |

Do not create a staging Version URL that exposes live services without a deliberate access-control review. Cloudflare Version URLs may use bound resources; an uploaded preview version that still points at the production D1 is **not isolated**. An empty staging D1 is expected: tests create their own synthetic records or cache and do not copy client or activation data from production.

## Minimum D1 schema

- Use existing canonical migration `migrations/0001_create_tables.sql` for `link_cache` and its expiration index. The historical `monthly_quota` table is created by the original migration but is not exercised by the v27 catalog proxy.
- Optionally use `0002_device_proof.sql` and `0003_device_activations.sql` only to test complete device-protocol error paths; **do not** insert enrolled/active devices or activation codes. These are not necessary to test movie/TV credits.
- Do **not** apply `0004_user_library_sync.sql`: it explicitly requires a separate authenticated user verifier and is outside the cast fix.
- Verify by readback the staging D1 UUID, tables, and binding; block if any reference equals the production D1 UUID.

## Validation gates

1. **Approval gate:** authorized owner approves staging resource creation and credentials; never infer production deployment approval from staging approval.
2. **Isolation gate:** readback names, account, D1 UUID, Worker name, compatibility date and bindings; prove no production D1, no production secret reuse, no production route.
3. **Offline gate:** `42514148321` PASS; preserve SHA of all four modules and compare against active source, with only one allowed regexp addition.
4. **Staging gate:** with authorized staging credentials and private access, GET `/v1/catalog/movie/550/credits` and `/v1/catalog/tv/1399/credits`; require HTTP 200 and a nonempty `cast` containing `name`, `character`, `profile_path` where available. Repeat calls and check cache; reject invalid IDs/paths and non-PT-BR requests. Test existing metadata/search/trailer routes for regression.
5. **Preproduction gate:** require fresh GET of production deployment/version/traffic, preflight and rollback plan. New production version must preserve all bindings, compatibility flags and modules; no D1 migrations, no Android OTA.
6. **Deployment gate:** explicit separate authorization; change production Worker only via governed release; capture previous deployment/version and compare traffic percentages. Do not treat candidate GitHub commit as evidence of live deployment.
7. **Readback gate:** live HTTP 200 with credits for both media types; verify metadata and unrelated endpoints, correct image URL formatting, logs and error rates. If failed, roll back to version `88779b1b-9dbd-4dc6-9884-eff52769ded2`, confirm 100% traffic and repeat smoke. Worker rollback does NOT revert D1 data.
8. **Android gate:** only after backend live proof, decide whether to sign/publish code91; on-device visual QA remains NOT_RUN.

## Exclusions and explicit stop conditions

- No shared production D1, secrets, or activation records.
- No updates to CLFCJ Workers, no unrelated Cloudflare resources.
- No creation, deploy, Version URL exposure, signing, or OTA mutation from this document.
- Stop if no separate staging D1 / authorized testing credential, if Cloudflare scope is not authorized, or if the production version has changed since the recorded baseline.

References:
- [Cloudflare D1 getting started](https://developers.cloudflare.com/d1/get-started/)
- [Cloudflare Worker versions and deployments](https://developers.cloudflare.com/workers/versions-and-deployments/)
- [Cloudflare Version URLs](https://developers.cloudflare.com/workers/versions-and-deployments/version-urls/)
- Canonical migrations and `.github/workflows/anytv-p0-d1-staging-preflight.yml` in repository.

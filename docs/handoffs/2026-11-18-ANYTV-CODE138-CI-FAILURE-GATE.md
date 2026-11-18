# AnyTV SQLite v37 — code138 CI failure gate (2026-11-18)

## Objective and immutable boundaries
Continue approved DEV shared drawer/account UI. Preserve main, Worker/D1, TV companion, existing signer/cert2026, and public OTA branch `feat/tcl-v22-updates-back-bluetooth` at `6bc297fc5b19b02a19ec5401165bf69e4b0a5f13` / code137. No OTA, merge, key change, production write, or automatic installation is authorized.

## Fresh independent GitHub Actions readback
- Signed candidate code138: run https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/44362912413; job 135009012668, completed FAILURE. Failure at step 14 `Existing UI, link and safety contract tests`; package/compile/sign/archive steps were SKIPPED, no artifacts.
- Exact assertion: `test_real_installed_apps_are_the_source`, `scripts/tests/test_streaming_screens_contract.py:502`; test expects `TclStreamingScreens(apps = allApps, host = host, pairing = pairing`, while assembled `TclInstalledApps.kt` has `apps = if (inventoryVerified) allApps else emptyList()`. Test source branches on literal `"val inventoryVerified"` but assembled file declares `var inventoryVerified`. Repair test variant identification without loosening inventory gates.
- Unsigned: run https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/44362913089; job 135008836359, completed FAILURE at `:app:compileReleaseKotlin`, no artifacts. Four missing references in assembled `TclStreamingScreens.kt`: `LgAppIcon` and `LgWebOsApp` near 1707 and 3591.
- Real definitions ARE present in `overrides/LgWebOsAppsBar.kt`; dedicated workflow `.github/workflows/anytv-lg-webos-unsigned-ci.yml` runs `scripts/apply_anytv_lg_apps.py`, which copies the LG implementation. General unsigned variant did not compile against those symbols. Repair the assembly/build source composition, reuse the real implementation; do not invent placeholder classes or remove LG support.
- P0 run 44362912479 reported SUCCESS by user; independent run verification NOT_TESTED in this readback.

## Required next safe action
1. Validate exact current DEV HEAD and both workflow assembly paths at matching SHA.
2. Minimal targeted test fix recognizing `var inventoryVerified` and verifying real verified inventory in ADB variant; preserve normal companion app feed.
3. Minimal fix to include real LG icon/model components in all variants using shared streaming screens, without changing transports, device logic, secrets or published paths.
4. Re-run actual signed/unsigned builds; inspect logs, package `com.tclremote.livre.v2`, `versionCode=138`, V2 certificate/signature, checksum, artifacts. Only then prepare MANUAL install, NEVER OTA.
5. Physical Android/TCL/LG account/drawer/session QA remains NOT_TESTED; remote account deletion endpoint remains BLOCKED.

## State
- Decision: BLOCK release; both CI runs FAILED. No code fix or new build attempted during this readback.
- Roadmap: 7/11 (63%) unchanged.
- SQLite `anytv`: NOT_VERIFIED in this turn; previous reported permission denial. Do not assert persisted.
- OpenViking task `8417856e-c876-4cce-8a52-e64414134ee5`: PENDING per user; live status NOT_VERIFIED.
- GitHub STATUS baseline reported commit `ec7bff77f6dcc82fe1b9b2c9ee34609b693b4124`; this independent handoff records the later evidence. Canonical STATUS was not edited in this action.

## Continuation — surgical DEV corrections and independent readback
- P0 job 135008833596, run 44362912479: `completed/success` verified via GitHub connector (not a substitute for Android build/QA).
- Corrected signed contract at `scripts/tests/test_streaming_screens_contract.py`: `31a9aaeca82942266febeed679832d5fe08e097f`; explicitly accepts immutable `val` and mutable `var inventoryVerified` while requiring the verified app-selection gate.
- Compiled-source composition: `scripts/apply_tcl_v22.py` now copies the existing `overrides/LgWebOsAppsBar.kt` into the prepared Android source (`2a0d4eae0dbd7d5a0848f01da8e84ddf61bf5dc2`). LG-specific `scripts/apply_anytv_lg_apps.py` now accepts it **only when identical to the canonical source** (`50fe489392c73fa3f3919e4b123c97141f06ba23`), otherwise fails closed.
- GitHub readback of all three changed files at the last commit was confirmed. No Kotlin/Gradle run was executed or found for that commit via available commit-status queries: `NOT_TESTED`. No signed code138 artifact exists from the prior failed runs, and there is NO authorization to use the legacy OTA-publishing signed workflow as a shortcut.
- SQLite multiproject checkpoint `anytv`: updated with optimistic version `37 -> 38` and readback matching operation `anytv-code138-root-cause-patches-20261118`, `BLOCKED`, evidence SHA `25e4ca4b9d6272763332e17bbac1464c49f137dcedc73d7dafea7c4c9426b052`.
- OpenViking task `8417856e-c876-4cce-8a52-e64414134ee5`: status still not confirmed; keyword search was unsupported by configured vector backend, semantic attempt failed. No memory write claimed.
- Next: identify and run only unsigned QA and **manual signed-candidate without OTA**; inspect Gradle compile logs and artifact checks, validate `com.tclremote.livre.v2`, code138, V2 certificate and SHA, then user-driven manual install and physical QA. Retain published OTA code137 and main/Worker/D1/TV/companion/cert2026 untouched.

## Continuation — GitHub Actions complete listing and dispatch gate (18/11/2026)
- GitHub compare proved DEV HEAD `c5a28f1733e53d3d0f81161290fb73c6f0c4ad7c` EXACT, no divergence from the preceding checkpoint.
- GitHub Actions (official upstream through governed Hub) listed recent runs: P0 `44378823608` HEAD `c5a28f17` completed SUCCESS. Earlier P0 `44377878309` at `50fe4893` SUCCESS. LG code100 unsigned `44377878426`, code101 `44377878008`, code102 `44377878053`, code103 `44377878246` on `50fe4893` all SUCCESS. These are different variants and DO NOT prove code138 signed.
- LG code98 unsigned `44377878528` FAILED in Gradle resolution of `org.gradle.toolchains.foojay-resolver-convention:1.0.0` from `source/settings.gradle.kts:19`, before Kotlin compilation; cannot attribute this failure to LG UI integration. Code99 `44377877957` CANCELLED. No blind plugin bypass.
- Independent examination of `.github/workflows/anytv-menu-code138-signed-candidate.yml`: pins versionCode 138, versionName 2.9.110-AnyTV, package check V2, compiled Kotlin/Compose unit tests, source/contract gates, existing signer verification and archived artifact `anytv-v2-29110-code138-signed-manual-candidate`. Workflow has **NO OTA publication** step.
- Workflow dispatch attempted exclusively for this safe signed candidate using official GitHub provider through governed MCP with `confirm_write=true`, returned `BLOCKED: DESTRUCTIVE_CONFIRMATION_REQUIRED`. Stop; do not retry by adding confirmation flags or alternative ungoverned route.
- Deprecated `.github/workflows/anytv-v2-candidate-compile.yml` is **NOT SAFE** for this goal: contains direct `git push` to `updates/android/v2/current.apk`. Never trigger it.
- GitHub comparison independently confirmed public OTA branch still EXACT `6bc297fc5b19b02a19ec5401165bf69e4b0a5f13`, 0 ahead/behind; code137 preserved.
- OpenViking `viking://user/factory-hub-agent/resources/anytv/continuity.md` was readable, but content dated 29/10/2026 (SQLite v6) and not evidence of new memory task `8417856e-c876-4cce-8a52-e64414134ee5` completing. Task status NOT_VERIFIED.
- Current release decision BLOCK. No code changes, signing, OTA, merge, infrastructure writes or manual install in this continuation. Need approved execution of safe candidate workflow through the GitHub Actions UI/provider gate, then readback of run logs and artifact identity/certificate. Physical QA still pending.

## Continuation — verified signed code138 from older SHA (18/11/2026 evening)
- **New GitHub Actions evidence:** the NO-OTA signed workflow `anytv-menu-code138-signed-candidate.yml` run [44377696022](https://github.com/brenoff2/tcl-apk-ci-builder/actions/runs/44377696022), source SHA `31a9aaeca82942266febeed679832d5fe08e097f`, concluded `SUCCESS` with job `135061146532`. Kotlin/Compose compiled tests and release `assembleRelease` succeeded; native existing signer recovered and checked with `apksigner verify` against the V2 certificate.
- Output artifact `anytv-v2-29110-code138-signed-manual-candidate` ID `14464105995`, recorded size `26499258` bytes (GitHub ZIP artifact size), retention expiry `2026-12-02T19:21:21Z`. Log marker signed APK SHA256 `712f00e2a0f9e00ca3d3320cfd122090d99d0ce5ca69435dc48fa867ab1d3c0f`; checks in workflow assert `com.tclremote.livre.v2`, `versionCode=138`, `2.9.110-AnyTV`, original V2 certificate and APK Signature Scheme v2.
- **Important scope:** that successful workflow ran **before** DEV LG component integration commits `2a0d4eae` and `50fe4893`; it is NOT a build of current DEV HEAD `4bb812103585cb1c9c6788a04775af8a9b04f6db`. Do not label the artifact as QA-approved current candidate or install as the final corrected release.
- Current repository branch HEAD remained at `4bb812103585cb1c9c6788a04775af8a9b04f6db` (exact compare). Public OTA branch independently preserved at `6bc297fc5b19b02a19ec5401165bf69e4b0a5f13`.
- A previous authorized dispatch attempt for current HEAD returned `DESTRUCTIVE_CONFIRMATION_REQUIRED`. No bypass permitted; no new run for current HEAD appears in the nine-run signed workflow listing. Next: approved safe workflow dispatch from GitHub UI and readback for current HEAD, then physical QA. If dispatch is unavailable, remain BLOCKED.

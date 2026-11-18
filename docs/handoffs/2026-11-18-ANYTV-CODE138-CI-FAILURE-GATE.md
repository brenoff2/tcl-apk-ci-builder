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

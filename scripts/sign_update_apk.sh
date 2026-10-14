#!/usr/bin/env bash
# Fail-closed Android signing: only an already-approved certificate may sign.
# Usage: sign_update_apk.sh --check | sign_update_apk.sh INPUT_APK OUTPUT_APK
set -euo pipefail

if [[ ${1:-} == '--check' && $# == 1 ]]; then
  check_only=1
elif [[ $# == 2 ]]; then
  check_only=0
  input_apk=$1
  output_apk=$2
  [[ -f "$input_apk" ]] || { echo 'ERROR: input APK not found' >&2; exit 2; }
  [[ ! -e "$output_apk" ]] || { echo 'ERROR: destination already exists' >&2; exit 2; }
else
  echo 'Usage: sign_update_apk.sh --check | sign_update_apk.sh INPUT_APK OUTPUT_APK' >&2
  exit 2
fi

: "${TCL_SIGNING_STORE_PASS:?Missing signing store password}"
: "${TCL_SIGNING_KEY_PASS:?Missing signing key password}"
: "${TCL_SIGNING_KEY_ALIAS:?Missing signing alias}"
: "${TCL_EXPECTED_CERT_SHA256:?Missing installed APK signing certificate SHA-256}"

expected=${TCL_EXPECTED_CERT_SHA256,,}
[[ "$expected" =~ ^[a-f0-9]{64}$ ]] || { echo 'ERROR: expected SHA-256 must be 64 hex digits' >&2; exit 2; }

apksigner=${TCL_APKSIGNER_BIN:-${ANDROID_HOME:-}/build-tools/36.0.0/apksigner}
[[ $check_only == 1 || -x "$apksigner" ]] || { echo 'ERROR: official apksigner not found' >&2; exit 2; }
command -v keytool >/dev/null || { echo 'ERROR: keytool not found' >&2; exit 2; }

tmpdir=$(mktemp -d)
trap 'rm -rf -- "$tmpdir"' EXIT
keystore=$tmpdir/tcl-release.jks
if [[ -n ${TCL_SIGNING_KEYSTORE_PATH:-} ]]; then
  [[ -f "$TCL_SIGNING_KEYSTORE_PATH" ]] || { echo 'ERROR: keystore file not found' >&2; exit 2; }
  cp -- "$TCL_SIGNING_KEYSTORE_PATH" "$keystore"
else
  : "${TCL_SIGNING_KEYSTORE_B64:?Missing signing keystore; provide PATH or B64}"
  printf '%s' "$TCL_SIGNING_KEYSTORE_B64" | base64 --decode > "$keystore"
fi
chmod 600 "$keystore"

actual=$(keytool -exportcert \
  -alias "$TCL_SIGNING_KEY_ALIAS" \
  -keystore "$keystore" \
  -storepass:env TCL_SIGNING_STORE_PASS 2>/dev/null | sha256sum | cut -d ' ' -f 1)
if [[ "$actual" != "$expected" ]]; then
  echo "BLOCKED: signing key does not match the certificate of the installed APK. Expected=$expected Found=$actual" >&2
  exit 3
fi
if [[ "$check_only" == 1 ]]; then
  echo "OK: approved signer $actual"
  exit 0
fi

"$apksigner" sign \
  --ks "$keystore" \
  --ks-key-alias "$TCL_SIGNING_KEY_ALIAS" \
  --ks-pass env:TCL_SIGNING_STORE_PASS \
  --key-pass env:TCL_SIGNING_KEY_PASS \
  --out "$tmpdir/signed.apk" "$input_apk"

report=$("$apksigner" verify --verbose --print-certs "$tmpdir/signed.apk")
printf '%s\n' "$report" | grep -qx 'Verifies' || { echo 'ERROR: signed APK failed apksigner verification' >&2; exit 4; }
cert=$(printf '%s\n' "$report" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n 1)
[[ "$cert" == "$expected" ]] || { echo 'ERROR: signed APK has a different signing certificate' >&2; exit 4; }

mv -- "$tmpdir/signed.apk" "$output_apk"
echo "OK: output APK signer=$cert"
sha256sum "$output_apk"

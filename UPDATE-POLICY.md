# Updating XioVPN (and why updates stay compatible)

## The problem this solves

Android refuses to install an update over an existing app unless the new APK is
signed with **the same key** as the installed one. A `debug` build is signed with
a throw-away key that changes per machine/run, so every CI build produced an APK
that could only be installed by uninstalling the old app first — and the in-app
updater would fail with *"App not installed"* / an incompatible-signature error.

## What is in place now

1. **A stable release key** (`xiopvn-release.jks`, 4096-bit RSA, ~30 years) signs
   every build. It lives only in the repository's Actions secrets:
   `XIO_KEYSTORE_BASE64`, `XIO_KEYSTORE_PASSWORD`, `XIO_KEY_ALIAS`,
   `XIO_KEY_PASSWORD`. Never commit the keystore, and **back it up**: losing it
   means no future update can be installed over an existing install.
2. **CI builds `assembleRelease`** (not debug) and prints
   `apksigner verify --print-certs`, so the signing fingerprint is visible in
   every build log.
3. **`versionCode` must strictly increase** for Android to accept an install over
   the old app. Manually running the "Build XioVPN APK" workflow takes a `bump`
   input (`patch` / `minor` / `major`) that raises `versionCode` and
   `versionName` before building.

## Shipping an update

1. Actions -> **Build XioVPN APK** -> *Run workflow* -> choose `bump` -> Run.
2. Download the `xiopvn-apk` artifact, or publish the files as a Release tagged
   `v<version>` so the in-app updater can find them.
3. The updater matches assets by name (`XioVPN-<version>-<abi>.apk`) and only
   offers versions strictly newer than `BuildConfig.VERSION_NAME`.

## Rules that keep updates working

- Never change `applicationId` (`com.xiovpn.mobile`) — a new id is a different app.
- Never re-key: same keystore forever.
- Always raise `versionCode`; never reuse or decrease it.
- Keep the `XioVPN-<version>-<abi>.apk` asset naming (legacy `UAC-*` still accepted).

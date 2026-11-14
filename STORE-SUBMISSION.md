# Publishing XioVPN on Iranian Android stores (+ Google Play)

## What only you can do (accounts and identity)

No automated tool can create these — they need your ID, phone number and payment:

| Store | Where | What you need |
|---|---|---|
| **Cafe Bazaar** (کافه بازار) | pishkhan.cafebazaar.ir | Iranian developer account (national ID / company), app reviewed by Bazaar |
| **Myket** (مایکت) | myket.ir / panel | Myket developer account, **app must pass Myket review** |
| **Google Play** | play.google.com/console | $25 one-time, an identity verification, and a real privacy-policy URL |
| **Sibche / Sibapp / Charkhoneh** | their own panels | each has its own account |

Uploading is a manual, logged-in step in each store's panel, and the reviewer
talks to the **account owner**, not to a script — so I cannot publish on your
behalf. What I can do is make the artifacts and store listing ready so you only
click through the panels.

## The real blocker: this app will likely be rejected

XioVPN is an SNI-spoofing / DPI-bypass VPN, i.e. a **circumvention tool**:

- **Google Play** prohibits apps that "interfere with, disrupt or access in an
  unauthorised manner" networks/other apps, and blocks VPN apps that violate its
  policy on anonymising traffic. Circumvention tools for sanctioned regions are
  a specific enforcement area. Expect rejection.
- **Cafe Bazaar and Myket** operate inside Iran and must comply with Iranian
  regulation; SNI-spoofing/anti-filtering apps are routinely removed from them.
  Publishing here also puts **your legal identity** on the record as the
  publisher.
- Independent of policy: a circumvention app is usually distributed through
  GitHub Releases / Telegram precisely because the stores remove it.

**Be clear-eyed about this before spending time on store accounts.** The safe,
already-working distribution is the GitHub Releases link you have.

## If you still want to submit

### 1. Build a signed release bundle
Actions -> *Build XioVPN APK* -> Run workflow -> `bump: patch`. The artifact
`xiopvn-release` now contains both:
- `XioVPN-<version>-<abi>.apk` (Cafe Bazaar / Myket / direct install)
- `app-release.aab` (Google Play; Play requires an AAB)

### 2. Assets each store asks for
- **Icon 512x512** — export from `assets/icon.png` (already 1024, downscale)
- **Feature graphic / banner** 1024x500 (Play)
- **2–8 phone screenshots** — take them on a device; show Home, the Connect
  button with the rim light, the configs list, settings
- **Short description** (≤80 chars) and **full description** (≤4000)
- **Privacy policy URL** — must be publicly reachable. Put `PRIVACY.md` in a
  public repo and use its raw or GitHub Pages URL.
- **Category**: Tools (Play: Tools / VPN is not allowed for circumvention)

### 3. Store-specific settings
- Play requires **targetSdk 35** (we have it) and a **signed AAB** (we build it).
- If you use Play, keep it out of Play's VPN declaration unless you are a
  registered VPN provider — declaring VPN without eligibility causes removal.
- Cafe Bazaar/Myket accept the **APK** directly; upload the `arm64-v8a` one as
  the primary and `armeabi-v7a` as a secondary build.

### 4. Listing text (draft, Persian)
**Short:** تونل امن شبکه با موتور Xray و PoW، کانفیگ خودکار و مسیر هوشمند.
**Full:** XioVPN یک کلاینت شبکه دوزبانه است: موتور Xray با کانفیگ خودکار
یککلیکی، عبور مستقیم برنامهها، آزمایشگاه SNI، نمودارهای زنده ترافیک و ایمنی
محلی. هیچ حسابی لازم نیست و هیچ دادهای جمعآوری نمیشود.

### 5. Version discipline
Same rules as updates: never change `applicationId`, always raise `versionCode`,
always sign with `xiopvn-release.jks`. Stores reject an upload whose versionCode
is not higher than the published one.

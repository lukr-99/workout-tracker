# Phase 6 — Play Store submission checklist

> **SUPERSEDED / not pursued.** The app is **personal-use only** — no Play Store release. The real
> final step is [final-phase-cutover.md](final-phase-cutover.md) (remove the C# app, promote to
> `main` + `release/2.0`). This checklist is kept only as reference if that decision ever changes.


The rework is **functionally complete and release-hardened** on `feature/app-rework`: native
Kotlin/Compose app, version **2.0.0**, minified R8 release build, secret-free signing config, CI,
Health Connect + auto-backup, import/export, analytics/records/recovery/progression, exercise images.
What's left to actually ship on Google Play is mostly **your** account/legal/asset work, plus some
prep an agent can draft. This doc splits the two.

## A. User-only actions (cannot/should not be automated)

1. **Upload keystore** — run the `keytool` command in [release-signing.md](release-signing.md),
   store the keystore + passwords in your password manager, and fill `keystore.properties` (or the
   `KEYSTORE_*` env vars). **Never commit it.** (The build reads it automatically.)
2. **Signed release artifact** — Play wants an **AAB**: `./gradlew bundleRelease` (verify the bundle
   task/signing wiring — see §C.1). Confirm the signed AAB installs on the A56 before uploading.
3. **Google Play Console** — create the app entry (one-time developer registration fee if it's a new
   account).
4. **Privacy policy at a public HTTPS URL** — reuse the in-app policy text (the Health Connect
   rationale screen). An agent can produce the page (§C.2); **you host it** (GitHub Pages, etc.) and
   paste the URL into Play Console. Required because the app uses Health Connect.
5. **Play Console declarations** — content rating (IARC questionnaire), **Data safety**, **Health
   apps declaration** (Health Connect access + purpose), target audience, ads (none), app access.
   Agent can draft the answers (§C.4); you submit them.
6. **Health Connect end-to-end** — on-device, grant the four health permissions and run a real
   import + export with your data (automated tests deliberately don't grant consent).
7. **Backup end-to-end** — pick a backup folder in Settings and confirm a real scheduled WorkManager
   backup file appears (inexact timing — may take a while).
8. **Ship decision / branch strategy** — decide when to merge `feature/app-rework` → `main` and tag
   (e.g. `v2.0.0`). Note `main`/`release/1.0` still hold the frozen MAUI POC; the rework has lived on
   `feature/app-rework` throughout. See §C.5 for the recommended flow.

## B. Dogfood first (recommended before submitting)

The app is only as convincing as its data. Import your real history and use it for a couple of weeks:
- Push the captured Lyfta CSV to the phone and import it via **Settings → Data → Import**
  (`import/lyfta/lyfta-export.csv`, 468 sets / 19 sessions). *(Ask and I'll push it to the phone.)*
- Then Progress/Records/Muscle-Recovery/charts render against real training — which is also what you
  want for the store screenshots (§C.3).

## C. Agent-doable prep (I can produce these on request)

1. **Verify/add `bundleRelease` (AAB)** + a short `docs/rework/release-process.md` (version bump →
   bundle → sign → upload), and a `versionCode` bump convention for updates.
2. **Privacy-policy page** — a self-contained HTML page derived from the in-app policy, ready to host.
3. **Store screenshots + feature graphic** — after dogfood import, capture a clean phone screenshot
   set via `tools/phone.ps1` (Home, live logging, Progress, Records, Muscle-Recovery, Settings), and
   draft the 1024×500 feature graphic + icon export.
4. **Listing copy + form answers** — app title, short (80char) + full description, and draft
   Data-safety / Health-declaration / content-rating answers (local-only storage, no data sharing,
   Health Connect read/write purpose).
5. **Branch/release flow** — a concrete plan to bring `feature/app-rework` to `main` and tag `v2.0.0`
   without disturbing the `release/1.0` MAUI POC (which stays as history).

## Status of the code (nothing blocking on the engineering side)

Verified on the merged tree (A56): `testDebugUnitTest`, `assembleDebug`, `assembleRelease` (R8 +
resource shrink), and `connectedDebugAndroidTest` all green. No known crashes. Schema at **v3** with
non-destructive migrations `1→2→3` and checked-in baselines. The remaining items above are release
logistics, not development.

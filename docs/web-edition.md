# Web edition

Status: M1 built (the `:web` module, browser storage, the lifting core, the page shell and the
deploy workflow). Not deployed yet. Runs are left out of v1 (owner's decision, 2026-10-25). The
decision and the alternatives are in [ADR 0001](adr/0001-web-edition.md).

The web edition lets friends on iPhone (and anyone with a current browser) use Ember from Safari and
add it to the home screen like an app. It is the same Kotlin code as the Android app, compiled to
WebAssembly with Compose Multiplatform and drawn on a canvas, modelled on Tarot2Go's web edition
(`../Tarot2Go/docs/web-edition.md`).

## Browsers

Same floor as Tarot2Go: WasmGC and the legacy exception-handling proposal.

| Browser | Minimum |
| --- | --- |
| Safari on iPhone and iPad | iOS / iPadOS 18.2 |
| Safari on a Mac | 18.2 |
| Chrome, Edge | 119 |
| Firefox | 120 |

On iOS every browser uses Safari's engine. The page checks both features before it loads the app and
shows "This browser is too old for Ember" with update steps when they are missing.

## What ships on the web

| Area | On the web | How |
| --- | --- | --- |
| Live workout, sets, set tags, supersets, rest timer | M1 | Shared UI and `LiveWorkoutViewModel`. The rest timer runs while the page is open. |
| Templates | M1 | Shared. |
| History and workout detail | M1 | Shared. |
| Exercise library, custom exercises, exercise notes | M1 | Shared. The starter catalog is seeded on first start, as on Android. |
| Settings (theme, units, default rest, reduce motion) | M1 | Stored in the browser document, same keys as the backup. Android-only rows (Health Connect, automatic backup, updates, catalog sync) are not shown. |
| Backup export (JSON) and CSV | M1 | The same `ExportBundle` JSON. On iPhone the share sheet ("Save to Files") when the browser can share files, else a download. |
| Backup import (Ember JSON, Lyfta CSV) | M1 | A file picker; the same preview, merge and replace flow. |
| Delete all data | M1 | Clears the browser document and reseeds the catalog. |
| Progress charts, records, e1RM, weekly volume, body heatmap | M1 (compiles and runs), checked in M2 | Shared. All charts are our own `Canvas` code. |
| Gyms and per-gym tracking, gym notes | M1 without GPS, polished in M2 | Gyms are picked by hand. No "you are at this gym" suggestion; "Set position" reports that the position is not available. |
| Share a workout | M1, text only | The share sheet with the workout as text, else the clipboard. No share card image yet. |
| Spotify button | M1 | A link, not a controller: opens `spotify:` (the app, if installed), then `https://open.spotify.com`. |
| Exercise photos and catalog pictures | M3 | Not in M1: the editor says photos are not available yet, and no pictures are drawn. Catalog pictures come from another site, which the CSP does not allow yet. |
| Runs | Not in v1 | Owner's decision, see "Run tracking". The Runs tab says runs are not in the web edition yet. |
| Health Connect | No | Android only. |
| Scheduled automatic backups | No | A browser cannot write files on a schedule. A backup reminder is planned for M3. |
| In-app updater | No | The service worker delivers new builds. |
| wger catalog sync | No | Not needed for v1. |
| Vibration and speech cues | No | No Vibration API on iPhone. The rest timer end shows on screen only. |

### Run tracking (decided: option 1)

Safari on iPhone does not give a web page location while the screen is off or the app is in the
background. A run recorded on the web only works with the screen on the whole time. The choices were:

1. **Leave runs out of v1 (chosen on 2026-10-25).** v1 is lifting only. Runs imported from an Android
   backup are kept in storage untouched, so a backup made on the web does not lose them, but they are
   not shown. Runs can come later as their own milestone.
2. **Runs read-only.** Run history, detail, splits and maps (MapLibre GL JS) for runs imported from
   Android. No recording.
3. **Screen-on recording.** Option 2 plus live recording with `watchPosition`, a screen wake lock and
   a clear warning that the screen must stay on.

## How it is put together

- `web/` is the `:web` Gradle module (Kotlin Multiplatform, `wasmJs` target, Compose Multiplatform
  1.7.3). Its `wasmJsMain` compiles `domain/`, `settings/`, `data/` and `ui/` straight from
  `app/src/main/java/com/lukr99/workout`, minus the list `androidOnly` in `web/build.gradle.kts`
  (Room, Run Mode, Health Connect, backup scheduling, the updater, GPS, and Android's side of every
  platform file).
- `web/src/wasmJsMain/kotlin/com/lukr99/workout/` holds the web's versions of the platform files
  (same package, name and signature as Android's; see ARCHITECTURE.md, "Shared code and platform
  files"), the web `data/AppContainer.kt`, and `web/`: `Main.kt`, the storage banner, the browser
  storage (`web/storage/`) and the JavaScript glue (`web/platform/`).
- The storage logic itself is shared code in `app/src/main/java/com/lukr99/workout/data/document/`,
  so the JVM tests in `:app` hold it to the same contract as the Room repository.
- `web/src/wasmJsMain/resources/` holds the page: `index.html`, `boot.css`, `boot.js` (browser check,
  loading screen, "too old" and "could not start" screens, update banner), `sw.js` (offline copy),
  `manifest.webmanifest`, `apple-touch-icon.png`, `icons/`, `_headers` (CSP and security headers) and
  `404.html`. The icons come from the Android launcher icon; regenerate them with
  `NODE_PATH="$(npm root -g)" node tools/render-web-icons.cjs` after changing it.
- The Barlow fonts are served next to the page from `app/src/main/res/font`; `Main` fetches them
  before the first frame, because a canvas has no system fonts to fall back on.
- `web/wrangler.toml`: the Cloudflare Pages project name (`ember-workout`) and output folder.
- `tools/stamp_web_dist.py` prepares a build for deploy; `tools/check-web-dist.cjs` checks a stamped
  build in headless Chromium; `.github/workflows/web.yml` builds, tests, checks and (only by hand)
  deploys.

Notes for the UI:

- The canvas fills the whole window, including the area under the iPhone home indicator.
  `globalThis.emberSafeArea()` returns the insets in CSS pixels and an `ember:safearea` event fires on
  resize.
- The CSP allows only same-origin files. Do not load fonts, images or scripts from another site.

## Storage

- **IndexedDB**, database `ember`:
  - `data`: one record, the whole workout data as one document in the export-bundle shape
    (exercises, templates, workouts, locations, notes, settings, and any runs and routes from an
    imported Android backup), with a revision number.
  - `quarantine`: stored data the app could not read, set aside once and never overwritten. The
    storage banner offers it as a download.
- In memory, the repositories hold the document as Kotlin models and expose `Flow`s like the Room DAOs
  do. A write changes the model and saves the whole document.
- **No lost updates across tabs.** Each save runs in one `readwrite` transaction that checks the stored
  revision against the one this tab loaded. If another tab saved in between, this tab takes the newer
  document, applies its own change on top and saves again. A `BroadcastChannel` tells other tabs to
  reload after a save, and a tab reloads when it becomes visible.
- **No silent failures.** A failed save (quota, storage blocked) shows a lasting banner with "Try
  again", and the change stays in memory. If the browser refuses storage altogether, the banner says
  that everything is lost when the page closes. The app never falls back silently.
- The app asks the browser to keep its storage (`navigator.storage.persist()`).
- Photos are not stored in M1. An Android backup's photos are dropped on import.
- Kotlin/Wasm 2.0.21 writes the number `1e-7` as `.e-7`, which is not valid JSON. The document codec
  and `JsonExporter.toJson` repair it (`data/document/JsonNumberRepair.kt`), so web backups always
  read back.

## Run it locally

Development build (no Binaryen download needed):

```bash
./gradlew :web:wasmJsBrowserDevelopmentRun        # serves on http://localhost:8080 and reloads
# or build the files only:
./gradlew :web:wasmJsBrowserDevelopmentExecutableDistribution
# output: web/build/dist/wasmJs/developmentExecutable
```

Linking needs more memory than Gradle's default; `gradle.properties` sets 3 GB for Gradle and 4 GB
for the Kotlin daemon. On a machine that cannot reach GitHub, add `-Pember.web.systemYarn=true` to use
a Yarn already on `PATH`.

The service worker caches nothing until the output is stamped, so a plain local build always shows
the latest code. To try the offline copy and the real headers locally:

```bash
python tools/stamp_web_dist.py --dev web/build/dist/wasmJs/developmentExecutable
NODE_PATH="$(npm root -g)" node tools/check-web-dist.cjs web/build/dist/wasmJs/developmentExecutable --dev
```

`--dev` on the stamp script skips the 25 MiB per-file limit (the unoptimized development `.wasm` is
larger); `--dev` on the check allows `eval`, which only the development bundle uses.

## Build and test

```bash
./gradlew :web:wasmJsBrowserDistribution          # production output:
                                                  # web/build/dist/wasmJs/productionExecutable
./gradlew :web:wasmJsNodeTest                     # web tests (run under Node.js)
./gradlew testDebugUnitTest                       # includes the shared storage contract tests
python tools/test_stamp_web_dist.py
python tools/stamp_web_dist.py web/build/dist/wasmJs/productionExecutable
```

CI (`Web edition` workflow) does all of this on every push or pull request that touches `web/`, the
shared app sources or the Gradle setup, then checks the build in Chromium and uploads it as the
`ember-web` artifact.

## Hosting and deploy

Cloudflare Pages, as a **Direct Upload** project: GitHub Actions builds, checks and uploads; Cloudflare
only serves. Free plan; no running costs.

One-time setup, by the owner (done on 2026-10-26):

1. Create the Pages project once, from a computer with a logged-in Wrangler:
   `npx wrangler pages project create ember-workout --production-branch=main --force`. The production branch
   must be `main`. The names `ember` and `ember-lift` were taken on pages.dev and would have got a
   random suffix, so the project is `ember-workout`. `--force` is needed with newer Wrangler, which otherwise hands Pages commands for a
   project that does not exist yet to Cloudflare Workers, and they fail.
2. In the GitHub repository: Settings > Environments > New environment `web-production`, with the
   secrets `CLOUDFLARE_API_TOKEN` (Account > Cloudflare Pages > Edit only; the token made for Tarot2Go
   works) and `CLOUDFLARE_ACCOUNT_ID`. Under "Deployment branches and tags" allow only `main`.

Each deploy: Actions > Web edition > Run workflow on `main`, tick **Deploy this build to Cloudflare
Pages**, and choose `preview` or `production`. The workflow never deploys on push or pull request, and
refuses to run without both secrets. Production lives at `https://ember-workout.pages.dev`.

## Install on iPhone

1. Open the link in Safari (iOS 18.2 or newer).
2. Wait for Ember to appear, then tap Share > Add to Home Screen > Add.
3. Use Ember from the home screen from then on. It runs full screen and works offline.

The home-screen app keeps its own storage, separate from Safari tabs, and Safari may delete the data
of a site that is not on the home screen and was not opened for 7 days. Install first, and move data
between the two with export and import.

## Updates

- Each deploy gets a new build id in `sw.js`. A new build downloads in the background, every file is
  checked against its SHA-256, and only a complete set replaces the old one.
- The new build waits beside the old one (no automatic `skipWaiting`), so a running app never meets
  files of a newer build. On the loading screen it takes over at once; in a running app a "A new
  version is ready. Reload" banner appears, and Reload hands over and reloads.

## Data stays in the browser

- No account and no server. Nothing is sent anywhere.
- Clearing Safari's website data or deleting the home-screen app deletes the data. Export a backup
  from Settings now and then. A backup made on the web imports on Android and the other way round.

## Milestones

- **M0, Android prep.** Done (PR #47): kotlinx-datetime, repository and settings interfaces, platform
  files.
- **M1, web shell and lifting core.** This milestone: the `:web` module, IndexedDB storage with
  revisions, quarantine and the storage banner, the seeded catalog, the shared screens, backup export
  and import, the page shell, service worker, CSP, stamp and check scripts, and `web.yml` with a
  manual-only deploy.
- **M2, the rest of v1.** Progress, gyms and gym notes checked on the web; hide the Runs tab and the
  GPS rows instead of showing "not available"; a gentle note in a Safari tab; iPhone safe areas; a
  real-iPhone test pass; the first deploy with the owner.
- **M3, v1.1.** Exercise photos with capture and compression, catalog pictures, the workout share card,
  a backup reminder in place of scheduled backups.
- **M4, runs (if chosen later).**

## Left out compared with Android

- No in-app updater, no Room, no Health Connect, no automatic backups, no Run Mode, no GPS.
- Drawn on a canvas: screen readers, browser text selection, browser zoom and find-in-page work poorly
  or not at all.
- Needs iOS 18.2 or a similarly recent browser, and a larger first download than a typical website.

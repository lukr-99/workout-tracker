# Web edition (draft)

Status: plan, nothing built yet. The decision and the alternatives are in
[ADR 0001](adr/0001-web-edition.md).

The web edition lets friends on iPhone (and anyone with a current browser) use Ember from Safari and
add it to the home screen like an app. It is the same Kotlin code as the Android app, compiled to
WebAssembly with Compose Multiplatform, modelled on Tarot2Go's web edition
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

| Area | v1 on the web | How |
| --- | --- | --- |
| Live workout, sets, set tags, supersets, rest timer | Yes | Shared UI and `LiveWorkoutViewModel`. The rest timer runs while the page is open. |
| Templates | Yes | Shared. |
| History and workout detail | Yes | Shared. |
| Exercise library, custom exercises, exercise notes | Yes | Shared. The starter catalog is seeded on first start, as on Android. |
| Progress charts, records, e1RM, weekly volume, body heatmap | Yes | Shared. All charts are our own `Canvas` code. |
| Gyms and per-gym tracking, gym notes | Yes, without GPS | Gyms are picked by hand. No "you are at this gym" suggestion. |
| Settings (theme, units, default rest) | Yes | A web `SettingsStore` in the same storage, same keys as the backup. |
| Backup export (JSON) and CSV | Yes | The same `ExportBundle` JSON, offered as a download. On iPhone the Web Share sheet ("Save to Files") is used when available. |
| Backup import (Ember JSON, Lyfta CSV) | Yes | `<input type=file>`; the same preview, merge and replace flow. |
| Delete all data | Yes | Clears the web storage and reseeds the catalog. |
| Exercise photos | v1.1 (milestone 3) | `<input type=file accept="image/*" capture>`, scaled in a canvas to at most 1600 px and stored as JPEG blobs. |
| Share a workout (text or card) | v1.1 | Web Share API with text; the share card image if the canvas render ports cleanly, else text only. |
| Spotify button | v1.1 | A link, not a controller: opens `spotify:` (the app, if installed) with `https://open.spotify.com` as the fallback. No playback control or now-playing. |
| Runs: live recording | Not in v1 | Owner's decision, see "Open question". |
| Runs: history, detail, routes | Not in v1 | Comes with the run decision. Maps would use MapLibre GL JS. |
| Health Connect | No | Android only. |
| Scheduled automatic backups | No | A browser cannot write files on a schedule. The app reminds the user to export instead (milestone 3). |
| In-app updater | No | The service worker delivers new builds. |
| wger catalog sync | No | Cross-origin calls are blocked by the CSP and not needed for v1. |
| Vibration and speech cues | No | No Vibration API on iPhone. Rest timer end shows on screen only. |

### Open question: run tracking

Safari on iPhone does not give a web page location while the screen is off or the app is in the
background. A run recorded on the web only works with the screen on the whole time. The choices:

1. **Leave runs out of v1 (recommended).** v1 is lifting only. Runs imported from an Android backup
   are kept in storage untouched, so a backup made on the web does not lose them, but they are not
   shown. Runs can come later as their own milestone.
2. **Runs read-only.** Show run history, detail, splits and maps (MapLibre GL JS with OpenFreeMap
   tiles) for runs imported from Android. No recording. Adds the map library and a CSP exception for
   the tile host.
3. **Screen-on recording.** Option 2 plus live recording with `watchPosition`, a screen wake lock and
   a clear warning that the screen must stay on and Ember must stay in front. The trace has gaps the
   moment the phone locks. Needs testing on a real iPhone, including the wake lock in the home-screen
   app.

## Storage

- **IndexedDB**, database `ember`, three object stores:
  - `data`: one record, the workout data as an `ExportBundle` without photos, plus a `revision`
    number and the format version. This keeps the browser copy in the format that
    [data-contract.md](data-contract.md) already describes and tests.
  - `photos`: one compressed JPEG blob per exercise photo, keyed by exercise id.
  - `quarantine`: data the app could not read, set aside once and never overwritten. Settings offers
    it as a download.
- In memory, the web repository holds the document as Kotlin models and exposes `Flow`s like the
  Room DAOs do. A write changes the model and saves the whole document.
- **No lost updates across tabs** (a Tarot2Go review finding). Each save runs in one `readwrite`
  transaction that checks the stored `revision` against the one this tab loaded. If another tab
  saved in between, the tab reloads the newer data, reapplies its change and saves again. A
  `BroadcastChannel` tells other tabs to reload after a save.
- **No silent failures** (also from that review). A failed save (quota, storage blocked) shows a
  lasting banner with "Export a backup". If the browser refuses storage altogether, the app says on
  every screen that data lives in memory only until the page closes. It never falls back silently.
- The app calls `navigator.storage.persist()` and shows a gentle note in a Safari tab, because Safari
  may delete data of a site that is not on the home screen and was not opened for 7 days.
- Runs and run routes from an imported Android backup stay in the document untouched (option 1), so
  web, then Android round trips lose nothing.

## Module layout

```text
app/        Android app, unchanged build. Shared sources stay where they are.
web/        :web, Kotlin Multiplatform, wasmJs target, Compose Multiplatform 1.7.3
  build.gradle.kts            srcDir into app/src/main/java/com/lukr99/workout/{domain,ui,...}
  src/wasmJsMain/kotlin/      web adapters: storage, settings, files, share, photos, fonts, ids
  src/wasmJsMain/resources/   index.html, boot.js, boot.css, sw.js, manifest, icons, _headers, 404
  src/wasmJsTest/             web tests (Node), storage and adapters
  wrangler.toml               Pages project name (proposed: ember)
tools/stamp_web_dist.py       build id and file hashes into sw.js, Pages limits check
tools/check-web-dist.cjs      headless Chromium check of a stamped build
.github/workflows/web.yml     build, test, check; deploy by hand only
```

What `:web` compiles from `app/src/main`:

- `commonMain`: `domain/` (all of it once portable), the export and import model and planner
  (`data/export`, the Room-free parts of `data/importer` and `data/transfer`), the repository
  interfaces, and `settings/` interfaces.
- `wasmJsMain`: `ui/` without `ui/run/`, `UpdatesViewModel` and the updater screens, plus the web's
  same-name files for each platform call. As in Tarot2Go, `ui/` is platform code there, because it
  uses those same-name declarations.
- Excluded: Room, WorkManager, Health Connect, location, map, routing, music, updater, wger sync.

The Barlow fonts are served next to the page from `app/src/main/res/font`, as Tarot2Go does with its
typefaces. The CSP allows only same-origin files.

Later, once the web edition has proven itself, the shared code moves into a `:shared` module.

## What changes on the Android side first

Each step is its own pull request or commit series, with the Android app unchanged for the owner and
`testDebugUnitTest assembleDebug lintDebug` green:

1. **Dates and times.** `java.time` becomes `kotlinx-datetime` in `domain/` (about 20 files) and in
   the shared `ui/` code (about 13 files, mostly `ui/components/Format.kt`). Date formatting goes
   behind one `formatDate` and friends with an Android file and a web file. Tests pin week starts,
   time zones and export timestamps.
2. **Ids and numbers.** `UUID.randomUUID()` in `domain/Models.kt` becomes a platform `newId()`.
   `"%.1f".format` and `String.format(Locale...)` become one shared number formatter.
   `java.net.URI` in `GuideLink` becomes plain string checks. GPX stays Android-only for now.
3. **Repository interfaces.** `WorkoutRepository` and `RunRepository` become interfaces; the Room
   classes become `RoomWorkoutRepository` and `RoomRunRepository`. `WorkoutDataService`,
   `WorkoutInsightsService` and `DataTransferService` depend on the interfaces and on a small
   `TransactionRunner` (already an interface).
4. **ViewModels.** Factories use the `viewModel { }` initializer instead of
   `ViewModelProvider.Factory`. `AppContainer` gets a small interface (or the factories take their
   dependencies directly), so the web can supply its own graph.
5. **Platform calls out of shared screens**, each into a same-name file:
   photo picker and camera (`ExerciseEditorScreen`), file pickers and share intents
   (`DataTransferScreen`, `DataTransferViewModel`), workout share (`WorkoutDetailScreen`),
   `BackHandler` (`App.kt`), animator scale (`components/Motion.kt`), the exercise thumbnail (Coil 2
   with `File`), window insets in `ExercisePickerScreen`, the music button, and the Health Connect and
   location rows in Settings. `SettingsStore` gets an interface over DataStore.
6. **Fonts.** `ui/theme/Type.kt` reads the Barlow fonts through a same-name file instead of
   `R.font`.

## Milestones

- **M0, Android prep.** Steps 1 to 6 above. No web code yet. Ships as a normal Android release, so the
  owner's phone proves nothing changed.
- **M1, web shell and lifting core.** `:web` module, boot page with the browser check, IndexedDB
  storage with revisions and quarantine, the web `SettingsStore`, seeded catalog. Screens: Home,
  live workout, templates, history, workout detail, library, settings, backup export and import,
  delete all data. Service worker with verified per-build caches and the waiting-build handover (no
  automatic `skipWaiting`), manifest, iOS meta tags, `_headers` with CSP, `stamp_web_dist.py`,
  `check-web-dist.cjs`, and `web.yml` without a deploy run. Web tests for storage (two tabs, quota,
  unreadable data, round trip with an Android backup).
- **M2, the rest of v1.** Progress screens and charts, gyms and per-gym tracking, gym notes, the "too
  old" and "could not start" screens polished, iPhone safe areas, a real-iPhone test pass.
- **M3, v1.1.** Exercise photos with capture and compression, Web Share for workouts, the Spotify link,
  a backup reminder in place of scheduled backups.
- **M4, runs (if chosen).** Read-only run history and maps, then screen-on recording, per the owner's
  answer above.
- **Deploy.** The Cloudflare Pages project and the first deploy are done with the owner, after M1 or
  M2. New projects must be created with
  `wrangler pages project create ember --production-branch=main --force`.

## Hosting and deploy

As Tarot2Go: Cloudflare Pages Direct Upload, built and checked in GitHub Actions, deployed only by
hand (`workflow_dispatch` with "deploy" ticked), production only from `main`, through the GitHub
environment `web-production` with `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID`, and
`cloudflare/wrangler-action` pinned by commit SHA. The Cloudflare account and a Pages-Edit token
already exist for Tarot2Go. Free plan; no running costs.

## Install on iPhone

1. Open the link in Safari (iOS 18.2 or newer).
2. Share > Add to Home Screen > Add.
3. Use Ember from the home screen from then on. It runs full screen and works offline.

The home-screen app keeps its own storage, separate from Safari tabs.

# ADR 0001: The web edition is Ember's own Kotlin, compiled to WebAssembly

Status: Proposed (2026-10-25). Waiting for the owner's answer on run tracking.

## Context

Friends on iPhones want to use Ember. Ember is an Android app, sideloaded, with no backend. A native
iOS app would need a Mac, an Apple developer account and TestFlight or App Store review for every
friend, which is out of proportion for a personal, noncommercial app.

The sibling app Tarot2Go solved the same problem on 2026-10-25 with a browser edition: the app's own
Kotlin and Compose UI compiled to WebAssembly with Compose Multiplatform, hosted as a static site on
Cloudflare Pages that installs to the iPhone home screen (Tarot2Go `docs/adr/0004-web-edition.md`).

Ember is larger and leans on the platform more than Tarot2Go:

- `domain/` (138 files) is free of Android imports, but about 20 of its files use `java.time`, and
  some use `java.util.UUID`, `String.format`, `java.net.URI` and the JVM XML parser (GPX). None of
  these exist on Kotlin/Wasm.
- ViewModels take the concrete, Room-backed `WorkoutRepository`, and 13 `ui/` files use `java.time`
  for formatting.
- A few screens call Android directly: activity result launchers (photos, file pickers, location
  permission), share intents, `Toast`, `LocalContext`, Coil 2 with `java.io.File`, Health Connect,
  MapLibre Android and two `AndroidViewModel`s in Run Mode.
- Settings use AndroidX DataStore with a `Context`.

Good news for the port: export and import already use `kotlinx.serialization`, all charts are our own
Compose `Canvas` code, nearly all copy is Kotlin strings (no `strings.xml`), and navigation is our own
`Navigator`.

## Decision

1. **Same approach as Tarot2Go.** A new `:web` Gradle module uses Kotlin Multiplatform with the
   `wasmJs` target and Compose Multiplatform 1.7.3 (matching Kotlin 2.0.21). It compiles shared code
   straight from `app/src/main` with `srcDir`, and adds web adapters under `web/src/wasmJsMain`. Where
   a screen needs a platform API, the call moves into a small file that each platform supplies under
   the same name and package (Android's stays in `app/`, the web's lives in `web/`).
2. **Make the shared code portable inside `:app` first.** Before `:web` exists, the Android app moves
   to portable APIs, verified by the existing JVM tests and lint:
   - `java.time` becomes `kotlinx-datetime` in `domain/` and in the shared `ui/` code.
   - `UUID` becomes a small `newId()` that each platform supplies; number formatting with
     `String.format` becomes one shared formatter in `domain/`.
   - `WorkoutRepository` and `RunRepository` become interfaces with Room classes behind them, so
     ViewModels no longer depend on Room.
   - ViewModel factories stop using `ViewModelProvider.Factory` directly and use the lifecycle
     `viewModel { }` initializer, which Compose Multiplatform also has.
   The Android app must behave exactly as before after each step. The export format does not change.
3. **Storage is IndexedDB, in the export format.** Workout data is one document in the shape of the
   `ExportBundle` (without photos), with a revision number. Photos are separate IndexedDB records
   (compressed JPEG blobs). localStorage is not enough: Safari limits it to about 5 MB, and one
   exercise photo can be several hundred kB.
4. **v1 scope: lifting only.** Lifting workouts, templates, history, the exercise library, progress
   charts, gyms without GPS, settings, and backup import and export. Runs, Health Connect, scheduled
   backups, the in-app updater and wger sync are left out of v1. Run tracking is the owner's decision
   (see web-edition.md, "Open question").
5. **Hosting like Tarot2Go.** A Cloudflare Pages Direct Upload project, built and checked by a GitHub
   Actions workflow, deployed only by hand from `main` through the `web-production` environment.
6. **Later, a real shared module.** Once the web edition has proven itself, the shared sources move
   into a `:shared` module that `:app` and `:web` both depend on. `srcDir` is the first step because
   it leaves the Android build untouched.

## Alternatives considered

- **A separate TypeScript web app.** Smaller and DOM-based, but it would duplicate the domain (records,
  progression, recovery, stats), the live-workout edits and the export format, and every change would
  have to be made twice.
- **A `:shared` KMP module from day one.** Cleaner, but it moves hundreds of files and changes the
  Android build at the same time as the port. Too much risk in one step for the shipped app.
- **Shims for `java.time` on the web** (same-name classes that mimic it). Less churn now, but they
  would be our own half copy of `java.time`, easy to get subtly wrong around time zones and weeks.
  `kotlinx-datetime` is the maintained answer and works on Android API 26 without desugaring.
- **Room on the web.** Room's multiplatform support does not cover the browser for our versions, and
  SQLite in WebAssembly with OPFS adds a second large download and its own failure modes on iOS.
- **A native iOS app.** Best experience, but the cost per friend is too high (see Context).

## Consequences

- Browser floor as for Tarot2Go: WasmGC and the legacy exception-handling proposal, so iOS 18.2,
  Chrome 119, Firefox 120. The page says so on older browsers instead of failing silently.
- A first download of roughly 15 to 25 MB; a service worker keeps it offline afterwards.
- Canvas rendering: screen readers, text selection, browser zoom and find-in-page are weaker.
- Data lives in the browser on one device. A backup moves it between the web and Android in both
  directions. Safari may delete data of a site that is not on the home screen.
- The `java.time` migration touches about 35 files on Android. It is mechanical but must keep week
  boundaries, time zones and the export timestamps identical; tests guard this.
- Shared code may no longer use JVM or Android APIs. `:web` enforces this by failing to compile, and
  CI builds it on every change to `app/src/main`.

Details, mapping and milestones: [docs/web-edition.md](../web-edition.md).

# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by Keep a Changelog, and this project currently uses simple semantic app versions for local releases.

## [Unreleased]

### Added

- **Repeat a workout.** An empty workout lists your latest workouts (one per name) with a Copy
  button. Copy loads that workout's exercises, supersets and sets, with last time's reps and
  weight ready to tick. It takes that name unless you already renamed the workout.
- **Switch to a template** from an empty workout. The workout takes the template's name, plan and
  link, so finishing can still offer to update the template.
- **Duplicate a set** from set options. The copy goes right after it with the same reps, weight
  and tags, ready to tick.

### Changed

- **Exercises further down a workout are folded to one row** with their body part and how many
  sets are planned, so the exercise you are on stays in view. Tap a row to open it. The exercise
  with the next set, and its superset partners, stay open.
- **Effort is one tap.** Set options show "reps left in the tank" as chips from 0 to 4+, with the
  matching RPE underneath. Tap the chosen chip again to clear it. 0 now works, which the old
  number pad treated as "clear".
- **The template editor is quicker to arrange.** Hold the dots on a card to drag it to a new place.
  A superset has an Ungroup button on its header, and reps have - and + that move the whole range
  (6-8 becomes 7-9). Tap the reps to set the range exactly.
- **The exercise menu** in a live workout links to the exercise's full history, and offers
  "Superset with next" with the name of the exercise it pairs with. An exercise already in a
  superset offers "Leave the superset" instead.
- **The template preview says how long the plan takes** ("about 55 min"), counted from its sets,
  reps and rest. A superset's exercises share one step, labelled 4a and 4b.

### Fixed

- An exercise's progress page opened before visiting the Progress tab showed "No sets logged yet".

## [2.6.0] - 2026-10-05

### Added

- **What's new after an update.** Home shows a short card about the release once, until you tap
  Got it. A fresh install shows nothing.
- **Clearer empty screens.** Home, History, Runs, Progress and the Library say what is missing and
  how to fill it, with a button where one helps (New template, Create exercise).
- A hint under a live workout says that tapping a set's number opens its tags, effort, note and
  Remove.
- **Notes you can see while you lift.** Each exercise card in a live workout now shows three
  kinds of notes, and only the ones that exist:
  - the exercise's own note from the Library (seat height, grip), pinned;
  - what you wrote on that exercise last time, with its date;
  - today's note.
- **Notes everywhere in a workout:**
  - a note for the whole workout;
  - a note per exercise, from the card's "Add note" button or its menu;
  - a note per set, from set options.
  - Past workouts can be edited the same way.
- **How-to steps and a guide link for each exercise.** The exercise editor takes steps (one per
  line) and a video or guide link. While logging, the info button on a card shows the steps as a
  numbered list, your note, and an "Open guide" button.
- **Body part picker in the exercise editor.** The editor now uses chips for the main muscle and
  for the muscles it also works, instead of free text. Typing your own body part still works.
- The exercise note shows in the Library catalog and in the exercise picker.
- Templates now have their own note and a note per exercise.
- **Backups hold everything.** Save JSON and automatic backup now include your settings and your
  exercise photos (scaled to 1600 px), and say which app version made them. Export format 1.8.
- **Replace everything** when restoring an Ember backup: after a confirmation that names what will
  be deleted, the phone's data is replaced by the backup exactly, settings and photos included.
  Merge stays the default.
- **Delete all data** in a danger zone at the end of the Data screen. You type "delete" to confirm.
  It also turns off automatic backup, so empty backups cannot push out your good ones, and leaves
  your backup files alone. Not available while a workout or run is live.

### Changed

- **Faster set logging.** Each set row shows what you did last time in a Previous column, and
  the set you are on has an orange outline. Tapping weight or reps opens a new number pad with
  both values side by side, quick steps (2.5 kg or 5 lb, 1 rep), a button that copies the set
  before, and Next and Done set. Done set saves the set and starts the rest timer.
- The rest timer floats at the bottom and says which set is next.
- **Templates learn from your workouts.** When you finish a workout that started from a template
  and did it differently (added or skipped an exercise, more or fewer sets), Ember lists the
  changes and asks whether to update the template, keep it as it is, or save the changes as a new
  template. The finish sheet also shows the time, weight lifted, sets and PRs.
- A template with a plan starts each exercise with its planned number of sets and its supersets.
- **A template preview.** Tapping a template on Home or in the Library shows its note, the plan
  per exercise with your best set last time, and when you last did it, with Edit and Start
  workout. The start button on Home still starts right away.
- **A rebuilt template editor.** Each exercise has steppers for sets and rest, a rep range, a note,
  and a menu to move it, remove it, or join it to the exercise above as a superset. Add exercises
  uses the same picker as the live workout, several at a time.
- **Add exercises on the go.** The picker lists your recent exercises first with what you did last
  time, lets you tick several and add them at once, and can add them as a superset.
- **Create an exercise without leaving the workout.** Type a name the library does not have and
  pick Create: a short form asks for the name, kind, main muscle, what else it works and the
  equipment. Steps, photo and guide link can be added later from the Library.
- An empty workout shows a big Add exercises button and your recent exercises as chips that add
  with one tap.
- **An exercise menu with history.** The three dots on an exercise now open a sheet with the last
  three times you did it (sets and best estimated 1RM), then Replace exercise, Superset with
  previous, Move up or down, a note for today, How to do it, and Remove.
- **Replace an exercise mid-workout.** When the machine is taken, swap the exercise for another
  of the same kind and keep the sets you already logged.
- The live workout header shows the elapsed time, the weight lifted and the sets done, with a
  bigger Finish button.
- Orange text and links are darker in light mode, so they are easier to read.
- **New tabs: Home, Library, Start, Runs, Progress.** The Library (exercises and templates) is now
  a tab. Settings moved to the gear on Home. Tab labels are bigger.
- **A new Home.** It shows today's date, your workout in progress with a Resume button, this
  week's days with a mark for each workout and run, the week's workouts, weight lifted and
  distance run, your templates with a start button, and your recent workouts and runs together.
- The Library lists exercises in one card with a body-part dot, the equipment and your pinned
  note, opens on Exercises, and its body-part filters show the same colour dots.
- Progress has one header above the Progress, Running and History switch, and its tiles and
  cards (and those on other screens) have the new bordered style.
- **A new Runs tab.** This week's distance, a big Start a run card, Plan a route and Import GPX
  side by side, saved routes as cards with their shape, and recent runs with their shape, time,
  pace and distance.
- **A new Settings page.** One scrolling page with a card per section (Appearance, Workouts,
  Health Connect, Your data, Updates) and a chip row that jumps to a section, lights it up briefly
  and follows you as you scroll. Theme, units, rest, backups and updates save as soon as you change
  them. With animations turned off in Android, the jump is instant and nothing flashes.
- **New fonts.** Titles and numbers use Barlow Condensed and everything else Barlow, bundled with
  the app so it looks the same offline.
- **Light mode works on every screen.** Labels, hints and status colours used to stay dark-mode
  grey or pale yellow in light mode. Every screen now reads its colours from one light or dark
  set, in the warmer palette of the redesign. The ember orange stays the same.
- The live exercise card moved "Move up", "Move down", "Superset with previous" and "Remove" into a
  menu, giving the exercise name room on narrow phones.
- The template editor uses the same exercise picker as the live workout, with filters and search.
- wger sync now stores an exercise's description as how-to steps, not as your personal note.
- Export format 1.7 adds `instructions` and `videoUrl` to exercises. Older exports still import.
- Database version 8, an additive migration with no data change.
- Database version 9 and export format 1.9: template exercises can hold a plan (target sets, a
  rep range, rest and a superset group). Existing templates have no plan and work as before, and
  older backups still import.
- **Debug builds are a separate app**, "Ember dev" (`com.lukr99.workout.debug`, version
  `x.y.z-dev`). They install beside the real app and can never replace it or wipe its data. The
  updater does not run in them.
- The repository follows the CodePrint baseline: new AGENTS, ARCHITECTURE, CONTRIBUTING, SECURITY
  and CONTEXT docs, a pitfalls log, and a CI job that validates the repository.
- **Updates are verified before they install.** The updater now takes exactly
  `Ember-<version>.apk` from a release, not just the first APK, and only over HTTPS. It checks the
  file against the release's published SHA-256 and checks that it is signed by the same key as
  the installed app. Only then does it open the installer. A release without a checksum is shown
  as "cannot be verified" instead of being installed.
- Updates has a link to all releases on GitHub, the manual way to update.
- The update dialog says "Ember" instead of the old "Workout Tracker".
- `tools/publish-release.ps1` builds the signed APK, checks its signing key, writes the checksum
  and creates a draft GitHub Release with both files.
- Developer loop:
  - stable test tags that show up as resource ids;
  - JVM screenshot tests in light and dark (Robolectric and Roborazzi);
  - a Maestro launch smoke flow;
  - the CodePrint `emulator`, `ui-check` and `agent-doctor` scripts.
  - The bundled exercise image index moved to `src/main/resources`, so tests read the same file.
- Code layout follows CodePrint: one type per file, one numbered file per database migration, the
  backup worker built through the app's own WorkManager setup, and the largest data and view model
  files split by job, with new JVM tests for each piece. The app behaves the same.

### Fixed

- **Saving a template wiped its notes**, including the "Created from workout on ..." note.
- **Adding the same exercise twice to a template crashed the editor.**
- **Saving an exercise wiped its secondary muscles.** The editor never passed them on.
- The exercise editor could open an existing exercise as a blank new one while a Library search
  or filter hid it.
- **A restore was not all or nothing.** Runs and routes were saved after the main transaction, so a
  failure there left a half-restored phone. Everything now commits in one transaction, and a failed
  restore leaves no trace.
- A restored exercise kept a photo path from the phone that made the backup, which pointed at a
  file that does not exist here. The path is now cleared, or replaced by the photo in the backup.

## [2.5.2] - 2026-09-20

### Fixed

- **In-app update failed to install**: the updater never checked that a download actually finished.
  A dropped connection simply ends the response stream without raising anything, so a partial APK
  was handed to Android's package installer, which rejected it as corrupt — looking like an install
  problem rather than the half-finished download it was. The download now verifies the transferred
  byte count against the size GitHub reports, checks the HTTP status, streams to a `.part` file that
  is only put in place once complete, and deletes it otherwise.
- **Silent updater failures**: every error was discarded by `runCatching { }.getOrNull()`, leaving
  "Download failed — try again later." and nothing in the logs. Failures now show the actual reason
  and are logged.

### Changed

- **Update download is ~3x smaller**: release builds ship only `arm64-v8a`. The universal APK
  carried native libraries for four architectures — 49 MB of its 52 MB — so roughly 39 MB of every
  update was code no phone could run. Debug builds keep every ABI so emulator tests still run.
- The Updates section now reports real download progress instead of an indeterminate spinner.

## [2.5.1] - 2026-09-20

### Fixed

- **Live logging crash**: marking sets done could kill the app outright. When a set earned a
  personal record, its "PR" chip appeared in the exercise card, and the card measured its height
  with intrinsics to size the superset rail — which a lazily-laid-out chip strip cannot answer, so
  Compose threw `IllegalStateException`. Because the PR flag is persisted into the draft, reopening
  the app crashed it again within seconds, leaving a force-stop as the only way out. The chip strip
  no longer uses a lazy list, and the superset rail is painted rather than measured, so neither
  half of the fault can recur. The same crash could be triggered by any tagged set, not just a PR.

## [2.5.0] - 2026-08-30

### Added

- **Per-exercise KG/LB input**: tap the unit chip on any live exercise to enter the machine's shown
  weight directly; storage remains metric and other exercises keep their own unit choice.
- **Combinable set tags**: warm-up, drop, to failure, failed early, negative, and back-off can now be
  selected together. Legacy single set types remain compatible with older history and exports.
- **Exercise lifecycle**: exercises can be started, timed, finished, collapsed into a compact stats
  summary, expanded again, or reopened. Logging the first set starts its timer automatically.
- **Superset editor**: clearer position/count visuals plus controls to add adjacent exercises,
  remove individual members, or ungroup the block.
- **Exact share preview**: workout detail now shows the rendered postcard before opening Android's
  share sheet, and exercise cards expose sets, reps, volume, best set, and elapsed time.
- **Guided Windows installer**: a release-packaging command creates a double-click installer for a
  USB-connected Android phone, including first-time USB-debugging guidance.

### Data

- Room schema **v7** adds non-destructive exercise timing/unit fields and set-tag JSON. Portable
  exports are now **v1.6** and still accept every published format from v1.0 onward.

## [2.4.0] - 2026-08-24

### Fixed

- **Live logging**: a completed ("done") set now persists via the set's `performedAtUtc`, so its
  checkmark — and any reps/weight typed just before — survive leaving the screen or the OS reclaiming
  the process. The draft is also flushed when the app is backgrounded.
- **Exercise picker**: the catalog list now owns its scroll gesture, so a fling no longer drags the
  bottom sheet closed mid-scroll without a selection.
- **Run + lift**: when a run and a lift are both live, the centre action asks which to resume instead
  of always reopening the run (the lift was previously unreachable).
- **PR celebration**: the personal-record banner is now opaque, elevated, sits clear of the top bar,
  and stays visible longer.
- **Exercise art / sync**: broader image matching for imported/custom names (singularised-token
  fallback), and wger sync now resolves site-relative image URLs to absolute.

### Added

- **Shareable workout postcard**: a past workout can be shared as a branded image summarising volume,
  sets, exercises, time and any personal records (Workout detail → Share).

## [2.3.0] - 2026-08-22

### Added

- **In-app updater**: Settings → *Updates* now checks GitHub Releases for a newer build and, when one
  is available, downloads its APK and hands it to the system installer. No third-party dependencies
  (`HttpURLConnection` + `org.json`). In-place installs require the release APK to be signed with the
  same key as the installed build, so the first release APK is installed manually; thereafter it
  upgrades in place. Adds the `REQUEST_INSTALL_PACKAGES` permission and a FileProvider `updates/` path.

## [2.2.0] - 2026-08-10

Run Mode reliability & map pass.

### Fixed

- **Background & locked-screen tracking**: a partial wake lock now keeps the CPU awake for the duration of
  a run, so GPS fixes and the live clock keep flowing while the app is backgrounded or the phone is locked
  — instead of arriving in sparse bursts that showed up as skipped, straight-line paths.
- **Paused-and-walked segments no longer connect or count**: pausing, walking somewhere (traffic, a
  crossing), then resuming no longer joins the two ends on the map or adds the walked distance to your
  total. The trace breaks at each manual resume; distance, splits, and personal-record windows all skip
  the gap. Persisted via a new `run_points.segmentStart` flag (DB schema **v6**, additive migration).
- **Runs & routes are now in backups and exports**: the JSON export and the automatic backup only ever
  wrote strength data — runs and saved routes were silently omitted, so a restore couldn't bring them
  back. Both directions now round-trip Run Mode data (runs with their full traces, including pause
  breaks, plus saved routes). Existing backups made before this fix do **not** contain runs.

### Changed

- **Live map view**: the camera follows your heading (rotates so the road ahead is up) at a closer,
  street-level zoom, so the route you're on is legible mid-run. The recenter button re-arms both.
- **Live-run compass**: moved out from under the stats panel into the right-edge control group (next to
  the recenter and music buttons), and made a toggle — one tap locks the map to the phone's heading
  (road ahead up), the next returns it to north-up. The needle always points to true north; an ember
  tint means it's currently locked to your heading.

## [2.1.1] - 2026-08-08

Written up on 2026-10-04 from the git history.

### Fixed

- Android 16 no longer warns that the app's native libraries are not 16 KB aligned. MapLibre,
  DataStore and `androidx.graphics:graphics-path` moved to aligned versions.

## [2.1.0] - 2026-08-08

Written up on 2026-10-04 from the git history. Details are in `docs/run-mode/`.

### Added

- **Run Mode**: live GPS runs in a foreground service, with a dark map, pace and splits.
- Run detail with stats, charts and personal records, plus a running section in Progress.
  Runs sync to Health Connect.
- **Route planner**: tap to drop points, snap to roads and save. A run can start from a saved route,
  shown as a faint guide.
- A small music button that opens Spotify.
- Spoken split cues, GPX import and export, offline map tiles, run sharing and route management.

### Changed

- The app is now called **Ember**. The shell has five items with a central Start action.

## [2.0.0] - 2026-07-26

Written up on 2026-10-04 from the git history. Details are in `docs/rework/`.

### Changed

- **Native rewrite**: the app is now Kotlin, Jetpack Compose and Room. The .NET MAUI proof of
  concept is kept on the `release/1.0` branch and tag `v1.0.0`.

### Added

- Live workout logging with a number pad, set types, RIR and RPE, supersets and a rest timer.
- Templates, an exercise catalog with custom exercises, photos and wger images.
- History with editing after the fact.
- Progress with e1RM and volume charts, personal records and a muscle recovery body map.
- Progression suggestions when adding an exercise.
- JSON and CSV export and import, a Lyfta CSV importer, Health Connect sync and automatic backup.

## [1.0.0] - 2026-07-25

### Milestone

- Marks the `.NET MAUI` implementation as the **frozen 1.0 proof-of-concept**. This version
  is preserved as a historical stepping stone on the `release/1.0` branch and the `v1.0.0` tag.
- Consolidates the 0.1–0.3 line: reliable offline logging, templates, live sessions, exercise
  catalog with `wger` sync, list-first browse screens with floating navigation, full editing of
  completed workouts, toast notifications, persisted theme selection, and JSON/CSV export.

### Notes

- Active development continues on `feature/app-rework`, a ground-up rewrite as a **native
  Android (Kotlin + Jetpack Compose)** app. See `docs/rework/` for the migration plan.
- The MAUI app remains buildable from this tag as a reference for behaviour and data shape.

## [0.1.0] - 2026-04-01

### Added

- Android-first `.NET MAUI` app scaffold with local Git and GitHub-backed repository setup
- Dark-theme shell navigation with pages for Home, Templates, Catalog, History, Workout Detail, Workout Editor, and Settings
- Core workout domain model for exercises, templates, sessions, entries, strength sets, cardio data, analytics snapshots, and exports
- Local SQLite persistence layer with seeded exercises and history-safe workout snapshots
- Custom exercise support plus manual sync/import support for public exercise metadata through `wger`
- JSON and CSV export services for future desktop-manager compatibility
- Basic analytics-ready queries for workout history, consistency, and exercise progression
- Repository tests covering initialization, template-to-session behavior, analytics, and export inclusion
- Project documentation covering architecture decisions and roadmap milestones

### Notes

- The current version is a foundation release focused on reliable offline logging and future compatibility.
- Progression graphs and richer statistics are planned on top of the existing history and analytics service layer in future milestones.

# Roadmap And Milestones

## Milestone 0.1

Goal: establish a reliable offline workout logging foundation.

- Create and edit workout templates
- Start a live workout from scratch or a template
- Add, edit, and remove strength sets inline
- Log cardio duration with optional distance and calories
- Create, edit, archive, and search exercises
- Persist active workouts and completed history locally
- Export JSON and CSV

## Milestone 0.2

Goal: strengthen usability and first-layer analytics.

- [x] Add better in-app summaries on the home screen
- Show exercise-level best set, total volume, and recent trend summaries
- [x] Add filters for body part, date range, and workout type
- Improve live workout editing ergonomics and validation
- Add persisted appearance selection in settings for system, light, and dark theme modes
- Clarify strength set rows with explicit reps and kg labeling in editor and detail views
- Tighten edit, delete, archive, and discard button placement with more consistent spacing
- Add confirmation dialogs for destructive actions such as discarding workouts and deleting saved items
- Add duplicate previous workout / repeat workout shortcuts

## UI architecture follow-up

Goal: keep the app fast to use without letting every action become a full page.

- Use full pages for longer editing flows such as workouts, templates, and exercise maintenance
- Reserve dialogs and popups for confirmations and single-purpose quick actions
- Improve searchable selection flows for larger dropdowns and design a reusable editable suggestion input for common numeric fields
- Extract shared action-row and card-spacing patterns so list screens stay visually consistent

## Milestone 0.3

Goal: introduce visual progression and consistency tracking.

- [x] Rework browse screens around list-first layouts with floating actions and popup-style editors
- [x] Restore smooth scrolling and stronger filter surfaces in workout history
- [x] Separate active workout into overview, add-exercise, and logged-exercise sections
- [x] Allow full editing of completed workouts after the fact
- [x] Replace ambiguous catalog terminology and expose richer body-part context for exercises
- [x] Move settings out of the bottom nav and continue the floating-menu navigation redesign
- [x] Show transient top toast notifications for action confirmations instead of inline status blocks
- Exercise progression charts
- Workout frequency and streak views
- Body-part distribution summaries
- Weekly and monthly consistency views
- Better personal records and milestone surfaces

## Milestone 0.4

Goal: prepare for ecosystem growth.

- Stable import workflow from previously exported bundles
- Safer backup and restore flow
- Windows companion planning or initial read-only desktop viewer
- More robust exercise sync and conflict handling

## Open future ideas

- Rest timers
- Supersets and grouped exercises
- Notes per workout block
- Estimated 1RM and volume trend calculations
- Wearable or health-platform integrations
- Signed release automation and GitHub Actions CI

## Next up (2026-11-11)

Ranked ideas after the 2.10.1 release. Each one has an item on the Ember GoalMaker board.

1. Web edition: read-only runs for runs imported from Android.
2. Backup reminder when the last export is old.
3. Progression suggestions: propose the next weight and reps from the last session.
4. Plate calculator and warm-up set generator in the live workout.
5. Training programs: multi-week plans that schedule templates and show what is next on Home.
6. Body weight and measurements log with a chart.
7. Home screen widget with the next workout and a start button.
8. Wear OS or notification controls for the rest timer and set logging.

Already on the board: favorites when searching for an exercise, AI chat in live workouts,
first-launch personalization, sharable catalog and templates.

Known tooling bugs: web CI check fails when Chromium cannot write its cache, and CI flakes on
emulator and Robolectric downloads.

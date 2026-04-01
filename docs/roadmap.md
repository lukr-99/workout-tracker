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

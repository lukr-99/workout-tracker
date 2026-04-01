# Design Decisions

## Product direction

- The app is `Android-first`, not general cross-platform first.
- The implementation still keeps `WorkoutTracker.Core` UI-agnostic so a Windows companion can reuse the same domain and persistence logic later.
- The app is intentionally offline-first and local-only in v0.1.

## Technology choices

- `.NET MAUI` was chosen because the primary development language is `C#`.
- `CommunityToolkit.Mvvm` handles the viewmodel command/property plumbing.
- `sqlite-net-pcl` is used for local persistence to keep the first version lightweight and easy to reason about.

## Data and history rules

- Completed workout history must preserve what was actually performed.
- Workout entries store snapshot exercise fields so later exercise edits do not rewrite history.
- Template edits do not mutate old workout sessions.
- Exercise deletion is handled as archiving so past data remains intact.

## Exercise catalog strategy

- The app ships with a small seeded local catalog.
- Custom exercises are first-class and editable.
- Public catalog sync is additive and currently uses `wger` as the external source.
- Synced exercises store external ids so future refreshes can update metadata without touching custom records.

## Analytics strategy

- v0.1 stores raw data needed for future statistics instead of hardcoding chart-specific tables.
- Analytics are exposed through service interfaces and derived queries.
- Progression graphs, consistency dashboards, and exercise summaries should be computed from history, not from duplicated summary state unless a future performance need justifies it.

## Export strategy

- JSON export is the long-term interoperability format for a future computer manager.
- CSV export exists for immediate spreadsheet visibility and manual inspection.
- Export includes enough raw history to support later external analytics.

## UI direction

- Dark theme is the default and intended look.
- The layout favors compact, spreadsheet-like editing over card-heavy mobile patterns.
- Live workout editing prioritizes speed and direct manipulation over wizard-like flows.
- Full pages are preferred for rich editing flows; dialogs should stay limited to confirmations and compact one-shot actions.
- Appearance preferences should be user-selectable and persisted locally instead of being hardcoded in the app bootstrap.
- Browse-heavy areas such as catalog, templates, and history should default to list-first layouts with overlay editors instead of stacking editor forms above the content feed.
- Exercise selection surfaces should show category and body-part context directly in the chooser instead of relying on generic picker rows.
- Short-lived success and error feedback should use top toast notifications instead of consuming layout space inside the page content.

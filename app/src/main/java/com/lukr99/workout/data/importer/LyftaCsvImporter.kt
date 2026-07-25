package com.lukr99.workout.data.importer

/**
 * TODO(Phase 3): Map the Lyfta CSV export into our domain and merge it via [WorkoutRepository].
 *
 * Out of scope for Phase 1 (data core). The mapping — columns, superset ids, set types, timed sets,
 * dedup against existing history — is specified in `docs/rework/05-lyfta-import.md`, with the
 * captured fixture at `import/lyfta/lyfta-export.csv`. The additive fields this importer needs
 * (`StrengthSet.setType`/`durationSeconds`, `WorkoutEntry.supersetGroup`) already exist as of the
 * `1.1` schema, so Phase 3 is a pure mapping job with no schema change.
 */
object LyftaCsvImporter {
    // Intentionally empty until Phase 3.
}

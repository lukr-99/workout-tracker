package com.lukr99.workout.domain.run

import kotlinx.serialization.Serializable

/**
 * Portable Run Mode domain model — the running counterpart to the strength `domain/Models.kt`.
 *
 * Pure Kotlin with **no Android imports** and the same wire conventions as the strength model:
 * timestamps are epoch-millis UTC `Long`, enums serialize as their Int **ordinal**, and every field
 * has a default so the `@Serializable` shapes double as the export contract (`ExportBundle` `1.5`).
 * Room persistence lives in `data/run/RunEntities.kt`; [com.lukr99.workout.data.run.RunRepository]
 * maps between the two. Analytics (pace, splits, PRs) operate on these models and stay portable.
 */

/** Where a run originated. Ordinal-stable — appended-to only, never reordered (wire format). */
enum class RunSource { Local, Imported, HealthConnect }

package com.lukr99.workout.domain

import java.util.UUID
import kotlinx.serialization.Serializable

/**
 * The portable domain model — ported from the MAUI `WorkoutTracker.Core/Domain/Models.cs`.
 *
 * These classes are the app's shared vocabulary **and** the `@Serializable` export contract
 * (see `data/export/ExportBundle.kt`). They are pure Kotlin with **no Android imports**, so the
 * analytics that operate on them stay portable toward a future desktop tool. Room persistence uses
 * a separate set of `@Entity` shapes in `data/`; the repository maps between the two.
 *
 * Timestamps are epoch-millis UTC `Long`; on the wire they become ISO-8601 strings
 * ([InstantMillisSerializer]) and enums become their Int ordinal, matching the `v1.0` export.
 */

/** MAUI `Guid.NewGuid().ToString("N")` — 32 lowercase hex chars, no dashes. */
fun newId(): String = UUID.randomUUID().toString().replace("-", "")

/** Includes the legacy exclusive type so older workouts display exactly as they did before v1.6. */
val StrengthSet.effectiveTags: Set<SetTag>
    get() = if (tags.isNotEmpty()) tags else buildSet {
        if (isWarmup) add(SetTag.Warmup)
        when (setType) {
            SetType.Normal -> Unit
            SetType.Warmup -> add(SetTag.Warmup)
            SetType.Drop -> add(SetTag.Drop)
            SetType.Failure -> add(SetTag.ToFailure)
            SetType.Negative -> add(SetTag.Negative)
            SetType.BackOff -> add(SetTag.BackOff)
        }
    }

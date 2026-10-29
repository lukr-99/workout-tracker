package com.lukr99.workout.ui.components

import androidx.compose.ui.graphics.Color
import com.lukr99.workout.domain.recovery.RecoveryState
import com.lukr99.workout.domain.recovery.WeeklyVolumeStatus
import com.lukr99.workout.ui.theme.EmberColors

/** The colours the body map, its legend and its rows share, so one colour always means one thing. */
object MuscleTints {
    fun recovery(state: RecoveryState, colors: EmberColors): Color = when (state) {
        RecoveryState.Ready -> colors.success
        RecoveryState.Recovering -> colors.warning
        RecoveryState.Fatigued -> colors.danger
    }

    /**
     * A pale green below the target, full green in range and violet above it. Null for a muscle with
     * no hard sets this week, which stays neutral.
     */
    fun weekly(status: WeeklyVolumeStatus, colors: EmberColors): Color? = when (status) {
        WeeklyVolumeStatus.None -> null
        WeeklyVolumeStatus.Below -> colors.success.copy(alpha = 0.4f)
        WeeklyVolumeStatus.InRange -> colors.success
        WeeklyVolumeStatus.Above -> colors.violet
    }
}

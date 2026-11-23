package com.lukr99.workout.ui.components

import com.lukr99.workout.ui.i18n.Msg
import com.lukr99.workout.ui.i18n.RecoveryText

/** What the Progress body map colours: recovery now, or hard sets in the last seven days. */
enum class MuscleMapMode(val label: Msg) {
    Recovery(RecoveryText.modeRecovery),
    WeeklySets(RecoveryText.modeWeeklySets),
}

package com.lukr99.workout.ui.settings

import com.lukr99.workout.ui.i18n.Msg
import com.lukr99.workout.ui.i18n.SettingsText

/** The Settings sections, in CodePrint's order: general first, then the app's own, data, updates. */
enum class SettingsSection(val title: Msg, val description: Msg) {
    Appearance(SettingsText.appearanceTitle, SettingsText.appearanceSummary),
    Workouts(SettingsText.workoutsTitle, SettingsText.workoutsSummary),
    Locations(SettingsText.locationsTitle, SettingsText.locationsSummary),
    HealthConnect(SettingsText.healthConnectTitle, SettingsText.healthConnectSummary),
    Claude(SettingsText.claudeTitle, SettingsText.claudeSummary),
    YourData(SettingsText.yourDataTitle, SettingsText.yourDataSummary),
    Updates(SettingsText.updatesTitle, SettingsText.updatesSummary),
}

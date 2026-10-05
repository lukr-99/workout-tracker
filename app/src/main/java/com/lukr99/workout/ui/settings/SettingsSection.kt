package com.lukr99.workout.ui.settings

/** The Settings sections, in CodePrint's order: general first, then the app's own, data, updates. */
enum class SettingsSection(val title: String, val description: String) {
    Appearance("Appearance", "How Ember looks on this phone."),
    Workouts("Workouts", "Units, rest and the exercise catalog."),
    HealthConnect("Health Connect", "Share workouts and runs with other health apps."),
    YourData("Your data", "Backups, import and export. Everything stays on this phone."),
    Updates("Updates", "Ember updates from its GitHub releases."),
}

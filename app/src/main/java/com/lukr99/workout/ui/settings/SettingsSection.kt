package com.lukr99.workout.ui.settings

/** The Settings sections, in CodePrint's order: general first, then the app's own, data, updates. */
enum class SettingsSection(val title: String, val description: String) {
    Appearance("Appearance", "How Ember looks on this phone."),
    Workouts("Workouts", "Units, rest and the exercise catalog."),
    Locations("Locations", "Gyms and places you train at."),
    HealthConnect("Health Connect", "Share workouts and runs with other health apps."),
    Claude("Claude", "Let Claude read your training when you allow it, and propose templates."),
    YourData("Your data", "Backups, import and export. Everything stays on this phone unless you share it."),
    Updates("Updates", "Ember updates from its GitHub releases."),
}

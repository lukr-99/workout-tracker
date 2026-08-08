package com.lukr99.workout.data.health

/** Android-free shapes used to test Health Connect synchronization without a provider/device. */
internal data class HealthWorkoutRecord(
    val recordId: String = "",
    val clientRecordId: String? = null,
    val dataOriginPackageName: String = "",
    val title: String,
    val notes: String = "",
    val startTimeUtcMillis: Long,
    val endTimeUtcMillis: Long,
    val exerciseType: Int,
    val bodyweightKg: Double? = null,
    /** Run-only extras (R2): total distance, energy, and the GPS route for an ExerciseRoute. */
    val distanceMeters: Double? = null,
    val totalEnergyKcal: Double? = null,
    val route: List<HealthRoutePoint> = emptyList(),
)

/** One point of a run's GPS route written as a Health Connect `ExerciseRoute.Location`. */
internal data class HealthRoutePoint(
    val timeUtcMillis: Long,
    val lat: Double,
    val lon: Double,
    val altitudeM: Double? = null,
)

enum class HealthConnectAvailability {
    Available,
    ProviderUpdateRequired,
    Unavailable,
}

internal interface HealthConnectGateway {
    val requiredPermissions: Set<String>

    suspend fun availability(): HealthConnectAvailability

    suspend fun grantedPermissions(): Set<String>

    suspend fun readExerciseSessions(
        fromUtcMillis: Long,
        toUtcMillis: Long,
    ): List<HealthWorkoutRecord>

    suspend fun writeExerciseSessions(records: List<HealthWorkoutRecord>)
}

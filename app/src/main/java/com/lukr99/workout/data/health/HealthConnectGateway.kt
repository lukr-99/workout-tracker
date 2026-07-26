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

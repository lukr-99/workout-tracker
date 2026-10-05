package com.lukr99.workout.data.health

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

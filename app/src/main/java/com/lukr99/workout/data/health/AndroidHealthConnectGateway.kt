package com.lukr99.workout.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs

/** The only adapter that touches Health Connect SDK record types. */
internal class AndroidHealthConnectGateway(context: Context) : HealthConnectGateway {
    private val appContext = context.applicationContext

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getWritePermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    override suspend fun availability(): HealthConnectAvailability =
        when (HealthConnectClient.getSdkStatus(appContext)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectAvailability.ProviderUpdateRequired
            else -> HealthConnectAvailability.Unavailable
        }

    override suspend fun grantedPermissions(): Set<String> =
        clientOrNull()?.permissionController?.getGrantedPermissions().orEmpty()

    override suspend fun readExerciseSessions(
        fromUtcMillis: Long,
        toUtcMillis: Long,
    ): List<HealthWorkoutRecord> {
        val client = clientOrNull() ?: return emptyList()
        val sessions = mutableListOf<HealthWorkoutRecord>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(fromUtcMillis),
                        Instant.ofEpochMilli(toUtcMillis),
                    ),
                    pageToken = pageToken,
                ),
            )
            sessions += response.records.map(ExerciseSessionRecord::toGatewayRecord)
            pageToken = response.pageToken
        } while (pageToken != null)

        val weights = mutableListOf<WeightRecord>()
        pageToken = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        Instant.ofEpochMilli(fromUtcMillis),
                        Instant.ofEpochMilli(toUtcMillis),
                    ),
                    pageToken = pageToken,
                ),
            )
            weights += response.records
            pageToken = response.pageToken
        } while (pageToken != null)

        return sessions.map { session ->
            val closest = weights.minByOrNull {
                abs(it.time.toEpochMilli() - session.startTimeUtcMillis)
            }?.takeIf {
                abs(it.time.toEpochMilli() - session.startTimeUtcMillis) <= WeightMatchWindowMillis
            }
            session.copy(bodyweightKg = closest?.weight?.inKilograms)
        }
    }

    override suspend fun writeExerciseSessions(records: List<HealthWorkoutRecord>) {
        val client = clientOrNull() ?: return
        if (records.isEmpty()) return
        client.insertRecords(
            records.flatMap { record ->
                buildList<Record> {
                    add(record.toHealthConnectRecord())
                    record.toWeightRecord()?.let(::add)
                    record.toDistanceRecord()?.let(::add)
                    record.toEnergyRecord()?.let(::add)
                }
            },
        )
    }

    private fun clientOrNull(): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(appContext) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(appContext)
        } else {
            null
        }

    private companion object {
        const val WeightMatchWindowMillis = 6 * 60 * 60 * 1_000L
    }
}

private fun ExerciseSessionRecord.toGatewayRecord() = HealthWorkoutRecord(
    recordId = metadata.id,
    clientRecordId = metadata.clientRecordId,
    dataOriginPackageName = metadata.dataOrigin.packageName,
    title = title.orEmpty(),
    notes = notes.orEmpty(),
    startTimeUtcMillis = startTime.toEpochMilli(),
    endTimeUtcMillis = endTime.toEpochMilli(),
    exerciseType = exerciseType,
)

private fun HealthWorkoutRecord.toHealthConnectRecord() = ExerciseSessionRecord(
    startTime = Instant.ofEpochMilli(startTimeUtcMillis),
    startZoneOffset = ZoneOffset.UTC,
    endTime = Instant.ofEpochMilli(endTimeUtcMillis),
    endZoneOffset = ZoneOffset.UTC,
    exerciseType = exerciseType,
    title = title,
    notes = notes.takeIf(String::isNotBlank),
    metadata = healthMetadata(clientRecordId),
    exerciseRoute = route.takeIf { it.isNotEmpty() }?.let { points ->
        ExerciseRoute(
            points.map {
                ExerciseRoute.Location(
                    time = Instant.ofEpochMilli(it.timeUtcMillis),
                    latitude = it.lat,
                    longitude = it.lon,
                    altitude = it.altitudeM?.let(Length::meters),
                )
            },
        )
    },
)

private fun HealthWorkoutRecord.toDistanceRecord(): DistanceRecord? = distanceMeters
    ?.takeIf { it > 0 }
    ?.let { meters ->
        DistanceRecord(
            startTime = Instant.ofEpochMilli(startTimeUtcMillis),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochMilli(endTimeUtcMillis),
            endZoneOffset = ZoneOffset.UTC,
            distance = Length.meters(meters),
            metadata = healthMetadata(clientRecordId?.let { "$it:distance" }),
        )
    }

private fun HealthWorkoutRecord.toEnergyRecord(): TotalCaloriesBurnedRecord? = totalEnergyKcal
    ?.takeIf { it > 0 }
    ?.let { kcal ->
        TotalCaloriesBurnedRecord(
            startTime = Instant.ofEpochMilli(startTimeUtcMillis),
            startZoneOffset = ZoneOffset.UTC,
            endTime = Instant.ofEpochMilli(endTimeUtcMillis),
            endZoneOffset = ZoneOffset.UTC,
            energy = Energy.kilocalories(kcal),
            metadata = healthMetadata(clientRecordId?.let { "$it:energy" }),
        )
    }

private fun HealthWorkoutRecord.toWeightRecord(): WeightRecord? = bodyweightKg
    ?.takeIf { it > 0 }
    ?.let { kilograms ->
        WeightRecord(
            time = Instant.ofEpochMilli(startTimeUtcMillis),
            zoneOffset = ZoneOffset.UTC,
            weight = Mass.kilograms(kilograms),
            metadata = healthMetadata(clientRecordId?.let { "$it:weight" }),
        )
    }

private fun healthMetadata(clientRecordId: String?) = Metadata(
    clientRecordId = clientRecordId,
    clientRecordVersion = 1,
    recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY,
)

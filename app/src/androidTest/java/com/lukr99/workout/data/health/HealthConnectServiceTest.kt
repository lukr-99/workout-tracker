package com.lukr99.workout.data.health

import android.content.Context
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.WorkoutDb
import com.lukr99.workout.data.WorkoutRepository
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.WorkoutSessionSource
import com.lukr99.workout.domain.WorkoutSessionStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HealthConnectServiceTest {
    private lateinit var db: WorkoutDb
    private lateinit var repository: WorkoutRepository

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            WorkoutDb::class.java,
        ).build()
        repository = WorkoutRepository(db.workoutDao())
    }

    @After
    fun cleanup() {
        db.close()
    }

    @Test
    fun exportIsStableAndImportIsDeduplicated() = runTest {
        repository.saveWorkoutSession(
            WorkoutSession(
                id = "local",
                name = "Strength",
                startedAtUtc = 1_000,
                endedAtUtc = 61_000,
                status = WorkoutSessionStatus.Completed,
                entries = listOf(
                    WorkoutEntry(
                        id = "entry",
                        workoutSessionId = "local",
                        exerciseSnapshotName = "Squat",
                        entryType = ExerciseCategory.Strength,
                        strengthSets = listOf(
                            StrengthSet(
                                id = "set",
                                workoutEntryId = "entry",
                                reps = 5,
                                weightKg = 100.0,
                            ),
                        ),
                    ),
                ),
            ),
        )
        val gateway = FakeGateway()
        val service = HealthConnectService(repository, gateway)

        assertEquals(1, service.exportCompletedSessions().exported)
        assertEquals(1, gateway.written.size)

        gateway.toRead = listOf(
            HealthWorkoutRecord(
                recordId = "provider-record",
                title = "Imported run",
                startTimeUtcMillis = 100_000,
                endTimeUtcMillis = 200_000,
                exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ),
        )
        assertEquals(1, service.importSessions(0, 300_000).imported)
        assertEquals(1, service.importSessions(0, 300_000).skipped)
        assertEquals(
            WorkoutSessionSource.HealthConnect,
            repository.getSessions().single { it.name == "Imported run" }.source,
        )
    }

    @Test
    fun platformAvailabilityCanBeQueriedWithoutAssumingProviderInstallation() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val availability = AndroidHealthConnectGateway(context).availability()

        assertTrue(availability in HealthConnectAvailability.entries)
    }

    private class FakeGateway : HealthConnectGateway {
        override val requiredPermissions = setOf("read", "write")
        var toRead = emptyList<HealthWorkoutRecord>()
        val written = mutableListOf<HealthWorkoutRecord>()

        override suspend fun availability() = HealthConnectAvailability.Available
        override suspend fun grantedPermissions() = requiredPermissions

        override suspend fun readExerciseSessions(
            fromUtcMillis: Long,
            toUtcMillis: Long,
        ) = toRead

        override suspend fun writeExerciseSessions(records: List<HealthWorkoutRecord>) {
            written += records
        }
    }
}

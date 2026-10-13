package com.lukr99.workout.data

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.lukr99.workout.data.run.ShareCardRenderer
import com.lukr99.workout.domain.CardioEntryData
import com.lukr99.workout.domain.ExerciseCategory
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.domain.WorkoutEntry
import com.lukr99.workout.domain.WorkoutSession
import com.lukr99.workout.domain.run.Run
import com.lukr99.workout.domain.run.TracePoint
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The workout and run share images, drawn by the real renderers. `recordRoborazziDebug` writes
 * `src/test/screenshots/share_card_*.png` so a change to either card can be checked by eye.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class ShareCardScreenshotTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // 13 Oct 2026, around noon UTC.
    private val noon = 1_791_892_800_000L

    private fun sets(vararg pairs: Pair<Double, Int>, prIndex: Int = -1) = pairs.mapIndexed { i, (kg, reps) ->
        StrengthSet(weightKg = kg, reps = reps, isWarmup = i == 0 && kg > 0 && pairs.size > 3, isPr = i == prIndex)
    }

    private fun lift(name: String, order: Int, sets: List<StrengthSet>) =
        WorkoutEntry(exerciseSnapshotName = name, sortOrder = order, strengthSets = sets)

    private val session = WorkoutSession(
        name = "Upper Body Power and Accessories",
        startedAtUtc = noon,
        completedDateUtc = noon,
        durationSeconds = 4_380,
        entries = listOf(
            lift("Bench Press", 0, sets(60.0 to 10, 100.0 to 5, 102.5 to 5, 100.0 to 6, prIndex = 2)),
            lift("Weighted Pull-up", 1, sets(20.0 to 6, 20.0 to 6, 20.0 to 5)),
            lift("Seated Dumbbell Shoulder Press With A Very Long Name", 2, sets(30.0 to 10, 32.5 to 8)),
            lift("Barbell Row", 3, sets(50.0 to 10, 80.0 to 8, 80.0 to 8, 85.0 to 6, prIndex = 3)),
            lift("Dips", 4, sets(0.0 to 15, 0.0 to 12)),
            lift("Cable Fly", 5, sets(15.0 to 15, 17.5 to 12)),
            WorkoutEntry(
                exerciseSnapshotName = "Rowing Machine",
                sortOrder = 6,
                entryType = ExerciseCategory.Cardio,
                exerciseSnapshotCategory = ExerciseCategory.Cardio,
                cardioData = CardioEntryData(durationSeconds = 600, distanceKm = 2.1),
            ),
            lift("Face Pull", 7, sets(25.0 to 15, 25.0 to 15)),
            lift("Hammer Curl", 8, sets(16.0 to 10, 16.0 to 10)),
            lift("Triceps Pushdown", 9, sets(30.0 to 12, 32.5 to 10)),
            lift("Plank", 10, sets(0.0 to 1)),
        ),
    )

    @Test
    fun workoutCard() {
        WorkoutShareCardRenderer(context).render(session, imperial = false)
            .captureRoboImage("src/test/screenshots/share_card_workout.png")
    }

    @Test
    fun workoutCardShort() {
        val short = session.copy(name = "Legs", entries = session.entries.take(3))
        WorkoutShareCardRenderer(context).render(short, imperial = true)
            .captureRoboImage("src/test/screenshots/share_card_workout_short.png")
    }

    @Test
    fun runCard() {
        // A lopsided loop so the trace has a recognisable shape.
        val trace = (0..120).map { i ->
            val a = i / 120.0 * 2 * Math.PI
            val r = 0.006 * (1 + 0.35 * sin(3 * a))
            TracePoint(t = noon + i * 10_000L, lat = 49.19 + r * sin(a), lon = 16.6 + r * 1.5 * cos(a))
        }
        val run = Run(
            startedAtUtc = noon,
            durationSeconds = 1_900,
            movingSeconds = 1_845,
            distanceMeters = 6_420.0,
            avgPaceSecPerKm = 287.4,
            trace = trace,
        )
        ShareCardRenderer(context).render(run, imperial = false)
            .captureRoboImage("src/test/screenshots/share_card_run.png")
    }
}

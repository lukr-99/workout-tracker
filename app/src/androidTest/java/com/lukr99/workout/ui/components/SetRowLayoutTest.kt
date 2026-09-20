package com.lukr99.workout.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.lukr99.workout.domain.SetTag
import com.lukr99.workout.domain.StrengthSet
import com.lukr99.workout.settings.UnitSystem
import com.lukr99.workout.ui.theme.WorkoutTheme
import org.junit.Rule
import org.junit.Test

/**
 * Regression guard for the v2.5.0 live-logging crash. Marking a set done can earn it a PR; the PR
 * chip strip then appeared inside the exercise card, whose superset rail sized the card with
 * [IntrinsicSize.Min]. The strip was a `LazyRow`, and a `SubcomposeLayout` cannot answer an
 * intrinsic measurement — so the app died with `IllegalStateException`, and died again on every
 * relaunch because `isPr` is persisted into the draft.
 *
 * A [SetRow] must stay measurable under intrinsic constraints whatever tags its set carries.
 */
class SetRowLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun prSetRowSurvivesIntrinsicHeightMeasurement() {
        renderUnderIntrinsicHeight(StrengthSet(reps = 8, weightKg = 100.0, isPr = true))

        compose.onNodeWithContentDescription("mark set not done").assertIsDisplayed()
        // Twice: the set badge and the tag chip that used to be a LazyRow. Both must be laid out,
        // otherwise this test would pass on a set row that quietly stopped rendering its chips.
        compose.onAllNodesWithText("PR").assertCountEquals(2)
    }

    @Test
    fun taggedSetRowSurvivesIntrinsicHeightMeasurement() {
        renderUnderIntrinsicHeight(
            StrengthSet(reps = 12, weightKg = 40.0, tags = setOf(SetTag.Warmup, SetTag.ToFailure)),
        )

        compose.onNodeWithContentDescription("mark set not done").assertIsDisplayed()
        compose.onNodeWithText("WARM-UP").assertIsDisplayed()
        compose.onNodeWithText("TO FAILURE").assertIsDisplayed()
    }

    /** Mirrors a superset card: a full-height rail beside content sized by [IntrinsicSize.Min]. */
    private fun renderUnderIntrinsicHeight(set: StrengthSet) {
        compose.setContent {
            WorkoutTheme(dark = true) {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    Box(Modifier.width(3.dp).fillMaxHeight().background(Color.Red))
                    Column(Modifier.weight(1f)) {
                        SetRow(
                            index = 0,
                            set = set,
                            units = UnitSystem.Metric,
                            done = true,
                            onReps = {},
                            onWeightKg = {},
                            onToggleDone = {},
                            onOptions = {},
                        )
                    }
                }
            }
        }
    }
}

package com.lukr99.workout.ui.android

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.claude.Proposal
import com.lukr99.workout.ui.theme.WorkoutTheme
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Home shows Claude's waiting proposals with Accept and Reject, so they are seen without push. */
@RunWith(AndroidJUnit4::class)
// A plain Application: WorkoutApp.onCreate starts Room work that leaks into the next test (docs/pitfalls.md).
@Config(sdk = [34], application = android.app.Application::class)
class ClaudeProposalsCardTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun oneTemplateShowsItsPlanAndTheButtonsAnswerIt() {
        val accepted = mutableListOf<Proposal>()
        val rejected = mutableListOf<Proposal>()
        val upper = template("p1", "Upper A")
        compose.setContent {
            WorkoutTheme(dark = true) {
                ClaudeProposalsCard(listOf(upper), busy = false, onAccept = { accepted += it }, onReject = { rejected += it })
            }
        }

        compose.onNodeWithText("Claude proposed a template").assertExists()
        compose.onNodeWithText("Upper A").assertExists()
        compose.onNodeWithText("Bench is up 6% in two weeks.").assertExists()
        compose.onNodeWithText("Barbell Bench Press: 4 x 6-8, rest 3:00, RIR 2").assertExists()
        compose.onNodeWithText("Accept").performClick()
        compose.onNodeWithText("Reject").performClick()

        compose.runOnIdle {
            assertEquals(listOf(upper), accepted)
            assertEquals(listOf(upper), rejected)
        }
    }

    @Test
    fun moreThanTwoPointToSettings() {
        compose.setContent {
            WorkoutTheme(dark = true) {
                ClaudeProposalsCard(listOf(template("p1", "A"), template("p2", "B"), template("p3", "C")), busy = false, onAccept = {}, onReject = {})
            }
        }

        compose.onNodeWithText("Claude proposed 3 things").assertExists()
        compose.onNodeWithText("1 more in Settings, Claude.").assertExists()
        assertEquals(2, compose.onAllNodesWithText("Accept").fetchSemanticsNodes().size)
        compose.onNodeWithText("C").assertDoesNotExist()
    }

    private fun template(id: String, title: String) = Proposal(
        id = id,
        kind = "template",
        status = "pending",
        title = title,
        reason = "Bench is up 6% in two weeks.",
        payload = Json.parseToJsonElement(
            """{"templateId":"t-$id","name":"$title","exercises":[{"exerciseId":"e1","name":"Barbell Bench Press","category":"strength",
               "bodyPart":"Chest","notes":"RIR 2","targetSets":4,"repsMin":6,"repsMax":8,"restSeconds":180}]}""",
        ) as JsonObject,
        createdAt = "2026-11-11T18:49:29Z",
    )
}

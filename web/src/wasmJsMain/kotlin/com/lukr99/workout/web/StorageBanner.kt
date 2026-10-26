package com.lukr99.workout.web

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.lukr99.workout.data.document.StorageState
import com.lukr99.workout.ui.theme.EmberTheme
import com.lukr99.workout.web.platform.downloadText
import com.lukr99.workout.web.storage.WebStorage
import kotlinx.coroutines.launch

/**
 * A lasting notice at the top of the page when the browser does not keep what the owner enters: a
 * save failed, storage is not available at all, or stored data could not be read. It never hides on
 * its own, so a problem with the data cannot go unseen.
 */
@Composable
internal fun StorageBanner(storage: WebStorage, modifier: Modifier = Modifier) {
    val status by storage.status.collectAsState()
    val scope = rememberCoroutineScope()
    val colors = EmberTheme.colors
    val notices = buildList {
        when (val state = status.state) {
            is StorageState.SaveFailed -> add(
                Notice(
                    text = "Your last change could not be saved (${state.reason}). It is kept until you close " +
                        "the page. Free up space or export a backup in Settings.",
                    action = "Try again",
                    onAction = { scope.launch { storage.database.retrySave() } },
                ),
            )
            is StorageState.MemoryOnly -> add(
                Notice(
                    text = "This browser does not let Ember store data (${state.reason}). Everything you enter " +
                        "is lost when you close the page. Export a backup before you leave.",
                ),
            )
            StorageState.Opening, StorageState.Saved -> Unit
        }
        if (status.setAside != null) {
            add(
                Notice(
                    text = "Some stored data could not be read. It was set aside and nothing was overwritten.",
                    action = "Download it",
                    onAction = {
                        scope.launch {
                            storage.database.quarantined().forEach { entry ->
                                downloadText("ember-unreadable-${entry.key}.json", entry.text, "application/json")
                            }
                        }
                    },
                ),
            )
        }
    }
    if (notices.isEmpty()) return
    Column(
        modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        notices.forEach { notice ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.danger).padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(notice.text, Modifier.weight(1f), color = colors.onDanger, style = MaterialTheme.typography.bodyMedium)
                notice.action?.let { label ->
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = notice.onAction) { Text(label, color = colors.onDanger) }
                }
            }
        }
    }
}

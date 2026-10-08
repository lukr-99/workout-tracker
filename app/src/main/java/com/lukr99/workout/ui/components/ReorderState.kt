package com.lukr99.workout.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Drag to reorder inside a LazyColumn. Holding an item's handle picks it up; dragging moves it past
 * the item under its middle, and the list scrolls when it reaches an edge. Only keys [canMove]
 * accepts take part, so headers and footers stay put. [onMove] swaps two items by key while the
 * finger moves; [onDrop] runs once when it lifts.
 */
class ReorderState internal constructor(
    private val listState: LazyListState,
    private val scope: CoroutineScope,
    private val canMove: (Any) -> Boolean,
    private val onMove: (from: Any, to: Any) -> Unit,
    private val onDrop: () -> Unit,
    private val haptics: HapticFeedback,
) {
    /** The key of the item being dragged, or null. */
    var draggingKey by mutableStateOf<Any?>(null)
        private set

    private var startOffset = 0
    private var dragged by mutableFloatStateOf(0f)

    private val draggingItem: LazyListItemInfo?
        get() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingKey }

    /** How far to draw the dragged item from where the list lays it out. */
    val draggingOffset: Float
        get() = draggingItem?.let { startOffset + dragged - it.offset } ?: 0f

    internal fun start(key: Any) {
        val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        draggingKey = key
        startOffset = item.offset
        dragged = 0f
    }

    internal fun drag(dy: Float) {
        dragged += dy
        val item = draggingItem ?: return
        val top = startOffset + dragged
        val middle = (top + item.size / 2f).toInt()
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.key != item.key && canMove(it.key) && middle in it.offset..(it.offset + it.size)
        }
        if (target != null) {
            // Moving the first visible item would make the list follow it; pin the scroll position.
            if (item.index == listState.firstVisibleItemIndex || target.index == listState.firstVisibleItemIndex) {
                scope.launch { listState.scrollToItem(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) }
            }
            onMove(item.key, target.key)
            return
        }
        val info = listState.layoutInfo
        val pastEnd = top + item.size - info.viewportEndOffset
        val pastStart = top - info.viewportStartOffset
        when {
            pastEnd > 0 -> scope.launch { listState.scrollBy(pastEnd.coerceAtMost(EDGE_SCROLL_STEP)) }
            pastStart < 0 -> scope.launch { listState.scrollBy(pastStart.coerceAtLeast(-EDGE_SCROLL_STEP)) }
        }
    }

    internal fun end() {
        if (draggingKey != null) onDrop()
        draggingKey = null
        dragged = 0f
    }

    private companion object {
        const val EDGE_SCROLL_STEP = 40f
    }
}

@Composable
fun rememberReorderState(
    listState: LazyListState,
    canMove: (Any) -> Boolean,
    onMove: (from: Any, to: Any) -> Unit,
    onDrop: () -> Unit = {},
): ReorderState {
    val scope = rememberCoroutineScope()
    val move by rememberUpdatedState(onMove)
    val drop by rememberUpdatedState(onDrop)
    val movable by rememberUpdatedState(canMove)
    val haptics = LocalHapticFeedback.current
    return remember(listState) { ReorderState(listState, scope, { movable(it) }, { a, b -> move(a, b) }, { drop() }, haptics) }
}

/** Makes this element the handle that picks up item [key] after a long press. */
fun Modifier.reorderHandle(state: ReorderState, key: Any): Modifier = pointerInput(state, key) {
    detectDragGesturesAfterLongPress(
        onDragStart = { state.start(key) },
        onDrag = { change, amount ->
            change.consume()
            state.drag(amount.y)
        },
        onDragEnd = state::end,
        onDragCancel = state::end,
    )
}

package com.lukr99.workout.web.platform

/** Adds a history entry for the page, so the browser's back gesture has somewhere to go that stays here. */
internal fun pushHistoryEntry(): Unit = js("history.pushState({ ember: true }, '')")

/** Calls [onPop] whenever the browser goes back (or forward) through the page's history entries. */
internal fun onHistoryPop(onPop: () -> Unit): Unit = js("window.addEventListener('popstate', function () { onPop(); })")

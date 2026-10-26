package com.lukr99.workout.web.platform

/**
 * Lets the browser's back button and the iPhone's back swipe do what Android's back gesture does.
 *
 * While any screen has something for back to do, the page keeps one history entry of its own on top
 * of the stack. Going back pops that entry instead of leaving the page, the innermost enabled handler
 * runs, and the entry is put back while there is still somewhere to go back to. The page never moves
 * through history itself (a scripted back is asynchronous and races the next push), so on the home
 * screen an entry may be left over, and the first back there only uses it up.
 *
 * Handlers are ordered by when they were registered, and enabling one does not move it, as with
 * Android's back dispatcher: a sheet opened over a screen stays innermost while the screen's own
 * handler comes and goes.
 */
internal class BrowserBack(
    private val pushEntry: () -> Unit = ::pushHistoryEntry,
    listen: (onPop: () -> Unit) -> Unit = ::onHistoryPop,
) {
    private val handlers = mutableListOf<Handler>()
    private var entryPushed = false

    init {
        listen(::popped)
    }

    /** Registers [onBack] as the innermost handler. */
    fun register(enabled: Boolean, onBack: () -> Unit): Handler = Handler(enabled, onBack).also {
        handlers += it
        sync()
    }

    /** One registered handler; disabled, it is passed over, and removed, it is gone. */
    inner class Handler internal constructor(enabled: Boolean, private val onBack: () -> Unit) {
        var enabled: Boolean = enabled
            set(value) {
                field = value
                sync()
            }

        fun remove() {
            handlers.remove(this)
        }

        internal fun handle() = onBack()
    }

    private fun popped() {
        entryPushed = false
        handlers.lastOrNull { it.enabled }?.handle()
        sync()
    }

    private fun sync() {
        if (!entryPushed && handlers.any { it.enabled }) {
            pushEntry()
            entryPushed = true
        }
    }

    internal companion object {
        /** The page's one instance, created (and listening) the first time a screen asks for back. */
        val shared: BrowserBack by lazy { BrowserBack() }
    }
}

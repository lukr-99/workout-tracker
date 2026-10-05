package com.lukr99.workout.ui.settings

/**
 * The shared Settings page rules from CodePrint's settings guide, without UI types so they have
 * unit tests: when to show jump navigation, and which section is current while scrolling.
 */
object SettingsScroll {
    /** Jump chips appear only with 4 or more visible sections. */
    const val MIN_SECTIONS_FOR_NAV = 4

    fun showNavigation(visibleSections: Int): Boolean = visibleSections >= MIN_SECTIONS_FOR_NAV

    /**
     * The current section: the last one whose top has passed [lineY] (80 dp below the top of the
     * scroll area), or the last section when the page is scrolled to the very bottom.
     *
     * [tops] holds each section's top relative to the top of the scroll area, null when it is
     * below the visible area. A section above the visible area counts as passed.
     */
    fun currentSection(tops: List<Int?>, firstVisible: Int, lineY: Int, atBottom: Boolean): Int {
        if (tops.isEmpty()) return 0
        if (atBottom) return tops.lastIndex
        var current = 0
        tops.forEachIndexed { index, top ->
            val passed = index < firstVisible || (top != null && top <= lineY)
            if (passed) current = index
        }
        return current
    }
}

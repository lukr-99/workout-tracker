package com.lukr99.workout.web.platform

// Whether the page runs as a home-screen app, a small flag kept in localStorage, and the safe-area
// insets that boot.js measures.

/** True when the page runs as an installed home-screen app rather than in a browser tab. */
internal fun isStandaloneApp(): Boolean = js(
    """!!((window.matchMedia && window.matchMedia('(display-mode: standalone)').matches) || window.navigator.standalone)""",
)

/** The stored text under [key], or null when there is none or the browser blocks storage. */
internal fun readLocalFlag(key: String): String? = js(
    """(function () { try { return window.localStorage.getItem(key); } catch (e) { return null; } })()""",
)

/** Stores [value] under [key]; does nothing when the browser blocks storage. */
internal fun writeLocalFlag(key: String, value: String): Unit = js(
    """(function () { try { window.localStorage.setItem(key, value); } catch (e) {} })()""",
)

/** One safe-area inset in CSS pixels: "top", "right", "bottom" or "left". */
internal fun safeAreaInset(side: String): Double = js(
    """(function () { var a = window.emberSafeArea ? window.emberSafeArea() : null; return a && a[side] ? Number(a[side]) : 0; })()""",
)

/** Calls [onChange] whenever boot.js reports new safe-area insets (rotation, resize). */
@JsFun("(onChange) => window.addEventListener('ember:safearea', () => onChange())")
internal external fun onSafeAreaChange(onChange: () -> Unit)

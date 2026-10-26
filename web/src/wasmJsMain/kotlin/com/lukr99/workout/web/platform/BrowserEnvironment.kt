package com.lukr99.workout.web.platform

import kotlin.js.Promise

// The few browser calls the platform files make beyond what the Kotlin/Wasm standard library wraps.
// Each is a single JavaScript expression, kept small enough to read at a glance.

/** True when the person has asked the system for less motion. */
internal fun prefersReducedMotion(): Boolean =
    js("!!(window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches)")

/** True on an iPhone or iPad, including an iPad that reports itself as a Mac. */
internal fun isIosDevice(): Boolean = js(
    """(function () {
        var ua = navigator.userAgent;
        return /iPad|iPhone|iPod/.test(ua) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);
    })()""",
)

/** [text] decomposed (NFD) and stripped of its combining marks, so an acute e becomes a plain e. */
internal fun removeCombiningMarks(text: String): String =
    js("text.normalize('NFD').replace(/\\p{M}+/gu, '')")

/**
 * Asks the browser to open the Spotify app through the `spotify:` link, and opens the Spotify web
 * player in a new tab when the page is still in front a moment later (no app took over).
 */
internal fun openSpotifyApp(): Unit = js(
    """(function () {
        var left = false;
        function onChange() { if (document.hidden) left = true; }
        document.addEventListener('visibilitychange', onChange);
        window.location.href = 'spotify:';
        setTimeout(function () {
            document.removeEventListener('visibilitychange', onChange);
            if (!left && !document.hidden) window.open('https://open.spotify.com', '_blank', 'noopener');
        }, 1500);
    })()""",
)

/** True when the browser can show a share sheet for text. */
internal fun canShareText(): Boolean = js("typeof navigator.share === 'function'")

/**
 * Shows the share sheet for [text]. Resolves to "shared", "cancelled" (the person closed the sheet)
 * or "failed" (the browser refused).
 */
internal fun shareTextNatively(title: String, text: String): Promise<JsString> = js(
    """(function () {
        try {
            return navigator.share({ title: title, text: text }).then(
                function () { return 'shared'; },
                function (e) { return e && e.name === 'AbortError' ? 'cancelled' : 'failed'; });
        } catch (e) { return Promise.resolve('failed'); }
    })()""",
)

/** True when the browser can copy text to the clipboard. */
internal fun canCopyText(): Boolean = js("!!(navigator.clipboard && navigator.clipboard.writeText)")

/** Copies [text] to the clipboard. Resolves to false when the browser refuses. */
internal fun copyTextToClipboard(text: String): Promise<JsBoolean> = js(
    """(function () {
        try {
            return navigator.clipboard.writeText(text).then(function () { return true; }, function () { return false; });
        } catch (e) { return Promise.resolve(false); }
    })()""",
)

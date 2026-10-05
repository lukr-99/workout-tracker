package com.lukr99.workout.domain

import java.net.URI

/**
 * Normalizes the optional guide or video link on an exercise. People paste links without a scheme
 * ("youtu.be/abc"), so a bare host gets `https://`. Anything that is not an http(s) address with a
 * host is rejected, so the app never hands an odd scheme to the browser.
 */
object GuideLink {

    /** The cleaned link, or null when [raw] is blank or not a usable web address. */
    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return null
        val withScheme = if (SCHEME.containsMatchIn(trimmed)) trimmed else "https://$trimmed"
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return null
        if (uri.host.isNullOrBlank() || !uri.host.contains('.')) return null
        return withScheme
    }

    /** True when [raw] is blank (no link) or normalizes to a web address. */
    fun isAcceptable(raw: String?): Boolean = raw.isNullOrBlank() || normalize(raw) != null

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")
}

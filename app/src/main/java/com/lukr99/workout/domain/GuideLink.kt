package com.lukr99.workout.domain

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
        if (withScheme.any { it.isWhitespace() || it in ILLEGAL }) return null
        val schemeEnd = withScheme.indexOf("://")
        val scheme = withScheme.substring(0, schemeEnd).lowercase()
        if (scheme != "http" && scheme != "https") return null
        val host = hostOf(withScheme.substring(schemeEnd + 3))
        if (host.isEmpty() || !host.contains('.') || !HOST.matches(host)) return null
        return withScheme
    }

    /** True when [raw] is blank (no link) or normalizes to a web address. */
    fun isAcceptable(raw: String?): Boolean = raw.isNullOrBlank() || normalize(raw) != null

    /** The host of an address after its `scheme://`: no user info, port, path, query or fragment. */
    private fun hostOf(rest: String): String {
        val authority = rest.substringBefore('/').substringBefore('?').substringBefore('#')
        return authority.substringAfterLast('@').substringBefore(':')
    }

    private val SCHEME = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*://")

    /** A DNS name or IPv4 address, as `java.net.URI` accepts for a server host. */
    private val HOST = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]*[a-zA-Z0-9])?)*\\.?$")

    /** Characters a URI may not contain unescaped. */
    private const val ILLEGAL = "\"<>\\^`{|}"
}

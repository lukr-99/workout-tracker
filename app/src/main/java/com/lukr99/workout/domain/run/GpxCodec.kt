package com.lukr99.workout.domain.run

import com.lukr99.workout.domain.newId
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Pure **GPX 1.1** codec for a run's trace — export a [Run] to a portable `.gpx` track and import a
 * `.gpx` back into a [Run] (`source = Imported`). No Android: writing is plain string building,
 * reading uses the JDK's bundled XML parser (present on the JVM and Android alike), so the whole
 * round-trip is unit-tested off-device. File IO is the Android layer's job — this only maps
 * `String <-> Run`, mirroring how [Polyline] owns the polyline codec.
 *
 * Wire shape: a single `<trk>` with one `<trkseg>` of `<trkpt lat lon>` points, each carrying an
 * optional `<ele>` (metres) and `<time>` (ISO-8601 UTC, absolute). On import the first point's time
 * anchors [Run.startedAtUtc] and every [TracePoint.t] becomes an offset from it, so an exported run
 * reimports to the same distance/duration/trace.
 */
object GpxCodec {

    const val MIME_TYPE = "application/gpx+xml"
    const val EXTENSION = "gpx"
    private const val NS = "http://www.topografix.com/GPX/1/1"
    private const val CREATOR = "Ember"

    /** Serialise [run]'s trace to a GPX 1.1 document. [name] labels the `<trk>` (falls back to notes). */
    fun encode(run: Run, name: String = run.notes.ifBlank { "Run" }): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""").append('\n')
        sb.append("""<gpx version="1.1" creator="$CREATOR" xmlns="$NS">""").append('\n')
        sb.append("  <metadata><time>").append(iso(run.startedAtUtc)).append("</time></metadata>\n")
        sb.append("  <trk>\n")
        sb.append("    <name>").append(escape(name)).append("</name>\n")
        sb.append("    <trkseg>\n")
        for (p in run.trace) {
            sb.append("      <trkpt lat=\"").append(num(p.lat)).append("\" lon=\"").append(num(p.lon)).append("\">")
            p.elevationM?.let { sb.append("<ele>").append(num(it)).append("</ele>") }
            sb.append("<time>").append(iso(run.startedAtUtc + p.t)).append("</time>")
            sb.append("</trkpt>\n")
        }
        sb.append("    </trkseg>\n")
        sb.append("  </trk>\n")
        sb.append("</gpx>\n")
        return sb.toString()
    }

    /**
     * Parse a GPX document into a [Run] (`source = Imported`), deriving distance/duration/pace/
     * elevation from the trace. Returns null if the document has no usable `<trkpt>`s. Namespace-
     * agnostic (accepts GPX 1.0/1.1 or vendor exports); missing `<time>`s degrade gracefully (points
     * keep `t = 0`, so distance survives even when timing is absent).
     */
    fun decode(gpx: String, id: String = newId()): Run? {
        val doc = runCatching {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                // Harden against XXE — this parses untrusted imported files.
                runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
                runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
                runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            }
            factory.newDocumentBuilder().parse(ByteArrayInputStream(gpx.toByteArray(Charsets.UTF_8)))
        }.getOrNull() ?: return null

        val nodes = doc.getElementsByTagName("trkpt")
        if (nodes.length == 0) return null

        data class Raw(val lat: Double, val lon: Double, val ele: Double?, val timeMs: Long?)
        val raw = ArrayList<Raw>(nodes.length)
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as? Element ?: continue
            val lat = el.getAttribute("lat").toDoubleOrNull() ?: continue
            val lon = el.getAttribute("lon").toDoubleOrNull() ?: continue
            val ele = childText(el, "ele")?.toDoubleOrNull()
            val timeMs = childText(el, "time")?.let { parseTime(it) }
            raw += Raw(lat, lon, ele, timeMs)
        }
        if (raw.isEmpty()) return null

        val anchorMs = raw.firstOrNull { it.timeMs != null }?.timeMs ?: 0L
        val trace = raw.map { r ->
            TracePoint(
                t = r.timeMs?.let { it - anchorMs }?.coerceAtLeast(0L) ?: 0L,
                lat = r.lat,
                lon = r.lon,
                elevationM = r.ele,
            )
        }
        val distance = Pace.traceDistanceMeters(trace)
        val durationSec = (trace.last().t / 1000).coerceAtLeast(0)
        val name = firstChildText(doc.documentElement, "name")
        return Run(
            id = id,
            startedAtUtc = anchorMs,
            durationSeconds = durationSec,
            movingSeconds = durationSec,
            distanceMeters = distance,
            avgPaceSecPerKm = Pace.paceSecPerKm(distance, durationSec.toDouble()),
            elevationGainM = Pace.elevationGainMeters(trace),
            source = RunSource.Imported,
            notes = name?.takeIf { it.isNotBlank() } ?: "Imported run",
            trace = trace,
        )
    }

    // --- helpers -------------------------------------------------------------------------------

    private fun childText(parent: Element, tag: String): String? {
        val children = parent.getElementsByTagName(tag)
        if (children.length == 0) return null
        return children.item(0).textContent?.trim()
    }

    /** First descendant [tag] text under [root] (used for the track name, which may be nested). */
    private fun firstChildText(root: Element?, tag: String): String? {
        root ?: return null
        val children = root.getElementsByTagName(tag)
        if (children.length == 0) return null
        return children.item(0).textContent?.trim()
    }

    private fun parseTime(text: String): Long? = runCatching {
        OffsetDateTime.parse(text).toInstant().toEpochMilli()
    }.recoverCatching {
        Instant.parse(text).toEpochMilli()
    }.getOrNull()

    private fun iso(epochMs: Long): String = Instant.ofEpochMilli(epochMs).toString()

    private fun num(v: Double): String = String.format(Locale.US, "%.6f", v)

    private fun escape(s: String): String = s
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

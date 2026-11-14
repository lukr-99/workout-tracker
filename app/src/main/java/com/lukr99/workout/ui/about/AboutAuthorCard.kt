package com.lukr99.workout.ui.about

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lukr99.workout.ui.AppTags
import com.lukr99.workout.ui.components.rememberReduceMotion
import com.lukr99.workout.ui.theme.BarlowCondensed
import com.lukr99.workout.ui.theme.EmberTheme

/**
 * The page's centrepiece: who made Ember. An ember-orange frame with a slow breathing glow, the
 * initials on a drawn weight plate, the name large, the invitation to give feedback, then Instagram,
 * GitHub and the issues page as buttons.
 */
@Composable
fun AboutAuthorCard(modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(24.dp)
    val glow = rememberGlow()
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(colors.surfaceRaised, colors.surface)))
            .drawBehind {
                val reach = size.width * 0.85f
                val top = Offset(size.width / 2f, 0f)
                drawCircle(
                    Brush.radialGradient(listOf(colors.primary.copy(alpha = glow), Color.Transparent), center = top, radius = reach),
                    radius = reach,
                    center = top,
                )
            }
            .border(2.dp, Brush.linearGradient(listOf(colors.primary, colors.warning, colors.primary.copy(alpha = 0.35f))), shape)
            .testTag(AppTags.ABOUT_AUTHOR)
            .padding(horizontal = 20.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PlateMonogram(AuthorLinks.initials())
        Text(
            "MADE BY",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.4.sp,
            color = colors.primaryText,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            AuthorLinks.NAME,
            style = MaterialTheme.typography.displayLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 2.dp).semantics { heading() },
        )
        Text(
            "I built Ember for my own training, and I keep making it better.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        PlateDivider(Modifier.padding(vertical = 20.dp))

        Text(
            "Tell me what you think",
            style = MaterialTheme.typography.titleLarge,
            color = colors.textPrimary,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "Feedback is welcome: what works, what does not, what is missing.",
            style = MaterialTheme.typography.bodyLarge,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 18.dp),
        )

        val uri = LocalUriHandler.current
        // A phone with no browser cannot open a link: the page simply stays as it is.
        fun open(url: String) { runCatching { uri.openUri(url) } }
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val instagram = @Composable { m: Modifier ->
                ProfileButton("Instagram", "@${AuthorLinks.INSTAGRAM_HANDLE}", filled = true, glyph = { InstagramGlyph(it) },
                    onClick = { open(AuthorLinks.INSTAGRAM_URL) }, modifier = m.testTag(AppTags.ABOUT_INSTAGRAM))
            }
            val github = @Composable { m: Modifier ->
                ProfileButton("GitHub", AuthorLinks.GITHUB_HANDLE, filled = false, glyph = { CodeGlyph(it) },
                    onClick = { open(AuthorLinks.GITHUB_URL) }, modifier = m.testTag(AppTags.ABOUT_GITHUB))
            }
            if (maxWidth >= SIDE_BY_SIDE) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    instagram(Modifier.weight(1f))
                    github(Modifier.weight(1f))
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    instagram(Modifier.fillMaxWidth())
                    github(Modifier.fillMaxWidth())
                }
            }
        }
        IssuesLink(onClick = { open(AuthorLinks.ISSUES_URL) }, modifier = Modifier.padding(top = 10.dp))
    }
}

/** A profile as a button: a glyph, the site, the handle, and an arrow that says it opens outside. */
@Composable
private fun ProfileButton(
    site: String,
    handle: String,
    filled: Boolean,
    glyph: @Composable (Color) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    val ink = if (filled) colors.onPrimary else colors.textPrimary
    Row(
        modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (filled) colors.primary else colors.surface)
            .then(if (filled) Modifier else Modifier.border(1.5.dp, colors.primary.copy(alpha = 0.6f), shape))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$site, $handle. Opens in the browser." }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        glyph(ink)
        Column(Modifier.weight(1f)) {
            Text(site, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ink)
            Text(handle, style = MaterialTheme.typography.labelLarge, color = if (filled) ink.copy(alpha = 0.8f) else colors.textSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = ink, modifier = Modifier.size(18.dp))
    }
}

/** The link to Ember's issues page, quieter than the two profiles. */
@Composable
private fun IssuesLink(onClick: () -> Unit, modifier: Modifier) {
    val colors = EmberTheme.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(colors.primarySoft)
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(AppTags.ABOUT_ISSUES)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = colors.primaryText, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text("Report a bug or an idea", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
            Text("Ember's issues page on GitHub", style = MaterialTheme.typography.labelLarge, color = colors.textSecondary)
        }
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
    }
}

/** The author's initials on a drawn weight plate: an orange rim, an inner ring and the centre hole. */
@Composable
private fun PlateMonogram(initials: String) {
    val colors = EmberTheme.colors
    Box(Modifier.size(92.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(92.dp)) {
            val r = size.minDimension / 2f
            drawCircle(colors.surface, r)
            drawCircle(colors.primary, r - 2.dp.toPx(), style = Stroke(5.dp.toPx()))
            drawCircle(colors.primary.copy(alpha = 0.35f), r - 12.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            // Four grip slots around the rim, like a real plate.
            val slot = Size(10.dp.toPx(), 3.dp.toPx())
            listOf(0f, 90f, 180f, 270f).forEach { angle ->
                rotate(angle) {
                    drawRoundRect(
                        colors.primary.copy(alpha = 0.55f),
                        topLeft = Offset(center.x - slot.width / 2f, 9.dp.toPx()),
                        size = slot,
                        cornerRadius = CornerRadius(slot.height),
                    )
                }
            }
        }
        Text(
            initials,
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = BarlowCondensed, fontSize = 34.sp, lineHeight = 36.sp),
            color = colors.primaryText,
        )
    }
}

/** A short bar with two small plates on each end: the card's divider. */
@Composable
private fun PlateDivider(modifier: Modifier = Modifier) {
    val colors = EmberTheme.colors
    Row(modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 5.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary))
        Box(Modifier.padding(start = 2.dp).size(width = 4.dp, height = 12.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary.copy(alpha = 0.6f)))
        Box(Modifier.padding(horizontal = 4.dp).size(width = 72.dp, height = 3.dp).clip(RoundedCornerShape(2.dp)).background(colors.border))
        Box(Modifier.size(width = 4.dp, height = 12.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary.copy(alpha = 0.6f)))
        Box(Modifier.padding(start = 2.dp).size(width = 5.dp, height = 18.dp).clip(RoundedCornerShape(2.dp)).background(colors.primary))
    }
}

/** A rounded square with a lens and a dot: a plain camera mark for Instagram. */
@Composable
private fun InstagramGlyph(ink: Color) {
    Canvas(Modifier.size(24.dp).clearAndSetSemantics { }) {
        val w = 2.dp.toPx()
        drawRoundRect(ink, topLeft = Offset(w / 2, w / 2), size = Size(size.width - w, size.height - w), cornerRadius = CornerRadius(6.dp.toPx()), style = Stroke(w))
        drawCircle(ink, radius = size.minDimension * 0.22f, style = Stroke(w))
        drawCircle(ink, radius = 1.4.dp.toPx(), center = Offset(size.width * 0.76f, size.height * 0.24f))
    }
}

/** Angle brackets around a slash, for code on GitHub. */
@Composable
private fun CodeGlyph(ink: Color) {
    Canvas(Modifier.size(24.dp).clearAndSetSemantics { }) {
        val stroke = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val h = size.height
        val x = size.width
        drawPath(Path().apply { moveTo(x * 0.30f, h * 0.24f); lineTo(x * 0.06f, h * 0.5f); lineTo(x * 0.30f, h * 0.76f) }, ink, style = stroke)
        drawPath(Path().apply { moveTo(x * 0.70f, h * 0.24f); lineTo(x * 0.94f, h * 0.5f); lineTo(x * 0.70f, h * 0.76f) }, ink, style = stroke)
        drawLine(ink, Offset(x * 0.57f, h * 0.18f), Offset(x * 0.43f, h * 0.82f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    }
}

/** The glow's strength: it breathes slowly between two levels, or stays at the middle with reduce motion. */
@Composable
private fun rememberGlow(): Float {
    if (rememberReduceMotion()) return 0.22f
    val transition = rememberInfiniteTransition(label = "about glow")
    val glow by transition.animateFloat(0.16f, 0.28f, infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    return glow
}

private val SIDE_BY_SIDE = 360.dp

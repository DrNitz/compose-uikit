// From Uiverse.io by chase2k25 - https://uiverse.io/chase2k25/nice-fly-98
// MIT License, Copyright (c) chase2k25 (chandu.exe)

package com.example.composeuikit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Neumorphic analog clock: a soft-shaded face with recessed markers, embossed numerals and
 * three hands.
 *
 * The dial is drawn to the design's own proportions, so it stays correct at any size — the
 * geometry is derived from the box it is given rather than hard-coded. It paints its own
 * background, which neumorphism needs: the raised and recessed shadows only read as depth
 * against the surface they sit on, so the component fills its bounds with the base colour.
 *
 * The hands are driven by the device clock. The second hand steps once per second, matching
 * the design's 60-step sweep; the minute and hour hands sweep continuously between ticks.
 *
 * ```kotlin
 * ComposeKitClock(size = 300.dp)
 * ```
 *
 * @param size diameter of the dial. The component is laid out larger than this to leave room
 *   for the outer shadows; a `Modifier` that sets its own size takes precedence.
 * @param showSeconds draw the second hand.
 * @param darkTheme use the dark palette; defaults to the system setting.
 */
@Composable
fun ComposeKitClock(
    modifier: Modifier = Modifier,
    size: Dp = 300.dp,
    showSeconds: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val palette = remember(darkTheme) { clockPalette(darkTheme) }

    // One tick per second, aligned to the second boundary so the step lands where the eye
    // expects it. Integer seconds keep the second hand discrete.
    var secondsOfDay by remember { mutableIntStateOf(secondsOfDayNow()) }
    LaunchedEffect(Unit) {
        while (true) {
            secondsOfDay = secondsOfDayNow()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .size(size + ShadowRoom * 2)
            .background(palette.base),
        contentAlignment = Alignment.Center,
    ) {
        // The dial shrinks to leave the shadow room inside whatever box we were given.
        val shortest = min(maxWidth.value, maxHeight.value)
        val faceRadius = (shortest * (150f / DesignTotal)).dp
        val unit = faceRadius / 150.dp

        val measurer = rememberTextMeasurer()
        val numberStyle = remember(unit, palette) {
            TextStyle(
                color = palette.number,
                fontSize = (24f * unit).sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.SansSerif,
                textAlign = TextAlign.Center,
            )
        }
        val numbers = remember(measurer, numberStyle) {
            (1..12).map { measurer.measure(AnnotatedString(it.toString()), numberStyle) }
        }

        Canvas(Modifier.fillMaxSize()) {
            val canvas = this.size
            drawNeumorphicClock(
                center = Offset(canvas.width / 2f, canvas.height / 2f),
                unit = faceRadius.toPx() / 150f,
                palette = palette,
                numbers = numbers,
                secondsOfDay = secondsOfDay,
                showSeconds = showSeconds,
            )
        }
    }
}

/**
 * Compact readout for toolbars and status strips: one line, seconds on.
 *
 * ```kotlin
 * ComposeKitClockPill()
 * ```
 */
@Composable
fun ComposeKitClockPill(
    modifier: Modifier = Modifier,
    use24Hour: Boolean = true,
) {
    val formatter = remember(use24Hour) {
        SimpleDateFormat(if (use24Hour) "HH:mm:ss" else "hh:mm:ss a", Locale.getDefault())
    }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = Date()
            delay(1_000L)
        }
    }
    Text(
        text = formatter.format(now),
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

/* ─────────────────────────────── geometry ─────────────────────────────── */

/** Design units: a 150-unit face radius, with room around it for the outer shadows. */
private const val DesignFaceRadius = 150f
private const val DesignTotal = 380f
private val ShadowRoom = 40.dp

private fun secondsOfDayNow(): Int {
    val now = Calendar.getInstance()
    return now.get(Calendar.HOUR_OF_DAY) * 3600 +
        now.get(Calendar.MINUTE) * 60 +
        now.get(Calendar.SECOND)
}

private fun DrawScope.drawNeumorphicClock(
    center: Offset,
    unit: Float,
    palette: ClockPalette,
    numbers: List<TextLayoutResult>,
    secondsOfDay: Int,
    showSeconds: Boolean,
) {
    val faceRadius = DesignFaceRadius * unit
    val innerRingRadius = 138f * unit
    val markerRadius = 132f * unit
    val markerDotRadius = 3f * unit
    val markerDotRadiusLarge = 4f * unit
    val numberRadius = 101f * unit
    val outerShadowOffset = 12f * unit
    val outerShadowBlur = 24f * unit
    val innerShadowWidth = 16f * unit
    val handCorner = 10f * unit

    fun pointAt(angleDeg: Float, radius: Float): Offset {
        val radians = Math.toRadians(angleDeg.toDouble())
        return Offset(
            center.x + radius * sin(radians).toFloat(),
            center.y - radius * cos(radians).toFloat(),
        )
    }

    // Raised face: a dark shadow down-right and a light one up-left.
    drawSoftShadow(
        center = center + Offset(outerShadowOffset, outerShadowOffset),
        radius = faceRadius,
        blur = outerShadowBlur,
        color = palette.shadowDark,
    )
    drawSoftShadow(
        center = center - Offset(outerShadowOffset, outerShadowOffset),
        radius = faceRadius,
        blur = outerShadowBlur,
        color = palette.shadowLight,
    )

    drawCircle(color = palette.base, radius = faceRadius, center = center)

    // Recessed inner ring: dark along the top-left edge, light along the bottom-right.
    drawInsetShading(
        center = center,
        radius = innerRingRadius,
        width = innerShadowWidth,
        dark = palette.shadowDark,
        light = palette.shadowLight,
    )

    // Markers: same colour as the face, so only their recess reads.
    for (marker in 1..12) {
        val angle = marker * 30f
        val isQuarter = marker % 3 == 0
        val dotRadius = if (isQuarter) markerDotRadiusLarge else markerDotRadius
        drawInsetShading(
            center = pointAt(angle, markerRadius),
            radius = dotRadius,
            width = 2f * unit,
            dark = palette.shadowDark,
            light = palette.shadowLight,
        )
    }

    // Numerals: embossed with a light shadow down-right and a dark one up-left.
    numbers.forEachIndexed { index, layout ->
        val position = pointAt((index + 1) * 30f, numberRadius)
        val topLeft = Offset(
            position.x - layout.size.width / 2f,
            position.y - layout.size.height / 2f,
        )
        drawText(
            textLayoutResult = layout,
            topLeft = topLeft,
            shadow = Shadow(palette.shadowLight, Offset(unit, unit), unit),
        )
        drawText(
            textLayoutResult = layout,
            topLeft = topLeft,
            shadow = Shadow(palette.shadowDark, Offset(-unit, -unit), unit),
        )
        drawText(textLayoutResult = layout, topLeft = topLeft)
    }

    val seconds = secondsOfDay % 60
    val minutes = secondsOfDay / 60 % 60
    val hours = secondsOfDay / 3600 % 12

    drawHand(
        center = center,
        width = 8f * unit,
        length = 50f * unit,
        corner = handCorner,
        color = palette.hourHand,
        angle = hours * 30f + minutes * 0.5f,
    )
    drawHand(
        center = center,
        width = 6f * unit,
        length = 80f * unit,
        corner = handCorner,
        color = palette.minuteHand,
        angle = minutes * 6f + seconds * 0.1f,
    )
    if (showSeconds) {
        drawHand(
            center = center,
            width = 3f * unit,
            length = 100f * unit,
            corner = handCorner,
            color = palette.secondHand,
            angle = seconds * 6f,
        )
    }

    // Centre pin, recessed, sitting over the hands.
    val pinRadius = 10f * unit
    drawCircle(color = palette.base, radius = pinRadius, center = center)
    drawInsetShading(
        center = center,
        radius = pinRadius,
        width = 6f * unit,
        dark = palette.shadowDark,
        light = palette.shadowLight,
    )
    drawCircle(color = palette.pinDot, radius = 3f * unit, center = center)
}

/**
 * A blurred disc of one colour. The design's neumorphic shadows are a blurred circle behind the
 * face, so this fakes the blur with a radial gradient that holds solid to the edge and then
 * falls away — no framework mask filter, which hardware-accelerated canvases do not support.
 */
private fun DrawScope.drawSoftShadow(center: Offset, radius: Float, blur: Float, color: Color) {
    val extent = radius + blur / 2f
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to color,
                (radius / extent) to color,
                1f to color.copy(alpha = 0f),
            ),
            center = center,
            radius = extent,
        ),
        radius = extent,
        center = center,
    )
}

/**
 * Inset shading around a circle: a sweep gradient stroked just inside the edge, dark where the
 * light falls away (top-left) and light where it lands (bottom-right).
 */
private fun DrawScope.drawInsetShading(
    center: Offset,
    radius: Float,
    width: Float,
    dark: Color,
    light: Color,
) {
    val outline = Path().apply {
        addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }
    clipPath(outline) {
        drawCircle(
            brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.000f to Color.Transparent,
                    0.125f to light, // 45deg, bottom right
                    0.300f to Color.Transparent,
                    0.500f to Color.Transparent,
                    0.625f to dark, // 225deg, top left
                    0.800f to Color.Transparent,
                    1.000f to Color.Transparent,
                ),
                center = center,
            ),
            radius = radius - width / 2f,
            center = center,
            style = Stroke(width = width),
        )
    }
}

/** A hand: a bar whose bottom sits on the dial centre, rounded at the tip, rotated in place. */
private fun DrawScope.drawHand(
    center: Offset,
    width: Float,
    length: Float,
    corner: Float,
    color: Color,
    angle: Float,
) {
    rotate(degrees = angle, pivot = center) {
        val tip = min(corner, width / 2f)
        val path = Path().apply {
            addRoundRect(
                RoundRect(
                    rect = Rect(center.x - width / 2f, center.y - length, center.x + width / 2f, center.y),
                    topLeft = CornerRadius(tip),
                    topRight = CornerRadius(tip),
                ),
            )
        }
        drawPath(path, color)
    }
}

/* ──────────────────────────────── palette ─────────────────────────────── */

private data class ClockPalette(
    val base: Color,
    val shadowDark: Color,
    val shadowLight: Color,
    val number: Color,
    val hourHand: Color,
    val minuteHand: Color,
    val secondHand: Color,
    val pinDot: Color,
)

private fun clockPalette(dark: Boolean): ClockPalette = if (dark) {
    ClockPalette(
        base = Color(0xFF2B2F38),
        shadowDark = Color(0xFF1B1E24),
        shadowLight = Color(0xFF3D434F),
        number = Color(0xFF7E8899),
        hourHand = Color(0xFFE8ECF3),
        minuteHand = Color(0xFFB9C2D0),
        secondHand = Color(0xFFFF6B6B),
        pinDot = Color(0xFFE8ECF3),
    )
} else {
    ClockPalette(
        base = Color(0xFFE0E5EC),
        shadowDark = Color(0xFFA3B1C6),
        shadowLight = Color(0xFFFFFFFF),
        number = Color(0xFFB8C1D1),
        hourHand = Color(0xFF4A5463),
        minuteHand = Color(0xFF7A8A9E),
        secondHand = Color(0xFFE65E5E),
        pinDot = Color(0xFF4A5463),
    )
}

@Preview(showBackground = true, widthDp = 400, heightDp = 460)
@Composable
private fun ComposeKitClockPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ComposeKitClock(size = 280.dp)
        ComposeKitClockPill()
    }
}

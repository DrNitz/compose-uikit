package com.example.composeuikit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp as lerpDp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * A story card that scrolls in place and grows into a full reader.
 *
 * Closed, it is a small page of prose under a soft edge fade with an "Open" pill. Tapping it
 * expands the card's own rectangle into a reading panel — the panel is laid out at its final
 * size from the start and only a rounded-rect clip grows, so the text never re-wraps mid-flight.
 * The reader opens on the passage the card was scrolled to, flashes that passage once, and keeps
 * your place when you step the type size up or down.
 *
 * Everything is measured from the container, so the same component works in a 400dp column or
 * full screen. Dismiss with the close button, a tap outside the panel, or the system back gesture.
 *
 * Approximations, because Compose has no direct equivalent: the liquid-glass refraction is a
 * translucent tint over a blurred backdrop rather than a real displacement map, and the drop cap
 * is a raised initial because text cannot wrap around a floating letter.
 *
 * ```kotlin
 * ComposeKitReaderCard(
 *     title = "The Clockmaker's Apprentice",
 *     chapterLabel = "Chapter One",
 *     excerpt = excerptParagraphs,
 *     chapter = chapterParagraphs,
 *     resumeAfter = "On the bench lay a watch with no face",
 * )
 * ```
 *
 * @param title heading on both the card and the reader.
 * @param excerpt paragraphs shown scrolling inside the card.
 * @param chapter paragraphs of the reader body.
 * @param chapterLabel small centred label above the body, e.g. `"Chapter One"`.
 * @param meta optional line under the reader title, e.g. `"Demo content"`.
 * @param resumeAfter snippet from `chapter` the card is showing. The reader opens scrolled to that
 *   paragraph and flashes the highlight from the snippet to the paragraph's end. Ignored when null.
 * @param darkTheme use the dark palette; defaults to the system setting.
 * @param sceneHeight height of the whole scene when the caller's modifier does not set one.
 * @param onOpenChange called with `true` once the reader is open, `false` when it closes.
 */
@Composable
fun ComposeKitReaderCard(
    title: String,
    excerpt: List<String>,
    chapter: List<String>,
    modifier: Modifier = Modifier,
    chapterLabel: String = "Chapter One",
    meta: String? = null,
    resumeAfter: String? = null,
    darkTheme: Boolean = isSystemInDarkTheme(),
    sceneHeight: Dp = 640.dp,
    onOpenChange: (Boolean) -> Unit = {},
) {
    val palette = remember(darkTheme) { readerCardPalette(darkTheme) }
    var open by remember { mutableStateOf(false) }

    val morph by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = spring(
            dampingRatio = if (open) 0.78f else 1f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "reader-morph",
    )

    val cardScroll = rememberScrollState()
    val proseScroll = rememberScrollState()
    var sizeIndex by remember { mutableIntStateOf(START_PROSE_SIZE) }
    val proseSize = PROSE_SIZES[sizeIndex].sp
    val flash = remember { Animatable(0f) }

    val resumeIndex = remember(chapter, resumeAfter) {
        if (resumeAfter.isNullOrBlank()) -1
        else chapter.indexOfFirst { it.contains(resumeAfter, ignoreCase = true) }
    }

    BackHandler(enabled = open) { open = false }
    LaunchedEffect(open) { onOpenChange(open) }

    LaunchedEffect(open) {
        if (!open) return@LaunchedEffect
        delay(360) // let the morph settle before moving the text
        if (resumeIndex > 0 && proseScroll.maxValue > 0) {
            val ratio = resumeIndex.toFloat() / chapter.size
            proseScroll.scrollTo((proseScroll.maxValue * ratio).roundToInt())
        }
        flash.snapTo(1f)
        flash.animateTo(0f, animationSpec = tween(durationMillis = 2_400, easing = LinearEasing))
    }

    // Step the type size without losing the reading position: ratio out, ratio back in.
    var pendingRatio by remember { mutableStateOf<Float?>(null) }
    LaunchedEffect(sizeIndex, pendingRatio) {
        val ratio = pendingRatio ?: return@LaunchedEffect
        pendingRatio = null
        if (proseScroll.maxValue > 0) {
            proseScroll.scrollTo((proseScroll.maxValue * ratio).roundToInt())
        }
    }
    val stepSize: (Int) -> Unit = { delta ->
        pendingRatio =
            if (proseScroll.maxValue > 0) proseScroll.value.toFloat() / proseScroll.maxValue else null
        sizeIndex = (sizeIndex + delta).coerceIn(0, PROSE_SIZES.lastIndex)
    }

    BoxWithConstraints(
        modifier = modifier
            .height(sceneHeight)
            .clip(RoundedCornerShape(28.dp))
            .background(palette.page),
    ) {
        val sceneW = maxWidth
        val sceneH = maxHeight
        val narrow = sceneW < 560.dp
        val inset = if (narrow) 8.dp else 24.dp

        val panelWidth = minOf(680.dp, sceneW - inset * 2)
        val panelHeight = minOf(860.dp, sceneH - inset * 2)
        val panelRadius = if (narrow) 32.dp else 40.dp
        val cardWidth = minOf(289.dp, sceneW)
        val cardHeight = minOf(290.dp, sceneH)
        val prosePadding = (panelWidth * 0.08f).coerceIn(22.dp, 64.dp)

        val shapeW = lerpDp(cardWidth, panelWidth, morph)
        val shapeH = lerpDp(cardHeight, panelHeight, morph)
        val shapeRadius = lerpDp(40.dp, panelRadius, morph)

        val cardAlpha = (1f - morph / 0.55f).coerceIn(0f, 1f)
        val readerAlpha = ((morph - 0.12f) / 0.75f).coerceIn(0f, 1f)
        val ambientBlur = 22.dp * morph

        Box(Modifier.fillMaxSize()) {
            // Ambient light. Blurred while the reader is open so the panel reads as glass over it.
            AmbientOrbs(
                palette = palette,
                drift = 1f - morph,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (ambientBlur > 0.5.dp) Modifier.blur(ambientBlur) else Modifier),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(cardWidth, cardHeight)
                    .alpha(cardAlpha),
            ) {
                CardFace(
                    title = title,
                    excerpt = excerpt,
                    palette = palette,
                    scroll = cardScroll,
                    onOpen = { open = true },
                    modifier = Modifier.fillMaxSize(),
                )
            }

            if (morph > 0.001f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.scrim.copy(alpha = palette.scrim.alpha * morph))
                        .clickable { open = false },
                )
            }

            // The reader. Fixed panel size; only this rounded-rect clip grows, from the card's
            // rectangle to the panel's, so the prose never lays out at an intermediate width.
            //
            // Composed only while it is showing. Left in the tree while closed, this full-size
            // sibling would sit above the card and win hit-testing, swallowing every tap.
            if (morph > 0.001f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            val w = shapeW.toPx()
                            val h = shapeH.toPx()
                            val r = shapeRadius.toPx()
                            val clip = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        (size.width - w) / 2f,
                                        (size.height - h) / 2f,
                                        (size.width + w) / 2f,
                                        (size.height + h) / 2f,
                                        r,
                                        r,
                                    ),
                                )
                            }
                            clipPath(clip) { this@drawWithContent.drawContent() }
                        },
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(panelWidth, panelHeight)
                            .clip(RoundedCornerShape(panelRadius))
                            .background(palette.panelTint)
                            .border(1.dp, palette.rim, RoundedCornerShape(panelRadius))
                            .alpha(readerAlpha),
                    ) {
                        ReaderPanel(
                            palette = palette,
                            title = title,
                            meta = meta,
                            chapterLabel = chapterLabel,
                            chapter = chapter,
                            resumeAfter = resumeAfter,
                            proseSize = proseSize,
                            prosePadding = prosePadding,
                            canSmaller = sizeIndex > 0,
                            canLarger = sizeIndex < PROSE_SIZES.lastIndex,
                            onSmaller = { stepSize(-1) },
                            onLarger = { stepSize(1) },
                            onClose = { open = false },
                            scroll = proseScroll,
                            flash = flash.value,
                        )
                    }
                }
            }
        }
    }
}

/* ─────────────────────────────── card ─────────────────────────────── */

@Composable
private fun CardFace(
    title: String,
    excerpt: List<String>,
    palette: ReaderCardPalette,
    scroll: ScrollState,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(40.dp))
            .background(palette.card)
            .border(1.dp, palette.rim, RoundedCornerShape(40.dp))
            .clickable(onClick = onOpen),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(start = 23.dp, end = 23.dp, top = 72.dp, bottom = 118.dp),
        ) {
            excerpt.forEachIndexed { index, paragraph ->
                Text(
                    text = if (index == 0) paragraph else "\u2003$paragraph",
                    color = palette.inkOnCard,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.9.sp,
                    lineHeight = 23.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        EdgeFade(palette.card, Edge.Top, Modifier.align(Alignment.TopCenter), 104.dp)
        EdgeFade(palette.card, Edge.Bottom, Modifier.align(Alignment.BottomCenter), 96.dp)

        Text(
            text = title,
            color = palette.title,
            fontFamily = FontFamily.Serif,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 24.dp),
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .clip(CircleShape)
                .background(palette.glassTint)
                .border(1.dp, palette.rim, CircleShape)
                .clickable(onClick = onOpen)
                .height(40.dp)
                .padding(start = 20.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "Open",
                color = palette.glassInk,
                fontFamily = FontFamily.Serif,
                fontSize = 19.5.sp,
            )
            ExpandGlyph(tint = palette.glassInk.copy(alpha = 0.75f))
        }
    }
}

private enum class Edge { Top, Bottom }

@Composable
private fun EdgeFade(color: Color, edge: Edge, modifier: Modifier, height: Dp) {
    val brush = if (edge == Edge.Top) {
        Brush.verticalGradient(
            0f to color,
            0.64f to color.copy(alpha = 0.98f),
            0.82f to color.copy(alpha = 0.72f),
            1f to Color.Transparent,
        )
    } else {
        Brush.verticalGradient(
            0f to Color.Transparent,
            0.6f to color.copy(alpha = 0.70f),
            1f to color.copy(alpha = 0.92f),
        )
    }
    Box(modifier.fillMaxWidth().height(height).background(brush))
}

/* ────────────────────────────── reader ────────────────────────────── */

@Composable
private fun ReaderPanel(
    palette: ReaderCardPalette,
    title: String,
    meta: String?,
    chapterLabel: String,
    chapter: List<String>,
    resumeAfter: String?,
    proseSize: TextUnit,
    prosePadding: Dp,
    canSmaller: Boolean,
    canLarger: Boolean,
    onSmaller: () -> Unit,
    onLarger: () -> Unit,
    onClose: () -> Unit,
    scroll: ScrollState,
    flash: Float,
) {
    val resumeIndex = remember(chapter, resumeAfter) {
        if (resumeAfter.isNullOrBlank()) -1
        else chapter.indexOfFirst { it.contains(resumeAfter, ignoreCase = true) }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = 118.dp, bottom = 132.dp, start = prosePadding, end = prosePadding),
            verticalArrangement = Arrangement.spacedBy((proseSize.value * 0.85f).dp),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally),
            ) {
                Text(
                    text = chapterLabel.uppercase(),
                    color = palette.muted,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 11.sp,
                    letterSpacing = 2.4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = (proseSize.value * 1.3f).dp),
                )

                chapter.forEachIndexed { index, paragraph ->
                    when {
                        index == 0 -> DropCapParagraph(
                            text = paragraph,
                            proseSize = proseSize,
                            palette = palette,
                        )

                        index == resumeIndex -> ResumeParagraph(
                            paragraph = paragraph,
                            marker = resumeAfter,
                            proseSize = proseSize,
                            palette = palette,
                            flash = flash,
                        )

                        else -> ProseParagraph(
                            text = paragraph,
                            proseSize = proseSize,
                            palette = palette,
                        )
                    }
                }

                Text(
                    text = "\u2766",
                    color = palette.muted,
                    fontSize = proseSize,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = (proseSize.value * 1.2f).dp),
                )
            }
        }

        EdgeFade(palette.panelSolid, Edge.Top, Modifier.align(Alignment.TopCenter), 112.dp)
        EdgeFade(palette.panelSolid, Edge.Bottom, Modifier.align(Alignment.BottomCenter), 120.dp)

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(start = 72.dp, end = 72.dp, top = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = palette.title,
                fontFamily = FontFamily.Serif,
                fontSize = 21.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
            )
            if (meta != null) {
                Text(
                    text = meta,
                    color = palette.muted,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 12.5.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 20.dp)
                .size(42.dp)
                .clip(CircleShape)
                .background(palette.glassTint)
                .border(1.dp, palette.rim, CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            CloseGlyph(tint = palette.glassInk)
        }

        ReaderToolbar(
            palette = palette,
            scroll = scroll,
            canSmaller = canSmaller,
            canLarger = canLarger,
            onSmaller = onSmaller,
            onLarger = onLarger,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
        )
    }
}

@Composable
private fun ReaderToolbar(
    palette: ReaderCardPalette,
    scroll: ScrollState,
    canSmaller: Boolean,
    canLarger: Boolean,
    onSmaller: () -> Unit,
    onLarger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress by remember {
        derivedStateOf {
            if (scroll.maxValue > 0) scroll.value.toFloat() / scroll.maxValue else 0f
        }
    }

    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.glassTint)
            .border(1.dp, palette.rim, RoundedCornerShape(24.dp))
            .padding(start = 6.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SizeButton("A", 14.sp, canSmaller, palette, onSmaller)
        SizeButton("A", 20.sp, canLarger, palette, onLarger)

        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(1.dp).height(20.dp).background(palette.track))
        Spacer(Modifier.width(10.dp))

        Box(
            Modifier
                .width(76.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(palette.track),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(4.dp)
                    .background(palette.accent),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = "${(progress.coerceIn(0f, 1f) * 100).roundToInt()}%",
            color = palette.glassInk,
            fontFamily = FontFamily.SansSerif,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SizeButton(
    label: String,
    fontSize: TextUnit,
    enabled: Boolean,
    palette: ReaderCardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = palette.glassInk.copy(alpha = if (enabled) 1f else 0.35f),
            fontFamily = FontFamily.Serif,
            fontSize = fontSize,
        )
    }
}

/* ───────────────────────────── typography ─────────────────────────── */

@Composable
private fun ProseParagraph(text: String, proseSize: TextUnit, palette: ReaderCardPalette) {
    Text(
        text = text,
        color = palette.ink,
        fontFamily = FontFamily.Serif,
        fontSize = proseSize,
        lineHeight = proseSize * 1.68f,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Raised initial. A real drop cap needs text to wrap around a floating letter, which Compose
 * cannot do, so the opening capital sits beside the paragraph instead of behind it.
 */
@Composable
private fun DropCapParagraph(text: String, proseSize: TextUnit, palette: ReaderCardPalette) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(
            text = text.take(1).uppercase(),
            color = palette.title,
            fontFamily = FontFamily.Serif,
            fontSize = proseSize * 3.4f,
            lineHeight = proseSize * 1.1f,
            modifier = Modifier.padding(end = 2.dp),
        )
        Text(
            text = text.drop(1),
            color = palette.ink,
            fontFamily = FontFamily.Serif,
            fontSize = proseSize,
            lineHeight = proseSize * 1.68f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ResumeParagraph(
    paragraph: String,
    marker: String?,
    proseSize: TextUnit,
    palette: ReaderCardPalette,
    flash: Float,
) {
    val annotated = remember(paragraph, marker, palette, flash) {
        val start = marker?.let { paragraph.indexOf(it, ignoreCase = true) } ?: -1
        if (start < 0) {
            AnnotatedString(paragraph)
        } else {
            buildAnnotatedString {
                append(paragraph)
                addStyle(
                    SpanStyle(
                        background = lerpColor(
                            palette.mark,
                            palette.accent.copy(alpha = 0.34f),
                            flash.coerceIn(0f, 1f),
                        ),
                    ),
                    start,
                    paragraph.length,
                )
            }
        }
    }
    Text(
        text = annotated,
        color = palette.ink,
        fontFamily = FontFamily.Serif,
        fontSize = proseSize,
        lineHeight = proseSize * 1.68f,
        modifier = Modifier.fillMaxWidth(),
    )
}

/* ─────────────────────────────── scene ────────────────────────────── */

@Composable
private fun AmbientOrbs(palette: ReaderCardPalette, drift: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val a by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb-a",
    )
    val b by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(25_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb-b",
    )
    val c by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(30_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "orb-c",
    )
    val shift = drift.coerceIn(0f, 1f)

    Canvas(modifier) {
        val base = size.minDimension * 0.64f
        val radius = base * 0.65f

        fun orb(cx: Float, cy: Float, dx: Float, dy: Float, t: Float, color: Color) {
            val center = Offset(cx + dx * t * shift, cy + dy * t * shift)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = palette.orbOpacity), Color.Transparent),
                    center = center,
                    radius = radius,
                ),
                radius = radius,
                center = center,
            )
        }

        orb(size.width * 0.15f, size.height * 0.05f, size.width * 0.09f, size.height * 0.06f, a, palette.orbA)
        orb(size.width * 0.92f, size.height * 0.40f, -size.width * 0.08f, size.height * 0.05f, b, palette.orbB)
        orb(size.width * 0.45f, size.height * 1.10f, size.width * 0.06f, -size.height * 0.08f, c, palette.orbC)
    }
}

/* ─────────────────────────────── glyphs ───────────────────────────── */

@Composable
private fun CloseGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(18.dp)) {
        val s = size.minDimension
        val stroke = 2.dp.toPx()
        drawLine(tint, Offset(s * 0.15f, s * 0.15f), Offset(s * 0.85f, s * 0.85f), stroke, StrokeCap.Round)
        drawLine(tint, Offset(s * 0.85f, s * 0.15f), Offset(s * 0.15f, s * 0.85f), stroke, StrokeCap.Round)
    }
}

@Composable
private fun ExpandGlyph(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(15.dp)) {
        val s = size.minDimension
        val stroke = 1.6.dp.toPx()
        val lo = 0.18f * s
        val hi = 0.82f * s
        drawLine(tint, Offset(lo, hi), Offset(hi, lo), stroke, StrokeCap.Round)
        // arrowheads at both ends of the diagonal
        drawLine(tint, Offset(hi, lo), Offset(hi - 0.26f * s, lo), stroke, StrokeCap.Round)
        drawLine(tint, Offset(hi, lo), Offset(hi, lo + 0.26f * s), stroke, StrokeCap.Round)
        drawLine(tint, Offset(lo, hi), Offset(lo + 0.26f * s, hi), stroke, StrokeCap.Round)
        drawLine(tint, Offset(lo, hi), Offset(lo, hi - 0.26f * s), stroke, StrokeCap.Round)
    }
}

/* ────────────────────────────── palette ───────────────────────────── */

private data class ReaderCardPalette(
    val page: Color,
    val card: Color,
    val rim: Color,
    val title: Color,
    val inkOnCard: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val glassTint: Color,
    val glassInk: Color,
    val panelTint: Color,
    val panelSolid: Color,
    val scrim: Color,
    val track: Color,
    val orbA: Color,
    val orbB: Color,
    val orbC: Color,
    val orbOpacity: Float,
    val mark: Color,
)

private fun readerCardPalette(dark: Boolean): ReaderCardPalette =
    if (dark) {
        ReaderCardPalette(
            page = Color(0xFF0E0E10),
            card = Color(0xFF1B1B1E),
            rim = Color(0x12FFFFFF),
            title = Color(0xFFF4F4F5),
            inkOnCard = Color(0xFFB4B4B9),
            ink = Color(0xFFD6D6DA),
            muted = Color(0xFF8B8B92),
            accent = Color(0xFFFF5A60),
            glassTint = Color(0x3828282E),
            glassInk = Color(0xFFF4F4F5),
            panelTint = Color(0xA818181C),
            panelSolid = Color(0xFF1B1B1E),
            scrim = Color(0x59000000),
            track = Color(0x24FFFFFF),
            orbA = Color(0xFF6B1D24),
            orbB = Color(0xFF1C2E66),
            orbC = Color(0xFF3B2A10),
            orbOpacity = 0.70f,
            mark = Color(0x33FF5A60),
        )
    } else {
        ReaderCardPalette(
            page = Color(0xFFDCDCDC),
            card = Color(0xFFF7F7F7),
            rim = Color(0x47FFFFFF),
            title = Color(0xFF000000),
            inkOnCard = Color(0xFF4A4A4A),
            ink = Color(0xFF2B2B2B),
            muted = Color(0xFF8A8A8A),
            accent = Color(0xFFD3343C),
            glassTint = Color(0x38FFFFFF),
            glassInk = Color(0xFF111111),
            panelTint = Color(0xA8FAFAFA),
            panelSolid = Color(0xFFF7F7F7),
            scrim = Color(0x59CDCDCD),
            track = Color(0x1A000000),
            orbA = Color(0xFFF4B6A0),
            orbB = Color(0xFFA9C4F2),
            orbC = Color(0xFFF3DD9B),
            orbOpacity = 0.55f,
            mark = Color(0x29D3343C),
        )
    }

private val PROSE_SIZES = listOf(16f, 17.5f, 19f, 21f, 23f)
private const val START_PROSE_SIZE = 2

/* ─────────────────────────────── preview ──────────────────────────── */

@Preview(showBackground = true, widthDp = 400, heightDp = 700)
@Composable
private fun ComposeKitReaderCardPreview() {
    MaterialTheme {
        ComposeKitReaderCard(
            title = "The Clockmaker's Apprentice",
            chapterLabel = "Chapter One",
            meta = "Demo content",
            excerpt = listOf(
                "The shop smelled of brass filings and cold tea. Neve had swept the same floor every morning for a year, and every morning the broom found a gear she had never seen before.",
                "Her master never explained the clocks. He handed her a screwdriver and said the rest was patience.",
            ),
            chapter = listOf(
                "Morning came grey through the shop window, and the hundred clocks on the wall disagreed about it, as they always had.",
                "The shop smelled of brass filings and cold tea. She had swept the same floor every morning for a year, and every morning the broom found a gear she had never seen before.",
                "On the bench lay a watch with no face, its hands turning in opposite directions. Her master had left it there without a note, which meant he expected her to ask nothing and finish it.",
                "By noon she had it running. By evening she understood that it had been running all along, and that the hands were not telling time but counting something else.",
                "She put down the screwdriver. Somewhere behind her the clocks on the wall fell silent at once, and she decided she would not mention it.",
            ),
            resumeAfter = "On the bench lay a watch with no face",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
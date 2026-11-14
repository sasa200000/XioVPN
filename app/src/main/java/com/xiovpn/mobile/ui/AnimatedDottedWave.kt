package com.xiovpn.mobile.ui

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.request.ImageRequestBuilder
import com.xiovpn.mobile.R
import com.xiovpn.mobile.ui.theme.XioColors
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.unit.Dp

@Composable
internal fun AnimatedDottedWave(
    accent: Color,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.verticalGradient(
                    0f to XioColors.BackgroundBottom.copy(alpha = 0.10f),
                    0.22f to accent.copy(alpha = 0.025f),
                    1f to XioColors.BackgroundBottom.copy(alpha = 0.58f),
                ),
            ),
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PlatformAnimatedWave(
                accent = accent,
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            LegacyAnimatedWebpWave(
                accent = accent,
                motionEnabled = motionEnabled,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun PlatformAnimatedWave(
    accent: Color,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val animatedWave: Drawable? = remember(context) {
        Api28Impl.decodeAnimatedWave(context)
    }

    DisposableEffect(animatedWave, motionEnabled) {
        if (animatedWave != null) {
            Api28Impl.setRunning(
                drawable = animatedWave,
                running = motionEnabled,
            )
        }

        onDispose {
            if (animatedWave != null) {
                Api28Impl.stop(animatedWave)
            }
        }
    }

    val accentArgb = accent.toArgb()

    AndroidView(
        factory = { viewContext ->
            ImageView(viewContext).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                setImageDrawable(animatedWave)
            }
        },
        update = { imageView ->
            imageView.setColorFilter(
                accentArgb,
                PorterDuff.Mode.SRC_IN,
            )

            if (animatedWave != null) {
                Api28Impl.setRunning(
                    drawable = animatedWave,
                    running = motionEnabled,
                )
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun LegacyAnimatedWebpWave(
    accent: Color,
    motionEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    remember(context.applicationContext) {
        if (!Fresco.hasBeenInitialized()) {
            Fresco.initialize(context.applicationContext)
        }
        true
    }

    val accentArgb = accent.toArgb()

    AndroidView(
        factory = { viewContext ->
            SimpleDraweeView(viewContext).apply {
                hierarchy =
                    GenericDraweeHierarchyBuilder
                        .newInstance(resources)
                        .setActualImageScaleType(
                            ScalingUtils.ScaleType.CENTER_CROP,
                        )
                        .setFadeDuration(0)
                        .build()

                tag = null
            }
        },
        update = { draweeView ->
            draweeView.hierarchy.setActualImageColorFilter(
                PorterDuffColorFilter(
                    accentArgb,
                    PorterDuff.Mode.SRC_IN,
                ),
            )

            val previousMotionState =
                draweeView.tag as? Boolean

            if (previousMotionState != motionEnabled) {
                draweeView.controller
                    ?.animatable
                    ?.stop()

                val request =
                    ImageRequestBuilder
                        .newBuilderWithResourceId(
                            R.drawable.xio_digital_wave,
                        )
                        .build()

                val controller =
                    Fresco
                        .newDraweeControllerBuilder()
                        .setOldController(
                            draweeView.controller,
                        )
                        .setImageRequest(request)
                        .setAutoPlayAnimations(
                            motionEnabled,
                        )
                        .build()

                draweeView.controller = controller
                draweeView.tag = motionEnabled
            } else {
                val animatable =
                    draweeView.controller?.animatable

                if (motionEnabled) {
                    animatable?.start()
                } else {
                    animatable?.stop()
                }
            }
        },
        modifier = modifier,
    )
}

@RequiresApi(Build.VERSION_CODES.P)
private object Api28Impl {

    fun decodeAnimatedWave(
        context: Context,
    ): Drawable {
        val source =
            android.graphics.ImageDecoder.createSource(
                context.resources,
                R.drawable.xio_digital_wave,
            )

        return android.graphics.ImageDecoder.decodeDrawable(
            source,
        )
    }

    fun setRunning(
        drawable: Drawable,
        running: Boolean,
    ) {
        val animated =
            drawable as?
                android.graphics.drawable.AnimatedImageDrawable
                ?: return

        animated.repeatCount =
            android.graphics.drawable.AnimatedImageDrawable
                .REPEAT_INFINITE

        if (running) {
            animated.start()
        } else {
            animated.stop()
        }
    }

    fun stop(
        drawable: Drawable,
    ) {
        (
            drawable as?
                android.graphics.drawable.AnimatedImageDrawable
            )
            ?.stop()
    }
}

/**
 * Live "sea wave" chart: the sampled values ripple like water and the crest
 * colour steps once per second while connected. Shared by the home traffic
 * cards so every live graph in XioVPN moves the same way.
 *
 * @param values   recent samples (oldest first)
 * @param colorTick monotonic tick that changes once per second; drives the hue
 * @param connected true while the tunnel is up (enables the hue cycle)
 */
@Composable
internal fun SeaWaveChart(
    values: List<Float>,
    accent: Color,
    connected: Boolean,
    motionEnabled: Boolean = true,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 34.dp,
) {
    val tick by rememberInfiniteTransition(label = "sea-wave-tick").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 60_000,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "sea-wave-hue",
    )
    val swell by rememberInfiniteTransition(label = "sea-wave-swell").animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = if (motionEnabled) 1_600 else 1,
                easing = androidx.compose.animation.core.LinearEasing,
            ),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
        ),
        label = "sea-wave-phase",
    )
    val crest = if (connected) Color.hsv(tick, 0.68f, 1f) else accent

    Canvas(modifier = modifier.height(height)) {
        val baseline = size.height - 3.dp.toPx()
        drawLine(
            color = accent.copy(alpha = 0.20f),
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1.dp.toPx(),
        )
        if (values.size < 2) return@Canvas

        val lo = values.min()
        val hi = values.max()
        val span = (hi - lo).coerceAtLeast(0.001f)
        val last = (values.lastIndex).coerceAtLeast(1).toFloat()
        val usable = (size.height - 6.dp.toPx()).coerceAtLeast(8.dp.toPx())

        fun pointFor(index: Int): Offset {
            val x = size.width * index / last
            val normalised = ((values[index] - lo) / span).coerceIn(0f, 1f)
            // Travelling swell, so even a flat series reads as moving water.
            val wave = 0.10f * kotlin.math.sin(swell + index * 0.55f)
            val y = baseline - (normalised + wave).coerceIn(0f, 1f) * usable
            return Offset(x, y)
        }

        val line = Path()
        val fill = Path()
        val first = pointFor(0)
        line.moveTo(first.x, first.y)
        fill.moveTo(first.x, baseline)
        fill.lineTo(first.x, first.y)
        for (index in 1..values.lastIndex) {
            val p = pointFor(index)
            line.lineTo(p.x, p.y)
            fill.lineTo(p.x, p.y)
        }
        fill.lineTo(size.width, baseline)
        fill.close()

        drawPath(
            path = fill,
            brush = Brush.verticalGradient(
                listOf(crest.copy(alpha = 0.30f), crest.copy(alpha = 0f)),
            ),
        )
        drawPath(
            path = line,
            color = crest,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        val head = pointFor(values.lastIndex)
        drawCircle(crest.copy(alpha = 0.34f), radius = 5.dp.toPx(), center = head)
        drawCircle(crest, radius = 2.4.dp.toPx(), center = head)
    }
}

/**
 * A bright light that laps the outline of a rounded rectangle, with the colour
 * stepping once per second. This is the card equivalent of the Connect button's
 * rim light, used around the live download/upload cards.
 *
 * @param tick monotonic counter incremented once per second by the caller
 * @param connected while true the colour cycles through the hue wheel;
 *                  otherwise it stays on [accent]
 * @param fixedColor non-null pins the colour (the user's Glow choice)
 */
@Composable
internal fun BorderLightDance(
    tick: Int,
    accent: Color,
    connected: Boolean,
    motionEnabled: Boolean = true,
    cornerRadius: androidx.compose.ui.unit.Dp = 20.dp,
    thickness: androidx.compose.ui.unit.Dp = 2.2.dp,
    lengthFraction: Float = 0.26f,
    modifier: Modifier = Modifier,
) {
    val orbit by androidx.compose.animation.core.rememberInfiniteTransition(label = "border-dance")
        .animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(
                    durationMillis = if (motionEnabled) 2_400 else 1,
                    easing = androidx.compose.animation.core.LinearEasing,
                ),
                repeatMode = androidx.compose.animation.core.RepeatMode.Restart,
            ),
            label = "border-dance-orbit",
        )
    val color = if (connected) Color.hsv((tick * 37f) % 360f, 0.70f, 1f) else accent

    Canvas(modifier = modifier) {
        val outline = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    radiusX = cornerRadius.toPx(),
                    radiusY = cornerRadius.toPx(),
                ),
            )
        }
        // Dim base so the shape reads even where the light is not.
        drawPath(
            path = outline,
            color = color.copy(alpha = 0.16f),
            style = Stroke(width = 1.dp.toPx()),
        )

        val measure = androidx.compose.ui.graphics.PathMeasure()
        measure.setPath(outline, false)
        val total = measure.length
        if (total <= 0f) return@Canvas

        // Head position plus a fading tail behind it.
        val head = orbit * total
        val tailLength = (total * lengthFraction).coerceAtLeast(8.dp.toPx())
        val segments = 26
        for (step in 0 until segments) {
            val t = step / segments.toFloat()
            val distance = (head - tailLength + tailLength * t + total) % total
            val position = measure.getPosition(distance)
            val alpha = (1f - t) * 0.80f
            val radius = thickness.toPx() * (1f - 0.45f * t)
            drawCircle(color = color.copy(alpha = alpha), radius = radius, center = position)
        }
        // Bright head.
        drawCircle(
            color = color,
            radius = thickness.toPx() * 1.25f,
            center = measure.getPosition(head % total),
        )
    }
}

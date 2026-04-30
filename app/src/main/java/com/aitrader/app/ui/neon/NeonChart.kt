package com.aitrader.app.ui.neon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aitrader.app.model.Candle
import kotlin.math.max
import kotlin.math.min

// ─── Sparkline ────────────────────────────────────────────────
@Composable
fun Sparkline(
    data: List<Double>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    width: Dp = 60.dp,
    height: Dp = 20.dp,
) {
    if (data.size < 2) return
    val dMin = data.min()
    val dMax = data.max()
    val spread = (dMax - dMin).coerceAtLeast(1e-6)
    val up = data.last() >= data.first()
    val stroke = color ?: if (up) NeonTokens.Bull else NeonTokens.Bear
    Canvas(modifier = modifier.width(width).height(height)) {
        val stepX = size.width / (data.size - 1)
        val path = Path()
        val fill = Path()
        data.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - (((v - dMin) / spread).toFloat() * size.height)
            if (i == 0) {
                path.moveTo(x, y)
                fill.moveTo(x, size.height)
                fill.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fill.lineTo(x, y)
            }
        }
        fill.lineTo(size.width, size.height)
        fill.close()
        drawPath(fill, color = stroke.copy(alpha = 0.12f))
        drawPath(path, color = stroke, style = Stroke(width = 1.5f))
    }
}

// ─── Mini chart wrapper for position rows ─────────────────────
@Composable
fun MiniChart(
    candles: List<Candle>,
    modifier: Modifier = Modifier,
    color: Color? = null,
    width: Dp = 54.dp,
    height: Dp = 22.dp,
) {
    if (candles.size < 2) return
    Sparkline(
        data = candles.map { it.c },
        modifier = modifier,
        color = color,
        width = width,
        height = height,
    )
}

// ─── Candlestick chart ────────────────────────────────────────
@Composable
fun Candlestick(
    candles: List<Candle>,
    modifier: Modifier,
    accent: Color = NeonTokens.Neon,
    showSma: Boolean = true,
    showEma: Boolean = false,
    showVolume: Boolean = true,
    lineMode: Boolean = false,
    crosshairIndex: Int? = null,
) {
    if (candles.size < 2) return
    val dMin = candles.minOf { it.l }
    val dMax = candles.maxOf { it.h }
    val spread = (dMax - dMin).coerceAtLeast(1e-6)

    // SMA20
    val sma = candles.indices.map { i ->
        if (i < 19) null
        else candles.subList(i - 19, i + 1).sumOf { it.c } / 20.0
    }

    // EMA20
    val ema: List<Double?> = if (showEma) {
        val k = 2.0 / 21.0
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size >= 20) {
            var e = candles.take(20).sumOf { it.c } / 20.0
            result[19] = e
            for (i in 20 until candles.size) {
                e = candles[i].c * k + e * (1 - k)
                result[i] = e
            }
        }
        result
    } else List(candles.size) { null }

    val maxBody = candles.maxOf { kotlin.math.abs(it.c - it.o) }.coerceAtLeast(1e-6)

    Canvas(modifier = modifier) {
        val priceH = if (showVolume) size.height * 0.75f else size.height
        val volH = size.height - priceH
        val stepX = size.width / candles.size
        val bodyW = max(2f, stepX * 0.65f)
        fun yFor(p: Double): Float =
            (((dMax - p) / spread).toFloat() * (priceH - 8f)) + 4f

        // grid lines
        listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
            val y = frac * priceH
            drawLine(
                SolidColor(NeonTokens.Border),
                Offset(0f, y),
                Offset(size.width, y),
                strokeWidth = 0.5f,
            )
        }

        if (lineMode) {
            // Draw as closing-price polyline with gradient fill
            val path = Path()
            val fill = Path()
            candles.forEachIndexed { i, c ->
                val x = i * stepX + stepX / 2f
                val y = yFor(c.c)
                if (i == 0) {
                    path.moveTo(x, y)
                    fill.moveTo(x, priceH)
                    fill.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fill.lineTo(x, y)
                }
            }
            fill.lineTo(size.width, priceH)
            fill.close()
            drawPath(fill, color = accent.copy(alpha = 0.15f))
            drawPath(path, color = accent, style = Stroke(width = 1.8f))
        } else {
            // Candles + volume
            candles.forEachIndexed { i, c ->
                val x = i * stepX + stepX / 2f
                val up = c.c >= c.o
                val col = if (up) NeonTokens.Bull else NeonTokens.Bear
                val yO = yFor(c.o)
                val yC = yFor(c.c)
                val yH = yFor(c.h)
                val yL = yFor(c.l)
                val bodyTop = min(yO, yC)
                val bodyH = max(1f, kotlin.math.abs(yC - yO))
                drawLine(SolidColor(col), Offset(x, yH), Offset(x, yL), strokeWidth = 1f)
                drawRect(color = col, topLeft = Offset(x - bodyW / 2, bodyTop), size = Size(bodyW, bodyH))
                if (showVolume) {
                    val v = (kotlin.math.abs(c.c - c.o) / maxBody).toFloat()
                    val h = v * (volH - 4f)
                    drawRect(
                        color = col.copy(alpha = 0.35f),
                        topLeft = Offset(x - bodyW / 2, size.height - h),
                        size = Size(bodyW, h),
                    )
                }
            }
        }

        // SMA line (amber)
        if (showSma) {
            val path = Path()
            var started = false
            sma.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val x = i * stepX + stepX / 2f
                val y = yFor(v)
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
            drawPath(path, color = NeonTokens.Amber.copy(alpha = 0.9f), style = Stroke(width = 1.3f))
        }

        // EMA line (cyan)
        if (showEma) {
            val path = Path()
            var started = false
            ema.forEachIndexed { i, v ->
                if (v == null) return@forEachIndexed
                val x = i * stepX + stepX / 2f
                val y = yFor(v)
                if (!started) { path.moveTo(x, y); started = true } else path.lineTo(x, y)
            }
            drawPath(path, color = NeonTokens.Cyan.copy(alpha = 0.9f), style = Stroke(width = 1.3f))
        }

        // Last price line
        val last = candles.last().c
        val lastY = yFor(last)
        drawLine(
            SolidColor(accent.copy(alpha = 0.6f)),
            Offset(0f, lastY),
            Offset(size.width, lastY),
            strokeWidth = 0.8f,
        )

        // Crosshair
        if (crosshairIndex != null && crosshairIndex in candles.indices) {
            val x = crosshairIndex * stepX + stepX / 2f
            val c = candles[crosshairIndex]
            val y = yFor(c.c)
            drawLine(SolidColor(NeonTokens.Neon.copy(alpha = 0.7f)), Offset(x, 0f), Offset(x, priceH), strokeWidth = 1f)
            drawLine(SolidColor(NeonTokens.Neon.copy(alpha = 0.7f)), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
    }
}

package com.sppidy.janus.ui.neon

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

// ─── Tag / chip ───────────────────────────────────────────────
@Composable
fun Tag(
    text: String,
    color: Color = NeonTokens.Neon,
    solid: Boolean = false,
    small: Boolean = false,
) {
    val fg = if (solid) Color.Black else color
    val bg = if (solid) color else Color.Transparent
    Box(
        modifier = Modifier
            .background(bg)
            .border(width = if (solid) 0.dp else 1.dp, color = color)
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            color = fg,
            fontSize = if (small) 9.sp else 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.12.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

// ─── Corner-bracket frame ─────────────────────────────────────
@Composable
fun Bracketed(
    color: Color = NeonTokens.Neon,
    padding: Dp = 12.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val bracketLen = 10.dp
    Box(
        modifier = modifier
            .drawBehind {
                val sw = 1.dp.toPx()
                val c = bracketLen.toPx()
                val w = size.width
                val h = size.height
                // top-left
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(c, 0f), sw)
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(0f, c), sw)
                // top-right
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(w - c, 0f), androidx.compose.ui.geometry.Offset(w, 0f), sw)
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(w, 0f), androidx.compose.ui.geometry.Offset(w, c), sw)
                // bottom-left
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(0f, h - c), androidx.compose.ui.geometry.Offset(0f, h), sw)
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(0f, h), androidx.compose.ui.geometry.Offset(c, h), sw)
                // bottom-right
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(w - c, h), androidx.compose.ui.geometry.Offset(w, h), sw)
                drawLine(SolidColor(color), androidx.compose.ui.geometry.Offset(w, h - c), androidx.compose.ui.geometry.Offset(w, h), sw)
            }
            .padding(padding),
    ) {
        Column { content() }
    }
}

// ─── Section label — "// THING" ───────────────────────────────
@Composable
fun SectionLabel(
    text: String,
    right: String? = null,
    color: Color = NeonTokens.TextMute,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "// ${text.uppercase()}",
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
        if (right != null) {
            Text(
                text = right.uppercase(),
                color = NeonTokens.TextDim,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
    }
}

// ─── Panel — terminal-style card ──────────────────────────────
@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color = NeonTokens.Border,
    glow: Boolean = false,
    fill: Color = NeonTokens.BgElev1,
    content: @Composable BoxScope.() -> Unit,
) {
    val glowMod = if (glow) {
        Modifier.drawBehind {
            val glowColor = NeonTokens.NeonSoft
            drawRect(glowColor, topLeft = androidx.compose.ui.geometry.Offset(-4.dp.toPx(), -4.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()))
        }
    } else Modifier
    Box(
        modifier = modifier
            .then(glowMod)
            .background(fill)
            .border(1.dp, accent, RoundedCornerShape(2.dp)),
        content = content,
    )
}

// ─── PnL text ─────────────────────────────────────────────────
@Composable
fun PnlText(
    value: Double,
    pct: Boolean = false,
    size: Int = 13,
    weight: FontWeight = FontWeight.SemiBold,
) {
    val c = if (value >= 0) NeonTokens.Bull else NeonTokens.Bear
    val text = if (pct) fmtPct(value) else fmtSigned(value)
    Text(
        text = text,
        color = c,
        fontSize = size.sp,
        fontWeight = weight,
        fontFamily = NeonTokens.MonoFamily,
    )
}

// ─── Pulsing live dot ─────────────────────────────────────────
@Composable
fun LiveDot(
    color: Color = NeonTokens.Bull,
    size: Dp = 8.dp,
) {
    val transition = rememberInfiniteTransition(label = "livedot")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringScale",
    )
    val alpha by transition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringAlpha",
    )
    Box(
        modifier = Modifier.size(size * 2),
        contentAlignment = Alignment.Center,
    ) {
        // Pulsing ring
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                .border(1.dp, color, RoundedCornerShape(50))
        )
        // Core dot
        Box(
            modifier = Modifier
                .size(size)
                .background(color, RoundedCornerShape(50))
        )
    }
}

// ─── Blinking cursor ──────────────────────────────────────────
@Composable
fun Cursor(
    color: Color = NeonTokens.Neon,
    size: Dp = 10.dp,
) {
    val transition = rememberInfiniteTransition(label = "cursor")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "blink",
    )
    Box(
        modifier = Modifier
            .width(size * 0.6f)
            .height(size)
            .graphicsLayer { this.alpha = alpha }
            .background(color)
    )
}

// ─── Neon button ──────────────────────────────────────────────
enum class NeonButtonVariant { Primary, Ghost, Bull, Bear, Muted }

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: NeonButtonVariant = NeonButtonVariant.Primary,
    icon: (@Composable () -> Unit)? = null,
    large: Boolean = false,
    small: Boolean = false,
    enabled: Boolean = true,
) {
    data class ButtonColors(val bg: Color, val fg: Color, val border: Color)
    val colors = when (variant) {
        NeonButtonVariant.Primary -> ButtonColors(NeonTokens.Neon, Color.Black, NeonTokens.Neon)
        NeonButtonVariant.Ghost -> ButtonColors(Color.Transparent, NeonTokens.Neon, NeonTokens.Neon)
        NeonButtonVariant.Bull -> ButtonColors(NeonTokens.Bull, Color(0xFF001A0F), NeonTokens.Bull)
        NeonButtonVariant.Bear -> ButtonColors(NeonTokens.Bear, Color(0xFF1A0010), NeonTokens.Bear)
        NeonButtonVariant.Muted -> ButtonColors(NeonTokens.BgElev2, NeonTokens.Text, NeonTokens.Border)
    }
    val vPad = if (small) 6.dp else if (large) 12.dp else 8.dp
    val hPad = if (small) 10.dp else if (large) 20.dp else 14.dp
    val fs = if (small) 11.sp else if (large) 14.sp else 12.sp
    Box(
        modifier = modifier
            .background(if (enabled) colors.bg else colors.bg.copy(alpha = 0.4f))
            .border(1.dp, if (enabled) colors.border else colors.border.copy(alpha = 0.4f))
            .graphicsLayer { alpha = if (enabled) 1f else 0.5f }
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = hPad, vertical = vPad),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) icon()
            Text(
                text = text.uppercase(),
                color = colors.fg,
                fontSize = fs,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
    }
}

// ─── Ticker tape (scrolling symbols) ──────────────────────────
data class TickerItem(val sym: String, val price: String, val changePct: Double)

val DefaultTickerItems = listOf(
    TickerItem("NIFTY", "24,812", 0.82),
    TickerItem("SENSEX", "81,294", 0.74),
    TickerItem("USDINR", "83.24", -0.12),
    TickerItem("BTC", "\$94,210", 2.41),
    TickerItem("GOLD", "72,480", 0.31),
    TickerItem("CRUDE", "\$78.42", -1.24),
    TickerItem("EURUSD", "1.0842", 0.15),
    TickerItem("GBPUSD", "1.2683", -0.08),
    TickerItem("BANKNIFTY", "52,410", 1.12),
)

@Composable
fun TickerTape(
    items: List<TickerItem> = DefaultTickerItems,
    speedMs: Int = 35_000,
    modifier: Modifier = Modifier,
) {
    var contentHalfWidth by remember { mutableFloatStateOf(0f) }
    val transition = rememberInfiniteTransition(label = "ticker")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(speedMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .background(NeonTokens.Bg)
            .border(1.dp, NeonTokens.Border)
            .clipToBounds(),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .graphicsLayer { translationX = -progress * contentHalfWidth }
                .onGloballyPositioned { coords ->
                    contentHalfWidth = coords.size.width.toFloat() / 2f
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(2) {
                Spacer(Modifier.width(20.dp))
                items.forEach { item ->
                    TickerItemView(item)
                    Spacer(Modifier.width(20.dp))
                }
            }
        }
    }
}

@Composable
private fun TickerItemView(item: TickerItem) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            item.sym,
            color = NeonTokens.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NeonTokens.MonoFamily,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            item.price,
            color = NeonTokens.TextMute,
            fontSize = 11.sp,
            fontFamily = NeonTokens.MonoFamily,
            maxLines = 1,
            softWrap = false,
        )
        Text(
            "${if (item.changePct >= 0) "▲" else "▼"}${"%.2f".format(abs(item.changePct))}%",
            color = if (item.changePct >= 0) NeonTokens.Bull else NeonTokens.Bear,
            fontSize = 11.sp,
            fontFamily = NeonTokens.MonoFamily,
            maxLines = 1,
            softWrap = false,
        )
    }
}

// ─── Grid background overlay ──────────────────────────────────
@Composable
fun GridBackground(
    modifier: Modifier = Modifier,
    cellSize: Dp = 24.dp,
    color: Color = NeonTokens.Neon,
    alpha: Float = 0.04f,
) {
    Box(
        modifier = modifier.drawBehind {
            val cs = cellSize.toPx()
            val c = color.copy(alpha = alpha)
            var x = 0f
            while (x <= size.width) {
                drawLine(SolidColor(c), androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), 1f)
                x += cs
            }
            var y = 0f
            while (y <= size.height) {
                drawLine(SolidColor(c), androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), 1f)
                y += cs
            }
        }
    )
}

// ─── Scanline overlay (animated) ──────────────────────────────
@Composable
fun ScanlineOverlay(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "scanline")
    val offset by transition.animateFloat(
        initialValue = -0.2f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "y",
    )
    Box(
        modifier = modifier.drawBehind {
            val bandH = size.height * 0.08f
            val topY = size.height * offset - bandH / 2
            val brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    NeonTokens.Neon.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startY = topY,
                endY = topY + bandH,
            )
            drawRect(brush, topLeft = androidx.compose.ui.geometry.Offset(0f, topY),
                size = androidx.compose.ui.geometry.Size(size.width, bandH))
        }
    )
}

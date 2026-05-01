package com.sppidy.janus.ui.neon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.sppidy.janus.R
import kotlin.math.abs

object NeonTokens {
    // Base palette — deep space blacks
    val Bg = Color(0xFF05060A)
    val BgElev1 = Color(0xFF0A0D14)
    val BgElev2 = Color(0xFF10141D)
    val BgElev3 = Color(0xFF161C26)
    val Border = Color(0xFF1E2730)
    val BorderHot = Color(0x59B4FF00) // rgba(180,255,0,0.35)

    // Text
    val Text = Color(0xFFE6FFE9)
    val TextMute = Color(0xFF7A8A7E)
    val TextDim = Color(0xFF4A5A4E)

    // Accents
    val Neon = Color(0xFFB4FF00)
    val NeonGlow = Color(0x80B4FF00) // rgba(180,255,0,0.5)
    val NeonSoft = Color(0x1FB4FF00) // rgba(180,255,0,0.12)

    // Semantic — bull/bear
    val Bull = Color(0xFF00FF94)
    val BullSoft = Color(0x24_00FF94) // rgba(0,255,148,0.14)
    val Bear = Color(0xFFFF2D6F)
    val BearSoft = Color(0x24_FF2D6F) // rgba(255,45,111,0.14)

    // Secondary
    val Amber = Color(0xFFFFB800)
    val Cyan = Color(0xFF00E5FF)
    val Violet = Color(0xFFB388FF)

    // Bundled fonts — JetBrains Mono for data/UI, Space Grotesk (variable) for headings
    val MonoFamily: FontFamily = FontFamily(
        Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
        Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
        Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
    )
    val DisplayFamily: FontFamily = FontFamily(
        Font(R.font.space_grotesk, FontWeight.Normal),
        Font(R.font.space_grotesk, FontWeight.Medium),
        Font(R.font.space_grotesk, FontWeight.Bold),
    )
}

// ₹1,00,00,000 → ₹1.00Cr etc.
fun fmtCurrency(v: Double?, sym: String = "₹"): String {
    if (v == null || v.isNaN()) return "—"
    val a = abs(v)
    return when {
        a >= 1e7 -> "$sym${"%.2f".format(v / 1e7)}Cr"
        a >= 1e5 -> "$sym${"%.2f".format(v / 1e5)}L"
        a >= 1000 -> "$sym${"%.2f".format(v / 1000)}K"
        else -> "$sym${"%.2f".format(v)}"
    }
}

fun fmtPct(v: Double): String {
    val sign = if (v >= 0) "+" else ""
    return "$sign${"%.2f".format(v)}%"
}

fun fmtSigned(v: Double): String {
    val sign = if (v >= 0) "+" else ""
    return "$sign${"%.2f".format(v)}"
}

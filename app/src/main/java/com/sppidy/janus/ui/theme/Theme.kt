package com.sppidy.janus.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sppidy.janus.ui.neon.NeonTokens

// Legacy color aliases — kept for screens not yet fully migrated to NeonTokens directly.
// All map to the NEON palette so the app stays visually consistent.
val Profit = NeonTokens.Bull
val Loss = NeonTokens.Bear
val BuyBlue = NeonTokens.Bull
val SellOrange = NeonTokens.Bear
val HoldGray = NeonTokens.TextMute
val BullGreen = NeonTokens.Bull
val BearRed = NeonTokens.Bear
val NeutralAmber = NeonTokens.Amber

private val NeonColorScheme = darkColorScheme(
    primary = NeonTokens.Neon,
    onPrimary = Color(0xFF000000),
    primaryContainer = NeonTokens.NeonSoft,
    onPrimaryContainer = NeonTokens.Neon,
    secondary = NeonTokens.Cyan,
    onSecondary = Color(0xFF000000),
    tertiary = NeonTokens.Amber,
    onTertiary = Color(0xFF000000),
    background = NeonTokens.Bg,
    onBackground = NeonTokens.Text,
    surface = NeonTokens.BgElev1,
    onSurface = NeonTokens.Text,
    surfaceVariant = NeonTokens.BgElev2,
    onSurfaceVariant = NeonTokens.TextMute,
    surfaceContainer = NeonTokens.BgElev2,
    surfaceContainerHigh = NeonTokens.BgElev3,
    outline = NeonTokens.Border,
    outlineVariant = NeonTokens.Border,
    error = NeonTokens.Bear,
    onError = Color(0xFF000000),
)

private val NeonTypography = Typography(
    displayLarge = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, letterSpacing = (-0.02).sp),
    displayMedium = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineLarge = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    headlineMedium = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineSmall = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.Bold, fontSize = 18.sp),
    titleLarge = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontFamily = NeonTokens.DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    titleSmall = TextStyle(fontFamily = NeonTokens.MonoFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontFamily = NeonTokens.MonoFamily, fontSize = 14.sp),
    bodyMedium = TextStyle(fontFamily = NeonTokens.MonoFamily, fontSize = 12.sp),
    bodySmall = TextStyle(fontFamily = NeonTokens.MonoFamily, fontSize = 11.sp),
    labelLarge = TextStyle(fontFamily = NeonTokens.MonoFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = NeonTokens.MonoFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, letterSpacing = 0.15.sp),
    labelSmall = TextStyle(fontFamily = NeonTokens.MonoFamily, fontWeight = FontWeight.Bold, fontSize = 9.sp, letterSpacing = 0.2.sp),
)

@Composable
fun AITraderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeonColorScheme,
        typography = NeonTypography,
        content = content,
    )
}

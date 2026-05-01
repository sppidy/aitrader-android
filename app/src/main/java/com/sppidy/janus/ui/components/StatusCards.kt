package com.sppidy.janus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.ui.theme.Loss
import com.sppidy.janus.ui.theme.Profit
import java.text.NumberFormat
import java.util.Locale

private val inrFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "IN"))
private val usdFormat = NumberFormat.getCurrencyInstance(Locale.of("en", "US"))

/** Format currency based on active market mode. */
fun formatCurrency(amount: Double): String {
    return if (AppPreferences.getMarketMode() == "FOREX") {
        usdFormat.format(amount)
    } else {
        inrFormat.format(amount)
    }
}

/** Clean symbol suffix based on market mode. */
fun cleanSymbol(symbol: String): String {
    return symbol.removeSuffix(".NS").removeSuffix("=X")
}

@Deprecated("Use formatCurrency() instead", ReplaceWith("formatCurrency(amount)"))
fun formatInr(amount: Double): String = formatCurrency(amount)

fun pnlColor(value: Double): Color = when {
    value > 0 -> Profit
    value < 0 -> Loss
    else -> Color.Gray
}

@Composable
fun StatCard(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun PnlText(value: Double, showSign: Boolean = true) {
    val sign = if (value >= 0 && showSign) "+" else ""
    Text(
        text = "$sign${formatCurrency(value)}",
        color = pnlColor(value),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
}

@Composable
fun PnlPctText(value: Double) {
    val sign = if (value >= 0) "+" else ""
    Text(
        text = "$sign${String.format("%.2f", value)}%",
        color = pnlColor(value),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
}

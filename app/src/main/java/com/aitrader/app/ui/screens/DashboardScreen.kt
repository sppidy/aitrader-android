package com.aitrader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aitrader.app.AppPreferences
import com.aitrader.app.model.Position
import com.aitrader.app.model.Trade
import com.aitrader.app.navigation.normalizeSymbolInput
import com.aitrader.app.ui.components.cleanSymbol
import com.aitrader.app.ui.components.formatCurrency
import com.aitrader.app.ui.neon.*
import com.aitrader.app.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    vm: DashboardViewModel = viewModel(),
    onOpenChart: (String) -> Unit = {},
) {
    val summary by vm.summary.collectAsState()
    val positions by vm.positions.collectAsState()
    val recentTrades by vm.recentTrades.collectAsState()
    val autopilot by vm.autopilot.collectAsState()
    val regime by vm.regime.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val actionMessage by vm.actionMessage.collectAsState()
    val marketMode by AppPreferences.marketMode.collectAsState()
    val selectedPortfolio by AppPreferences.selectedPortfolio.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NeonTokens.Bg),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // ── Header ───────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "// PORTFOLIO",
                        color = NeonTokens.TextMute,
                        fontSize = 10.sp,
                        letterSpacing = 2.5.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "NEON",
                            color = NeonTokens.Text,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NeonTokens.DisplayFamily,
                            letterSpacing = (-0.5).sp,
                        )
                        Text(
                            ".",
                            color = NeonTokens.Neon,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NeonTokens.DisplayFamily,
                        )
                        Text(
                            "TRADER",
                            color = NeonTokens.Text,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NeonTokens.DisplayFamily,
                            letterSpacing = (-0.5).sp,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Tag(text = marketMode, color = NeonTokens.Neon)
                    if (marketMode == "NSE") {
                        Tag(
                            text = if (selectedPortfolio == "eval") "EVAL" else "MAIN",
                            color = NeonTokens.TextMute,
                        )
                    }
                }
            }
        }

        // ── Error banner ─────────────────────────────────────
        error?.let { msg ->
            item { BannerPanel(msg, NeonTokens.Bear) }
        }

        // ── Action message ───────────────────────────────────
        actionMessage?.let { msg ->
            item {
                Panel(
                    modifier = Modifier.fillMaxWidth(),
                    accent = NeonTokens.BorderHot,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            msg,
                            color = NeonTokens.Text,
                            fontSize = 12.sp,
                            fontFamily = NeonTokens.MonoFamily,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "[X]",
                            color = NeonTokens.Neon,
                            fontSize = 10.sp,
                            fontFamily = NeonTokens.MonoFamily,
                            modifier = Modifier.clickable { vm.clearAction() }.padding(start = 8.dp),
                        )
                    }
                }
            }
        }

        // ── Hero: Total Value ───────────────────────────────
        summary?.let { s ->
            item {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Box {
                        GridBackground(
                            modifier = Modifier.matchParentSize(),
                            cellSize = 24.dp,
                            alpha = 0.04f,
                        )
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "TOTAL VALUE",
                                    color = NeonTokens.TextMute,
                                    fontSize = 10.sp,
                                    letterSpacing = 2.sp,
                                    fontFamily = NeonTokens.MonoFamily,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    LiveDot(color = NeonTokens.Neon, size = 6.dp)
                                    Text(
                                        "LIVE",
                                        color = NeonTokens.Neon,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = NeonTokens.MonoFamily,
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                formatCurrency(s.totalValue),
                                color = NeonTokens.Text,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = NeonTokens.MonoFamily,
                                letterSpacing = (-0.5).sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                PnlText(value = s.totalReturnPct, pct = true, size = 13)
                                Text(
                                    "${if (s.realizedPnl >= 0) "+" else ""}${formatCurrency(s.realizedPnl)} realized",
                                    color = NeonTokens.TextMute,
                                    fontSize = 11.sp,
                                    fontFamily = NeonTokens.MonoFamily,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Autopilot ───────────────────────────────────────
        item {
            val running = autopilot?.running == true
            Panel(
                modifier = Modifier.fillMaxWidth(),
                accent = if (running) NeonTokens.BorderHot else NeonTokens.Border,
                glow = running,
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .border(1.dp, if (running) NeonTokens.Neon else NeonTokens.Border),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "[CPU]",
                                    color = if (running) NeonTokens.Neon else NeonTokens.TextMute,
                                    fontSize = 8.sp,
                                    fontFamily = NeonTokens.MonoFamily,
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        "AUTOPILOT",
                                        color = NeonTokens.Text,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp,
                                        fontFamily = NeonTokens.MonoFamily,
                                    )
                                    if (running) LiveDot(color = NeonTokens.Neon, size = 6.dp)
                                }
                                Text(
                                    if (running)
                                        "CYCLE #${autopilot?.cycle ?: 0} • EVERY ${autopilot?.interval ?: 15}M"
                                    else "AGENT IDLE",
                                    color = NeonTokens.TextMute,
                                    fontSize = 10.sp,
                                    fontFamily = NeonTokens.MonoFamily,
                                )
                            }
                        }
                        NeonButton(
                            text = if (running) "STOP" else "START",
                            variant = if (running) NeonButtonVariant.Bear else NeonButtonVariant.Primary,
                            small = true,
                            onClick = { if (running) vm.stopAutopilot() else vm.startAutopilot() },
                        )
                    }
                }
            }
        }

        // ── Quick stats row ─────────────────────────────────
        summary?.let { s ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatTile(
                        "CASH",
                        formatCurrency(s.cash),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        "REALIZED",
                        formatCurrency(s.realizedPnl),
                        color = if (s.realizedPnl >= 0) NeonTokens.Bull else NeonTokens.Bear,
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        "REGIME",
                        regime,
                        color = when (regime) {
                            "BULL" -> NeonTokens.Bull
                            "BEAR" -> NeonTokens.Bear
                            else -> NeonTokens.Amber
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // ── Quick actions ───────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeonButton(
                    text = "AI TRADE",
                    onClick = { vm.runTrade(useAi = true) },
                    variant = NeonButtonVariant.Primary,
                    modifier = Modifier.weight(1f),
                )
                NeonButton(
                    text = "RULE TRADE",
                    onClick = { vm.runTrade(useAi = false) },
                    variant = NeonButtonVariant.Ghost,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        // Force-rescan (NSE only feature). Neon muted button.
        if (marketMode == "NSE") {
            item {
                NeonButton(
                    text = "RESCAN WATCHLIST",
                    onClick = { vm.forceWatchlistScan() },
                    variant = NeonButtonVariant.Muted,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        // ── Positions ───────────────────────────────────────
        if (positions.isNotEmpty()) {
            item {
                SectionLabel(
                    text = "positions",
                    right = "${positions.size} OPEN",
                )
            }
            items(positions) { pos ->
                PositionRowNeon(pos = pos, onOpenChart = onOpenChart)
            }
        }

        // ── Recent trades ──────────────────────────────────
        if (recentTrades.isNotEmpty()) {
            item { SectionLabel(text = "recent trades", right = "LAST ${minOf(5, recentTrades.size)}") }
            item {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        recentTrades.takeLast(5).reversed().forEachIndexed { i, trade ->
                            TradeRow(trade, firstRow = i == 0, onOpenChart = onOpenChart)
                        }
                    }
                }
            }
        }
    }
}

// ─── Subcomponents ────────────────────────────────────────────

@Composable
private fun BannerPanel(msg: String, color: Color) {
    Panel(
        modifier = Modifier.fillMaxWidth(),
        accent = color,
    ) {
        Text(
            msg,
            modifier = Modifier.padding(12.dp),
            color = color,
            fontSize = 12.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier, color: Color = NeonTokens.Text) {
    Panel(modifier = modifier) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                label,
                color = NeonTokens.TextMute,
                fontSize = 8.sp,
                letterSpacing = 1.5.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = color,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NeonTokens.MonoFamily,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PositionRowNeon(pos: Position, onOpenChart: (String) -> Unit) {
    Panel(
        modifier = Modifier.fillMaxWidth().clickable { onOpenChart(pos.symbol) },
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, NeonTokens.Border),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    cleanSymbol(pos.symbol).take(3),
                    color = NeonTokens.Neon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        cleanSymbol(pos.symbol),
                        color = NeonTokens.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${pos.quantity}@${"%.2f".format(pos.avgPrice)}",
                        color = NeonTokens.TextDim,
                        fontSize = 9.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        formatCurrency(pos.currentPrice),
                        color = NeonTokens.TextMute,
                        fontSize = 11.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    PnlText(value = pos.pnlPct, pct = true, size = 10)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                PnlText(value = pos.pnl, size = 12)
            }
        }
    }
}

@Composable
private fun TradeRow(trade: Trade, firstRow: Boolean, onOpenChart: (String) -> Unit) {
    val side = trade.side ?: ""
    val isBuy = side.equals("BUY", ignoreCase = true)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (firstRow) Modifier else Modifier.border(width = 1.dp, color = Color.Transparent))
            .clickable { onOpenChart(trade.symbol) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(if (isBuy) NeonTokens.BullSoft else NeonTokens.BearSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isBuy) "B" else "S",
                    color = if (isBuy) NeonTokens.Bull else NeonTokens.Bear,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            Column {
                Text(
                    cleanSymbol(trade.symbol),
                    color = NeonTokens.Text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val px = trade.entryPrice ?: trade.exitPrice ?: 0.0
                Text(
                    "${trade.quantity} @ ${"%.2f".format(px)}",
                    color = NeonTokens.TextDim,
                    fontSize = 9.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
        PnlText(value = trade.pnl)
    }
}

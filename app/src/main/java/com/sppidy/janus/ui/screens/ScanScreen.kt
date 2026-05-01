package com.sppidy.janus.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sppidy.janus.model.Signal
import com.sppidy.janus.ui.components.cleanSymbol
import com.sppidy.janus.ui.components.formatCurrency
import com.sppidy.janus.ui.neon.*
import com.sppidy.janus.viewmodel.ScanViewModel
import com.sppidy.janus.viewmodel.isActionableForApply

@Composable
fun ScanScreen(vm: ScanViewModel = viewModel()) {
    val signals by vm.signals.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val error by vm.error.collectAsState()
    val scanType by vm.scanType.collectAsState()
    val applyMessage by vm.applyMessage.collectAsState()
    val applyError by vm.applyError.collectAsState()
    val applyingSymbols by vm.applyingSymbols.collectAsState()

    var filter by remember { mutableStateOf("ALL") }

    val filtered = remember(signals, filter) {
        when (filter) {
            "BUY" -> signals.filter { it.signal.uppercase() == "BUY" }
            "SELL" -> signals.filter { it.signal.uppercase() == "SELL" }
            "HOLD" -> signals.filter { it.signal.uppercase() == "HOLD" }
            else -> signals
        }.sortedByDescending { it.confidence ?: 0.0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NeonTokens.Bg),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ──────────────────────────────────────────
        item {
            Column {
                Text(
                    "// MARKET SCAN",
                    color = NeonTokens.TextMute,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Text(
                    "Watchlist",
                    color = NeonTokens.Text,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.DisplayFamily,
                )
            }
        }

        // ── Scan trigger ────────────────────────────────────
        item {
            Panel(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                "${if (scanType == "AI") "AI scan" else "Rule scan"} · ${signals.size} signals",
                                color = NeonTokens.Text,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                            Text(
                                if (isLoading) "Scanning watchlist..." else "Tap scan to refresh",
                                color = NeonTokens.TextMute,
                                fontSize = 9.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NeonButton(
                                text = if (isLoading) "SCANNING" else "AI SCAN",
                                onClick = { vm.runScan(useAi = true) },
                                variant = NeonButtonVariant.Primary,
                                small = true,
                                enabled = !isLoading,
                            )
                            NeonButton(
                                text = "RULE",
                                onClick = { vm.runScan(useAi = false) },
                                variant = NeonButtonVariant.Ghost,
                                small = true,
                                enabled = !isLoading,
                            )
                        }
                    }
                    if (isLoading) {
                        Spacer(Modifier.height(10.dp))
                        ScanProgressBar()
                    }
                }
            }
        }

        // ── Errors / messages ───────────────────────────────
        error?.let { msg -> item { ScanBanner(msg, NeonTokens.Bear) } }
        applyError?.let { msg -> item { ScanBanner(msg, NeonTokens.Bear) } }
        applyMessage?.let { msg -> item { ScanBanner(msg, NeonTokens.Neon) } }

        // ── Filter chips ────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                listOf("ALL", "BUY", "HOLD", "SELL").forEach { f ->
                    FilterChip(
                        text = f,
                        selected = filter == f,
                        onClick = { filter = f },
                    )
                }
            }
        }

        // ── Empty state ─────────────────────────────────────
        if (!isLoading && signals.isEmpty() && error == null) {
            item {
                Panel(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "[ NO SIGNALS ]",
                            color = NeonTokens.TextDim,
                            fontSize = 12.sp,
                            letterSpacing = 2.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "tap AI SCAN to analyse watchlist",
                            color = NeonTokens.TextMute,
                            fontSize = 10.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
            }
        }

        // ── Results ─────────────────────────────────────────
        items(filtered) { signal ->
            ScanRowNeon(
                signal = signal,
                isApplying = applyingSymbols.contains(signal.symbol),
                showApply = scanType == "AI" && signal.isActionableForApply(),
                onApply = { vm.applySignal(signal) },
            )
        }
    }
}

@Composable
private fun ScanProgressBar() {
    val transition = rememberInfiniteTransition(label = "scan")
    val offset by transition.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
        ),
        label = "offset",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(NeonTokens.BgElev2)
            .drawBehind {
                val barW = size.width * 0.4f
                val x = size.width * offset - barW / 2
                val brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, NeonTokens.Neon, Color.Transparent),
                    startX = x,
                    endX = x + barW,
                )
                drawRect(
                    brush,
                    topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(barW, size.height),
                )
            }
    )
}

@Composable
private fun ScanBanner(msg: String, color: Color) {
    Panel(modifier = Modifier.fillMaxWidth(), accent = color) {
        Text(
            msg,
            modifier = Modifier.padding(12.dp),
            color = color,
            fontSize = 11.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) NeonTokens.Neon else Color.Transparent)
            .border(1.dp, if (selected) NeonTokens.Neon else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text,
            color = if (selected) Color.Black else NeonTokens.TextMute,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun ScanRowNeon(
    signal: Signal,
    isApplying: Boolean,
    showApply: Boolean,
    onApply: () -> Unit,
) {
    val action = signal.signal.uppercase()
    val actColor = when (action) {
        "BUY" -> NeonTokens.Bull
        "SELL" -> NeonTokens.Bear
        else -> NeonTokens.Amber
    }
    val confidence = signal.confidence ?: 0.0
    Panel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Action color bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(60.dp)
                    .background(actColor),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        cleanSymbol(signal.symbol),
                        color = NeonTokens.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Tag(text = action, color = actColor, small = true)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "conf ${"%.2f".format(confidence)}",
                        color = NeonTokens.TextDim,
                        fontSize = 9.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                }
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    signal.price?.let {
                        Text(
                            formatCurrency(it),
                            color = NeonTokens.TextMute,
                            fontSize = 11.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    signal.reason?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            color = NeonTokens.TextDim,
                            fontSize = 9.sp,
                            fontFamily = NeonTokens.MonoFamily,
                            maxLines = 1,
                        )
                    }
                }
                // Confidence bar
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(NeonTokens.BgElev2),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(confidence.toFloat().coerceIn(0f, 1f))
                            .height(4.dp)
                            .background(actColor.copy(alpha = 0.7f)),
                    )
                }
                // SL / Target
                if (signal.stopLoss != null || signal.target != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        signal.stopLoss?.let {
                            Text(
                                "SL ${formatCurrency(it)}",
                                color = NeonTokens.Bear,
                                fontSize = 10.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                        signal.target?.let {
                            Text(
                                "TP ${formatCurrency(it)}",
                                color = NeonTokens.Bull,
                                fontSize = 10.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                    }
                }
                if (showApply) {
                    Spacer(Modifier.height(8.dp))
                    NeonButton(
                        text = if (isApplying) "APPLYING..." else "APPLY $action",
                        onClick = onApply,
                        variant = if (action == "BUY") NeonButtonVariant.Bull else NeonButtonVariant.Bear,
                        small = true,
                        enabled = !isApplying,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

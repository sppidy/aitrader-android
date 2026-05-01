package com.sppidy.janus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.model.Candle
import com.sppidy.janus.model.DirectTradeRequest
import com.sppidy.janus.navigation.normalizeSymbolInput
import com.sppidy.janus.repository.TradingRepository
import com.sppidy.janus.ui.neon.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

private data class ChartTimeframe(val label: String, val apiValue: String)

private val CHART_TIMEFRAMES = listOf(
    ChartTimeframe("5m", "5m"),
    ChartTimeframe("15m", "15m"),
    ChartTimeframe("1h", "1h"),
    ChartTimeframe("1D", "1D"),
    ChartTimeframe("1W", "1W"),
    ChartTimeframe("1M", "1M"),
    ChartTimeframe("1Y", "1Y"),
)

@Composable
fun ChartScreen(
    initialSymbol: String? = null,
    showBack: Boolean = true,
    showSearch: Boolean = false,
    onBack: () -> Unit = {},
) {
    val normalizedInitialSymbol = remember(initialSymbol) { normalizeSymbolInput(initialSymbol.orEmpty()) }
    val defaultSymbol = if (AppPreferences.getMarketMode() == "FOREX") "EURUSD=X" else "SBIN"
    // In search mode (top-level Chart tab) start empty so user chooses a symbol.
    // From a position/trade push use the provided symbol, or the default as last resort.
    var symbol by rememberSaveable(normalizedInitialSymbol, showSearch) {
        mutableStateOf(
            when {
                normalizedInitialSymbol.isNotBlank() -> normalizedInitialSymbol
                showSearch -> ""
                else -> defaultSymbol
            }
        )
    }
    var tfLabel by rememberSaveable { mutableStateOf("1h") }
    var lineMode by rememberSaveable { mutableStateOf(false) }
    var showSma by rememberSaveable { mutableStateOf(true) }
    var showEma by rememberSaveable { mutableStateOf(false) }
    var candles by remember { mutableStateOf<List<Candle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refreshTick by remember { mutableStateOf(0) }
    var showBuyDialog by remember { mutableStateOf(false) }
    var showSellDialog by remember { mutableStateOf(false) }
    var tradeQuantity by remember { mutableStateOf("") }
    var tradeLoading by remember { mutableStateOf(false) }
    var tradeMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Pan / zoom state on candle window
    var visibleCount by rememberSaveable { mutableStateOf(100) }
    var scrollOffset by rememberSaveable { mutableStateOf(0) }
    var crosshairIndex by remember { mutableStateOf<Int?>(null) }

    val tf = CHART_TIMEFRAMES.firstOrNull { it.label == tfLabel } ?: CHART_TIMEFRAMES[2]

    // Auto-refresh every 30s
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            refreshTick++
        }
    }

    LaunchedEffect(symbol, tf.apiValue, refreshTick) {
        if (symbol.isBlank()) {
            candles = emptyList()
            isLoading = false
            error = null
            return@LaunchedEffect
        }
        if (refreshTick == 0) isLoading = true
        error = null
        try {
            val resp = TradingRepository.getCandles(symbol, tf.apiValue, 300)
            candles = resp.candles.orEmpty()
            if (candles.isEmpty()) error = "No chart data for $symbol"
            scrollOffset = 0
        } catch (e: Exception) {
            if (candles.isEmpty()) error = "Failed to load chart: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // Slice visible candles based on pan/zoom state
    val visibleCandles = remember(candles, visibleCount, scrollOffset) {
        if (candles.isEmpty()) emptyList()
        else {
            val end = (candles.size - scrollOffset).coerceIn(1, candles.size)
            val start = (end - visibleCount).coerceAtLeast(0)
            candles.subList(start, end)
        }
    }

    val last = visibleCandles.lastOrNull()
    val first = visibleCandles.firstOrNull()
    val changePct = if (last != null && first != null && first.c > 0) ((last.c - first.c) / first.c) * 100 else null
    val chartHigh = visibleCandles.maxOfOrNull { it.h } ?: 0.0
    val chartLow = visibleCandles.minOfOrNull { it.l } ?: 0.0
    val currencySymbol = AppPreferences.currencySymbol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonTokens.Bg)
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Header ──────────────────────────────────────────
        ChartHeader(
            symbol = symbol,
            showBack = showBack,
            showSearch = showSearch,
            onBack = onBack,
            onSymbolChange = { newSym ->
                val n = normalizeSymbolInput(newSym)
                if (n.isNotBlank() && n != symbol) {
                    symbol = n
                    scrollOffset = 0
                    crosshairIndex = null
                }
            },
        )

        // Empty state when search mode without a symbol yet
        if (symbol.isBlank()) {
            EmptySearchState()
            return@Column
        }

        // ── Big price panel ─────────────────────────────────
        Panel(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.fillMaxWidth()) {
                GridBackground(modifier = Modifier.matchParentSize(), alpha = 0.04f)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            "$currencySymbol${"%.2f".format(last?.c ?: 0.0)}",
                            color = NeonTokens.Text,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = NeonTokens.MonoFamily,
                            letterSpacing = (-0.5).sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (changePct != null) PnlText(value = changePct, pct = true, size = 13)
                            val delta = if (last != null && first != null) last.c - first.c else 0.0
                            Text(
                                "${if (delta >= 0) "+" else ""}${"%.2f".format(delta)} · ${tf.label}",
                                color = NeonTokens.TextMute,
                                fontSize = 10.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("H / L", color = NeonTokens.TextMute, fontSize = 9.sp, letterSpacing = 1.5.sp, fontFamily = NeonTokens.MonoFamily)
                        Text("%.2f".format(chartHigh), color = NeonTokens.Text, fontSize = 11.sp, fontFamily = NeonTokens.MonoFamily)
                        Text("%.2f".format(chartLow), color = NeonTokens.TextMute, fontSize = 11.sp, fontFamily = NeonTokens.MonoFamily)
                    }
                }
            }
        }

        // ── Timeframe pills ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            CHART_TIMEFRAMES.forEach { t ->
                TfPill(label = t.label, selected = t.label == tfLabel) {
                    tfLabel = t.label
                    scrollOffset = 0
                    crosshairIndex = null
                }
            }
        }

        // ── Chart canvas + pan/zoom gestures ─────────────────
        Panel(modifier = Modifier.fillMaxWidth(), fill = NeonTokens.Bg) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .padding(10.dp)
                    .pointerInput(candles.size, tf.label) {
                        if (candles.isEmpty()) return@pointerInput
                        detectTransformGestures { _, pan, zoom, _ ->
                            // Zoom (pinch) — adjust visibleCount
                            if (zoom != 1f) {
                                visibleCount = (visibleCount / zoom).roundToInt().coerceIn(20, minOf(300, candles.size))
                            }
                            // Pan — shift scrollOffset; positive pan.x moves chart right (shows older data)
                            val pxPerCandle = size.width.toFloat() / visibleCount.coerceAtLeast(1)
                            val deltaCandles = (pan.x / pxPerCandle).roundToInt()
                            if (deltaCandles != 0) {
                                scrollOffset = (scrollOffset + deltaCandles).coerceIn(0, candles.size - visibleCount)
                            }
                        }
                    }
                    .pointerInput(candles.size, visibleCandles.size) {
                        if (visibleCandles.isEmpty()) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val startX = down.position.x
                            val startY = down.position.y
                            val stepX = size.width.toFloat() / visibleCandles.size
                            crosshairIndex = (startX / stepX).toInt().coerceIn(0, visibleCandles.size - 1)
                            do {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    val dx = abs(change.position.x - startX)
                                    val dy = abs(change.position.y - startY)
                                    // Only hold crosshair if mostly static
                                    if (dx < 14f && dy < 14f) {
                                        crosshairIndex = (change.position.x / stepX).toInt().coerceIn(0, visibleCandles.size - 1)
                                    } else {
                                        crosshairIndex = null
                                        break
                                    }
                                } else {
                                    break
                                }
                            } while (true)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    isLoading && candles.isEmpty() -> Text(
                        "LOADING...",
                        color = NeonTokens.TextMute,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    error != null -> Text(
                        error!!,
                        color = NeonTokens.Bear,
                        fontSize = 10.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    visibleCandles.size >= 2 -> Candlestick(
                        candles = visibleCandles,
                        modifier = Modifier.fillMaxSize(),
                        accent = NeonTokens.Neon,
                        showSma = showSma,
                        showEma = showEma,
                        showVolume = !lineMode,
                        lineMode = lineMode,
                        crosshairIndex = crosshairIndex,
                    )
                }

                // Crosshair info box (top-left overlay)
                if (crosshairIndex != null && crosshairIndex!! < visibleCandles.size) {
                    val c = visibleCandles[crosshairIndex!!]
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(NeonTokens.Bg.copy(alpha = 0.85f))
                            .border(1.dp, NeonTokens.Neon)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            Text(
                                "O ${"%.2f".format(c.o)} H ${"%.2f".format(c.h)}",
                                color = NeonTokens.Text,
                                fontSize = 9.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                            Text(
                                "L ${"%.2f".format(c.l)} C ${"%.2f".format(c.c)}",
                                color = if (c.c >= c.o) NeonTokens.Bull else NeonTokens.Bear,
                                fontSize = 9.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                    }
                }
            }
        }

        // ── Chart tools row ─────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ModeToggle(label = "Candles", selected = !lineMode) { lineMode = false }
                ModeToggle(label = "Line", selected = lineMode) { lineMode = true }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IndicatorToggle(
                    label = "SMA20",
                    selected = showSma,
                    color = NeonTokens.Amber,
                ) { showSma = !showSma }
                IndicatorToggle(
                    label = "EMA20",
                    selected = showEma,
                    color = NeonTokens.Cyan,
                ) { showEma = !showEma }
            }
        }

        // Zoom +/- + reset
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ToolIconButton("−", "Zoom out") {
                visibleCount = (visibleCount * 1.3).roundToInt().coerceAtMost(minOf(300, candles.size.coerceAtLeast(1)))
            }
            ToolIconButton("+", "Zoom in") {
                visibleCount = (visibleCount / 1.3).roundToInt().coerceAtLeast(20)
            }
            ToolIconButton("«", "Pan older") {
                scrollOffset = (scrollOffset + 20).coerceAtMost((candles.size - visibleCount).coerceAtLeast(0))
            }
            ToolIconButton("»", "Pan newer") {
                scrollOffset = (scrollOffset - 20).coerceAtLeast(0)
            }
            ToolIconButton("◉", "Reset view") {
                visibleCount = 100
                scrollOffset = 0
                crosshairIndex = null
            }
            Spacer(Modifier.weight(1f))
            Text(
                "${visibleCandles.size}/${candles.size}",
                color = NeonTokens.TextDim,
                fontSize = 9.sp,
                fontFamily = NeonTokens.MonoFamily,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }

        // ── AI commentary ──────────────────────────────────
        AiCommentary(candles)

        // ── Buy / Sell ──────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NeonButton(
                text = "BUY",
                onClick = { tradeQuantity = ""; showBuyDialog = true },
                variant = NeonButtonVariant.Bull,
                large = true,
                enabled = !tradeLoading,
                modifier = Modifier.weight(1f),
            )
            NeonButton(
                text = "SELL",
                onClick = { tradeQuantity = ""; showSellDialog = true },
                variant = NeonButtonVariant.Bear,
                large = true,
                enabled = !tradeLoading,
                modifier = Modifier.weight(1f),
            )
        }

        tradeMessage?.let { msg ->
            Panel(modifier = Modifier.fillMaxWidth(), accent = if (msg.startsWith("Error")) NeonTokens.Bear else NeonTokens.Neon) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    color = if (msg.startsWith("Error")) NeonTokens.Bear else NeonTokens.Text,
                    fontSize = 11.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }

        // ── Signals grid ────────────────────────────────────
        SectionLabel(text = "signals")
        val rsi = computeRsi(candles)
        val macd = computeMacdBias(candles)
        val vol = last?.v ?: 0L
        val vola = computeVolatility(candles)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SignalTile("RSI(14)", if (rsi == null) "—" else "%.1f".format(rsi), rsiColor(rsi), Modifier.weight(1f))
            SignalTile("MACD", macd.first, macd.second, Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SignalTile("VOL", fmtVol(vol), NeonTokens.Text, Modifier.weight(1f))
            SignalTile("VOLATILITY", if (vola == null) "—" else "%.2f".format(vola), NeonTokens.Text, Modifier.weight(1f))
        }
    }

    if (showBuyDialog) {
        TradeDialog(
            title = "BUY ${symbol.removeSuffix(".NS").removeSuffix("=X")}",
            price = last?.c,
            quantity = tradeQuantity,
            onQuantityChange = { tradeQuantity = it.filter { c -> c.isDigit() } },
            variant = NeonButtonVariant.Bull,
            confirmText = "CONFIRM BUY",
            hint = "Leave empty for auto-size",
            currencySymbol = currencySymbol,
            onDismiss = { showBuyDialog = false },
            onConfirm = {
                if (tradeLoading) return@TradeDialog
                val qty = tradeQuantity.toIntOrNull()
                if (qty != null && qty <= 0) {
                    tradeMessage = "Error: quantity must be positive"
                    return@TradeDialog
                }
                tradeLoading = true
                showBuyDialog = false
                scope.launch {
                    try {
                        val resp = TradingRepository.executeTrade(
                            DirectTradeRequest(symbol = symbol, action = "buy", quantity = qty)
                        )
                        tradeMessage = if (resp.status == "filled") {
                            "Bought ${resp.quantity ?: ""} @ $currencySymbol${"%.2f".format(resp.fillPrice ?: 0.0)}"
                        } else "Error: ${resp.message ?: "buy failed"}"
                    } catch (e: Exception) {
                        tradeMessage = "Error: ${e.message}"
                    }
                    tradeLoading = false
                }
            },
        )
    }

    if (showSellDialog) {
        TradeDialog(
            title = "SELL ${symbol.removeSuffix(".NS").removeSuffix("=X")}",
            price = last?.c,
            quantity = tradeQuantity,
            onQuantityChange = { tradeQuantity = it.filter { c -> c.isDigit() } },
            variant = NeonButtonVariant.Bear,
            confirmText = "CONFIRM SELL",
            hint = "Leave empty to close entire position",
            currencySymbol = currencySymbol,
            onDismiss = { showSellDialog = false },
            onConfirm = {
                if (tradeLoading) return@TradeDialog
                val qty = tradeQuantity.toIntOrNull()
                if (qty != null && qty <= 0) {
                    tradeMessage = "Error: quantity must be positive"
                    return@TradeDialog
                }
                tradeLoading = true
                showSellDialog = false
                scope.launch {
                    try {
                        val resp = TradingRepository.executeTrade(
                            DirectTradeRequest(symbol = symbol, action = "sell", quantity = qty)
                        )
                        tradeMessage = if (resp.status == "filled") {
                            val pnl = resp.pnl?.let { " P&L $currencySymbol${"%.2f".format(it)}" } ?: ""
                            "Sold ${resp.quantity ?: ""} @ $currencySymbol${"%.2f".format(resp.fillPrice ?: 0.0)}$pnl"
                        } else "Error: ${resp.message ?: "sell failed"}"
                    } catch (e: Exception) {
                        tradeMessage = "Error: ${e.message}"
                    }
                    tradeLoading = false
                }
            },
        )
    }
}

// ─── Subcomponents ────────────────────────────────────────────

@Composable
private fun EmptySearchState() {
    Panel(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            GridBackground(modifier = Modifier.matchParentSize(), alpha = 0.04f)
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    "[ SEARCH ]",
                    color = NeonTokens.TextDim,
                    fontSize = 12.sp,
                    letterSpacing = 3.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Text(
                    "type a symbol above",
                    color = NeonTokens.Text,
                    fontSize = 14.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Text(
                    if (AppPreferences.getMarketMode() == "FOREX")
                        "e.g. EURUSD · GBPUSD · USDJPY · XAUUSD"
                    else
                        "e.g. SBIN · RELIANCE · HDFCBANK · INFY",
                    color = NeonTokens.TextMute,
                    fontSize = 10.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
    }
}

@Composable
private fun ChartHeader(
    symbol: String,
    showBack: Boolean,
    showSearch: Boolean,
    onBack: () -> Unit,
    onSymbolChange: (String) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showBack) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(1.dp, NeonTokens.Border)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Text("<", color = NeonTokens.Text, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.MonoFamily)
            }
        }
        if (showSearch) {
            ChartSearchBar(
                currentSymbol = symbol,
                onCommit = onSymbolChange,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (AppPreferences.getMarketMode() == "FOREX") "// FOREX" else "// NSE.EQ",
                    color = NeonTokens.TextMute,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        symbol.removeSuffix(".NS").removeSuffix("=X"),
                        color = NeonTokens.Text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                    )
                    Tag(text = "LIVE", color = NeonTokens.Neon)
                }
            }
        }
    }
}

@Composable
private fun ChartSearchBar(
    currentSymbol: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var raw by rememberSaveable(currentSymbol) {
        mutableStateOf(currentSymbol.removeSuffix(".NS").removeSuffix("=X"))
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    Row(
        modifier = modifier
            .background(NeonTokens.BgElev1)
            .border(1.dp, NeonTokens.Border)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(">", color = NeonTokens.Neon, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.MonoFamily)
        Box(modifier = Modifier.weight(1f)) {
            if (raw.isEmpty()) {
                Text(
                    "symbol (e.g. SBIN)",
                    color = NeonTokens.TextDim,
                    fontSize = 13.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            BasicTextField(
                value = raw,
                onValueChange = { raw = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = NeonTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = NeonTokens.MonoFamily,
                ),
                cursorBrush = SolidColor(NeonTokens.Neon),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    onCommit(raw)
                    keyboardController?.hide()
                }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        Tag(text = "LIVE", color = NeonTokens.Neon)
    }
}

@Composable
private fun TfPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) NeonTokens.Neon else Color.Transparent)
            .border(1.dp, if (selected) NeonTokens.Neon else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) Color.Black else NeonTokens.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun ModeToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) NeonTokens.NeonSoft else Color.Transparent)
            .border(1.dp, if (selected) NeonTokens.Neon else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = if (selected) NeonTokens.Neon else NeonTokens.TextMute,
            fontSize = 10.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun IndicatorToggle(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) color.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (selected) color else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            color = if (selected) color else NeonTokens.TextMute,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun ToolIconButton(glyph: String, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .background(NeonTokens.BgElev1)
            .border(1.dp, NeonTokens.Border)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            glyph,
            color = NeonTokens.Text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun SignalTile(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(NeonTokens.BgElev1)
            .border(1.dp, NeonTokens.Border)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = NeonTokens.TextMute, fontSize = 9.sp, letterSpacing = 1.5.sp, fontFamily = NeonTokens.MonoFamily)
            Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.MonoFamily)
        }
    }
}

@Composable
private fun AiCommentary(candles: List<Candle>) {
    if (candles.size < 20) return
    val rsi = computeRsi(candles)
    val last = candles.last().c
    val sma = candles.takeLast(20).sumOf { it.c } / 20.0
    val bias = when {
        last > sma && (rsi ?: 50.0) in 40.0..70.0 -> "Bullish bias — above 20-SMA with neutral RSI."
        last > sma && (rsi ?: 0.0) >= 70 -> "Above 20-SMA but RSI overbought — pullback risk."
        last < sma && (rsi ?: 50.0) <= 30 -> "Oversold under 20-SMA — possible bounce."
        last < sma -> "Weak — below 20-SMA, trend reversal needed."
        else -> "Consolidating near 20-SMA — wait for breakout."
    }
    Panel(modifier = Modifier.fillMaxWidth(), accent = NeonTokens.Neon) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier.size(18.dp).border(1.dp, NeonTokens.Neon),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("*", color = NeonTokens.Neon, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.MonoFamily)
                }
                Text(
                    "AGENT.COMMENTARY",
                    color = NeonTokens.Neon,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(bias, color = NeonTokens.Text, fontSize = 11.sp, lineHeight = 16.sp, fontFamily = NeonTokens.MonoFamily)
        }
    }
}

@Composable
private fun TradeDialog(
    title: String,
    price: Double?,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    variant: NeonButtonVariant,
    confirmText: String,
    hint: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonTokens.BgElev1,
        tonalElevation = 0.dp,
        title = {
            Text(title, color = NeonTokens.Text, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.DisplayFamily, letterSpacing = 1.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (price != null) {
                    Text("Price $currencySymbol${"%.2f".format(price)}", color = NeonTokens.TextMute, fontSize = 11.sp, fontFamily = NeonTokens.MonoFamily)
                }
                Row(
                    modifier = Modifier.background(NeonTokens.Bg).border(1.dp, NeonTokens.Border).padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("QTY >", color = NeonTokens.Neon, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = NeonTokens.MonoFamily)
                    BasicTextField(
                        value = quantity,
                        onValueChange = onQuantityChange,
                        singleLine = true,
                        textStyle = TextStyle(color = NeonTokens.Text, fontSize = 14.sp, fontFamily = NeonTokens.MonoFamily),
                        cursorBrush = SolidColor(NeonTokens.Neon),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(hint, color = NeonTokens.TextDim, fontSize = 10.sp, fontFamily = NeonTokens.MonoFamily)
            }
        },
        confirmButton = { NeonButton(text = confirmText, onClick = onConfirm, variant = variant) },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = NeonTokens.TextMute, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontFamily = NeonTokens.MonoFamily)
            }
        },
    )
}

// ─── Indicator calculations ───────────────────────────────────

private fun computeRsi(candles: List<Candle>, period: Int = 14): Double? {
    if (candles.size <= period) return null
    val deltas = candles.zipWithNext { a, b -> b.c - a.c }
    val gains = deltas.map { if (it > 0) it else 0.0 }
    val losses = deltas.map { if (it < 0) -it else 0.0 }
    val avgGain = gains.takeLast(period).average()
    val avgLoss = losses.takeLast(period).average()
    if (avgLoss == 0.0) return 100.0
    val rs = avgGain / avgLoss
    return 100.0 - (100.0 / (1.0 + rs))
}

private fun computeMacdBias(candles: List<Candle>): Pair<String, Color> {
    if (candles.size < 26) return "—" to NeonTokens.TextMute
    fun ema(data: List<Double>, period: Int): Double {
        val k = 2.0 / (period + 1)
        var e = data.take(period).average()
        for (i in period until data.size) e = data[i] * k + e * (1 - k)
        return e
    }
    val closes = candles.map { it.c }
    val macd = ema(closes, 12) - ema(closes, 26)
    return when {
        macd > 0 -> "BULLISH" to NeonTokens.Bull
        macd < 0 -> "BEARISH" to NeonTokens.Bear
        else -> "NEUTRAL" to NeonTokens.Amber
    }
}

private fun computeVolatility(candles: List<Candle>, period: Int = 20): Double? {
    if (candles.size <= period) return null
    val returns = candles.takeLast(period + 1).zipWithNext { a, b ->
        if (a.c > 0) (b.c - a.c) / a.c else 0.0
    }
    val mean = returns.average()
    val variance = returns.map { (it - mean).let { d -> d * d } }.average()
    return sqrt(variance) * 100.0
}

private fun rsiColor(rsi: Double?): Color = when {
    rsi == null -> NeonTokens.TextMute
    rsi >= 70 -> NeonTokens.Bear
    rsi <= 30 -> NeonTokens.Bull
    else -> NeonTokens.Amber
}

private fun fmtVol(v: Long): String = when {
    abs(v) >= 1_000_000 -> "${"%.1f".format(v / 1_000_000.0)}M"
    abs(v) >= 1_000 -> "${"%.1f".format(v / 1_000.0)}K"
    else -> "$v"
}

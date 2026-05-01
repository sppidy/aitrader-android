package com.aitrader.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aitrader.app.AppPreferences
import com.aitrader.app.model.Position
import com.aitrader.app.repository.TradingRepository
import com.aitrader.app.ui.components.cleanSymbol
import com.aitrader.app.ui.neon.*
import kotlinx.coroutines.launch

/**
 * Manual portfolio management screen — place BUY and SELL orders directly
 * (not via the AI pipeline), and inspect open positions with per-row SELL.
 *
 * Auto-refreshes when orders succeed by re-requesting /api/status.
 */
@Composable
fun PortfolioScreen() {
    val scope = rememberCoroutineScope()

    var summary by remember { mutableStateOf<com.aitrader.app.model.PortfolioSummary?>(null) }
    var positions by remember { mutableStateOf<List<Position>>(emptyList()) }
    var watchlist by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    var buySymbol by remember { mutableStateOf("") }
    var buyQty by remember { mutableStateOf("") }
    var buyPrice by remember { mutableStateOf("") }

    // Per-position SELL qty inputs, keyed by symbol.
    val sellQtyState = remember { mutableStateMapOf<String, String>() }

    suspend fun refresh() {
        loading = true
        try {
            val status = TradingRepository.getStatus()
            summary = status.summary
            positions = status.positions.orEmpty()
            watchlist = status.watchlist.orEmpty()
            error = null
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }

    LaunchedEffect(Unit) { refresh() }

    fun placeOrder(side: String, symbol: String, qtyText: String, priceText: String) {
        val sym = cleanSymbol(symbol.trim()).uppercase()
        if (sym.isBlank()) {
            error = "Symbol required"
            return
        }
        val qty = qtyText.trim().toIntOrNull()
        val price = priceText.trim().toDoubleOrNull()
        scope.launch {
            loading = true
            try {
                val res = TradingRepository.placeOrder(sym, side, qty, price)
                lastResult = "${res.action ?: side} ${res.quantity ?: "?"}×${res.symbol ?: sym}" +
                    " @ ₹${"%.2f".format(res.price ?: 0.0)}"
                error = null
                refresh()
            } catch (e: Exception) {
                error = e.message ?: "Order failed"
                lastResult = null
            } finally {
                loading = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NeonTokens.Bg),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        // ── Header ──
        item {
            Column {
                Text(
                    "// PORTFOLIO // MANUAL",
                    color = NeonTokens.TextMute,
                    fontSize = 10.sp,
                    letterSpacing = 2.5.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "ORDER BOOK",
                        color = NeonTokens.Text,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                        letterSpacing = (-0.5).sp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(NeonTokens.Neon)
                    )
                }
            }
        }

        // ── KPIs ──
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KpiCell("TOTAL", fmtCurrency(summary?.totalValue), Modifier.weight(1f))
                KpiCell("CASH",  fmtCurrency(summary?.cash),       Modifier.weight(1f))
                KpiCell(
                    "RETURN",
                    summary?.totalReturnPct?.let { fmtPct(it) } ?: "—",
                    Modifier.weight(1f),
                    tint = when {
                        summary == null -> NeonTokens.Text
                        (summary?.totalReturnPct ?: 0.0) >= 0.0 -> NeonTokens.Bull
                        else -> NeonTokens.Bear
                    },
                )
            }
        }

        // ── Place order panel ──
        item {
            Panel(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionLabel("// PLACE ORDER")
                    Text(
                        "Quantity empty → Kelly-sized from cash. Price empty → live quote.",
                        color = NeonTokens.TextDim,
                        fontSize = 10.sp,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    FieldRow(
                        label = "SYMBOL",
                        value = buySymbol,
                        onValueChange = { buySymbol = it },
                        placeholder = "e.g. TATASTEEL.NS",
                        kbd = KeyboardType.Ascii,
                        ime = ImeAction.Next,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(1f)) {
                            FieldRow(
                                label = "QTY",
                                value = buyQty,
                                onValueChange = { buyQty = it.filter { ch -> ch.isDigit() } },
                                placeholder = "auto",
                                kbd = KeyboardType.Number,
                                ime = ImeAction.Next,
                            )
                        }
                        Box(Modifier.weight(1f)) {
                            FieldRow(
                                label = "LIMIT PRICE",
                                value = buyPrice,
                                onValueChange = {
                                    buyPrice = it.filter { ch -> ch.isDigit() || ch == '.' }
                                },
                                placeholder = "live",
                                kbd = KeyboardType.Decimal,
                                ime = ImeAction.Done,
                                onImeDone = { placeOrder("BUY", buySymbol, buyQty, buyPrice) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton(
                            text = if (loading) "…" else "BUY",
                            onClick = { placeOrder("BUY", buySymbol, buyQty, buyPrice) },
                            modifier = Modifier.weight(1f),
                            variant = NeonButtonVariant.Bull,
                            enabled = !loading,
                        )
                        NeonButton(
                            text = if (loading) "…" else "SELL",
                            onClick = { placeOrder("SELL", buySymbol, buyQty, buyPrice) },
                            modifier = Modifier.weight(1f),
                            variant = NeonButtonVariant.Bear,
                            enabled = !loading,
                        )
                    }
                    lastResult?.let {
                        Text(
                            "✓ $it",
                            color = NeonTokens.Bull,
                            fontSize = 11.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                    error?.let {
                        Text(
                            "⚠ $it",
                            color = NeonTokens.Bear,
                            fontSize = 11.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
            }
        }

        // ── Positions ──
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                SectionLabel("// OPEN POSITIONS")
                Spacer(Modifier.weight(1f))
                Text(
                    positions.size.toString(),
                    color = NeonTokens.Neon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }

        if (positions.isEmpty()) {
            item {
                Text(
                    "no open positions",
                    color = NeonTokens.TextDim,
                    fontSize = 12.sp,
                    fontFamily = NeonTokens.MonoFamily,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }

        items(positions, key = { it.symbol }) { p ->
            val qtyText = sellQtyState[p.symbol] ?: ""
            PositionRow(
                p = p,
                qtyText = qtyText,
                onQtyChange = { sellQtyState[p.symbol] = it },
                onSell = { placeOrder("SELL", p.symbol, qtyText, "") },
                onSellAll = { placeOrder("SELL", p.symbol, "", "") },
                disabled = loading,
            )
        }
    }
}

@Composable
private fun KpiCell(label: String, value: String, modifier: Modifier = Modifier,
                   tint: androidx.compose.ui.graphics.Color = NeonTokens.Text) {
    Panel(modifier) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                label, color = NeonTokens.TextMute,
                fontSize = 9.sp, letterSpacing = 2.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
            Text(
                value, color = tint,
                fontSize = 17.sp, fontWeight = FontWeight.Bold,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    kbd: KeyboardType = KeyboardType.Text,
    ime: ImeAction = ImeAction.Default,
    onImeDone: (() -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            label,
            color = NeonTokens.TextMute,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
        Spacer(Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonTokens.BgElev2)
                .border(1.dp, NeonTokens.Border)
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                cursorBrush = SolidColor(NeonTokens.Neon),
                textStyle = TextStyle(
                    color = NeonTokens.Text,
                    fontSize = 13.sp,
                    fontFamily = NeonTokens.MonoFamily,
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = kbd,
                    imeAction = ime,
                    autoCorrectEnabled = false,
                ),
                keyboardActions = KeyboardActions(onDone = { onImeDone?.invoke() }),
            )
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    color = NeonTokens.TextDim,
                    fontSize = 13.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
    }
}

@Composable
private fun PositionRow(
    p: Position,
    qtyText: String,
    onQtyChange: (String) -> Unit,
    onSell: () -> Unit,
    onSellAll: () -> Unit,
    disabled: Boolean,
) {
    Panel(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cleanSymbol(p.symbol),
                    color = NeonTokens.Text,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = NeonTokens.MonoFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                PnlText(p.pnlPct, pct = true, size = 12)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                KV("QTY",  p.quantity.toString())
                KV("AVG",  "%.2f".format(p.avgPrice))
                KV("LAST", "%.2f".format(p.currentPrice))
                KV("P&L",  "%.0f".format(p.pnl),
                   tint = if (p.pnl >= 0) NeonTokens.Bull else NeonTokens.Bear)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(NeonTokens.BgElev2)
                            .border(1.dp, NeonTokens.Border)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                    ) {
                        BasicTextField(
                            value = qtyText,
                            onValueChange = { v -> onQtyChange(v.filter { it.isDigit() }) },
                            singleLine = true,
                            cursorBrush = SolidColor(NeonTokens.Neon),
                            textStyle = TextStyle(
                                color = NeonTokens.Text,
                                fontSize = 12.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                        )
                        if (qtyText.isEmpty()) {
                            Text(
                                "qty (default ALL)",
                                color = NeonTokens.TextDim,
                                fontSize = 12.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                    }
                }
                NeonButton(
                    text = "SELL",
                    onClick = onSell,
                    variant = NeonButtonVariant.Bear,
                    small = true,
                    enabled = !disabled,
                )
                NeonButton(
                    text = "ALL",
                    onClick = onSellAll,
                    variant = NeonButtonVariant.Ghost,
                    small = true,
                    enabled = !disabled,
                )
            }
        }
    }
}

@Composable
private fun KV(label: String, value: String,
               tint: androidx.compose.ui.graphics.Color = NeonTokens.Text) {
    Column {
        Text(
            label,
            color = NeonTokens.TextMute,
            fontSize = 8.sp,
            letterSpacing = 1.8.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
        Text(
            value,
            color = tint,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

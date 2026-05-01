package com.sppidy.janus.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sppidy.janus.ui.neon.*
import com.sppidy.janus.viewmodel.LogViewModel

@Composable
fun LogScreen(vm: LogViewModel = viewModel()) {
    val logs by vm.logs.collectAsState()
    val isConnected by vm.isConnected.collectAsState()
    val autoScroll by vm.autoScroll.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val availableDates by vm.availableDates.collectAsState()

    var levelFilter by remember { mutableStateOf("ALL") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, levelFilter) {
        if (levelFilter == "ALL") logs else logs.filter { logLevel(it) == levelFilter }
    }

    LaunchedEffect(filteredLogs.size, autoScroll) {
        if (autoScroll && filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(NeonTokens.Bg),
    ) {
        // ── Header ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    "// STREAM.TTY",
                    color = NeonTokens.TextMute,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "Live Logs",
                        color = NeonTokens.Text,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                    )
                    LiveDot(
                        color = if (isConnected) NeonTokens.Bull else NeonTokens.Amber,
                        size = 6.dp,
                    )
                }
            }
            NeonButton(
                text = if (autoScroll) "AUTO-SCROLL" else "MANUAL",
                onClick = { vm.toggleAutoScroll() },
                variant = NeonButtonVariant.Muted,
                small = true,
            )
        }
        Divider()

        // ── Date selector ──────────────────────────────────
        if (availableDates.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                availableDates.take(7).forEach { date ->
                    val sel = date == selectedDate
                    Box(
                        modifier = Modifier
                            .background(if (sel) NeonTokens.Cyan.copy(alpha = 0.15f) else Color.Transparent)
                            .border(1.dp, if (sel) NeonTokens.Cyan else NeonTokens.Border)
                            .clickable { vm.selectDate(date) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            date.takeLast(5),
                            color = if (sel) NeonTokens.Cyan else NeonTokens.TextMute,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
            }
        }

        // ── Level filter chips ─────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            listOf("ALL", "INFO", "OK", "WARN", "ERR", "TRADE").forEach { f ->
                LevelFilterChip(f, levelFilter == f) { levelFilter = f }
            }
        }
        Divider()

        // ── Log body ───────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(NeonTokens.Bg)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            items(filteredLogs) { line ->
                LogLine(line)
            }
            item {
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Cursor(color = NeonTokens.Neon, size = 10.dp)
                }
            }
        }

        // ── Status bar ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(NeonTokens.BgElev1)
                .padding(horizontal = 14.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${filteredLogs.size} LINES",
                color = NeonTokens.TextMute,
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
            Text(
                if (isConnected) "STREAMING" else "DISCONNECTED",
                color = if (isConnected) NeonTokens.Bull else NeonTokens.Bear,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
    }
}

@Composable
private fun LevelFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val col = levelColor(text)
    Box(
        modifier = Modifier
            .background(if (selected) col else Color.Transparent)
            .border(1.dp, if (selected) col else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            color = if (selected) Color.Black else col,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun LogLine(line: String) {
    val level = logLevel(line)
    val (ts, msg) = splitTimestamp(line)
    val col = levelColor(level)
    Row(
        modifier = Modifier.padding(vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (ts.isNotEmpty()) {
            Text(
                ts,
                color = NeonTokens.TextDim,
                fontSize = 10.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
        Text(
            "[$level]",
            color = col,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = NeonTokens.MonoFamily,
            modifier = Modifier.widthIn(min = 54.dp),
        )
        Text(
            msg,
            color = NeonTokens.Text,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            fontFamily = NeonTokens.MonoFamily,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(NeonTokens.Border),
    )
}

// ─── Level parsing ────────────────────────────────────────────
private fun logLevel(line: String): String {
    val up = line.uppercase()
    return when {
        up.contains("ERROR") || up.contains("FAIL") || up.contains("EXCEPTION") || up.contains("CRITICAL") -> "ERR"
        up.contains("WARN") -> "WARN"
        up.contains("TRADE") || up.contains("BUY ") || up.contains("SELL ") || up.contains("FILLED") -> "TRADE"
        up.contains(" OK") || up.contains("SUCCESS") || up.contains("COMPLETED") -> "OK"
        else -> "INFO"
    }
}

private fun splitTimestamp(line: String): Pair<String, String> {
    val tsRegex = Regex("""^(\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}(?:[.,]\d+)?)\s+(.+)$""")
    tsRegex.matchEntire(line)?.let { m ->
        return m.groupValues[1].takeLast(8) to m.groupValues[2]
    }
    val shortTsRegex = Regex("""^(\d{2}:\d{2}:\d{2}(?:[.,]\d+)?)\s+(.+)$""")
    shortTsRegex.matchEntire(line)?.let { m ->
        return m.groupValues[1].take(8) to m.groupValues[2]
    }
    return "" to line
}

private fun levelColor(level: String): Color = when (level) {
    "INFO" -> NeonTokens.Cyan
    "OK" -> NeonTokens.Bull
    "WARN" -> NeonTokens.Amber
    "ERR" -> NeonTokens.Bear
    "TRADE" -> NeonTokens.Neon
    else -> NeonTokens.Text
}

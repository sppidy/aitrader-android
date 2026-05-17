package com.sppidy.janus.model

import com.google.gson.annotations.SerializedName

// ── API Responses ──

data class StatusResponse(
    val status: String,
    @SerializedName("agent_name") val agentName: String? = null,
    val summary: PortfolioSummary?,
    val positions: List<Position>?,
    @SerializedName("recent_trades") val recentTrades: List<Trade>?,
    val watchlist: List<String>?,
    val autopilot: AutopilotInfo?,
    val message: String?
)

data class PortfolioSummary(
    val cash: Double,
    @SerializedName("positions_value") val positionsValue: Double,
    @SerializedName("total_value") val totalValue: Double,
    @SerializedName("initial_capital") val initialCapital: Double,
    @SerializedName("total_return_pct") val totalReturnPct: Double,
    @SerializedName("realized_pnl") val realizedPnl: Double,
    @SerializedName("open_positions") val openPositions: Int,
    @SerializedName("total_trades") val totalTrades: Int
)

data class Position(
    val symbol: String,
    val quantity: Int,
    @SerializedName("avg_price") val avgPrice: Double,
    @SerializedName("current_price") val currentPrice: Double,
    val pnl: Double,
    @SerializedName("pnl_pct") val pnlPct: Double,
    @SerializedName("highest_price") val highestPrice: Double,
    @SerializedName("entry_time") val entryTime: String
)

data class Trade(
    val symbol: String,
    val side: String?,
    val quantity: Int,
    @SerializedName("entry_price") val entryPrice: Double?,
    @SerializedName("exit_price") val exitPrice: Double?,
    val pnl: Double,
    @SerializedName("pnl_pct") val pnlPct: Double,
    val timestamp: String?
)

data class AutopilotInfo(
    val running: Boolean,
    val cycle: Int,
    @SerializedName("started_at") val startedAt: String?,
    val interval: Int,
    val pid: Int? = null
)

// ── Scan ──

data class ScanResponse(
    val status: String,
    val signals: List<Signal>?,
    val message: String?,
    @SerializedName("job_id") val jobId: String? = null
)

data class ScanJobStatusResponse(
    val status: String,
    val signals: List<Signal>?,
    val error: String?
)

data class Signal(
    val symbol: String,
    val signal: String,
    val price: Double?,
    val confidence: Double?,
    val reason: String?,
    @SerializedName("stop_loss") val stopLoss: Double?,
    val target: Double?
)

data class ApplySignalPayload(
    val symbol: String,
    val signal: String,
    val price: Double? = null,
    val confidence: Double? = null,
    val reason: String? = null,
    @SerializedName("stop_loss") val stopLoss: Double? = null,
    val target: Double? = null,
    @SerializedName("position_size_pct") val positionSizePct: Double? = null
)

data class ApplySignalsRequest(
    val signals: List<ApplySignalPayload>,
    @SerializedName("min_confidence") val minConfidence: Double = 0.0
)

// ── Trade execution ──

data class TradeResponse(
    val status: String,
    val trades: List<TradeAction>?,
    val summary: PortfolioSummary?,
    val message: String?
)

data class TradeAction(
    val action: String,
    val symbol: String,
    val price: Double
)

// ── Autopilot ──

data class AutopilotRequest(
    val interval: Int = 15,
    @SerializedName("use_ai") val useAi: Boolean = true,
    val force: Boolean = false
)

data class AutopilotResponse(
    val status: String,
    val message: String?
)

data class WatchlistResponse(
    val status: String,
    val watchlist: List<String> = emptyList(),
    @SerializedName("stale_counts") val staleCounts: Map<String, Int> = emptyMap(),
    @SerializedName("hold_cycles") val holdCycles: Map<String, Int> = emptyMap(),
    val added: List<String> = emptyList(),
    @SerializedName("updated_at") val updatedAt: String? = null
)

// ── Trade request ──

data class TradeRequest(
    @SerializedName("use_ai") val useAi: Boolean = true
)

data class DirectTradeRequest(
    val symbol: String,
    val action: String,
    val quantity: Int? = null
)

data class DirectTradeResponse(
    val status: String,
    val symbol: String? = null,
    @SerializedName("fill_price") val fillPrice: Double? = null,
    val quantity: Int? = null,
    val pnl: Double? = null,
    val message: String? = null
)

// ── Manual order (BUY/SELL) ──

data class OrderRequest(
    val symbol: String,
    val side: String,          // "BUY" or "SELL"
    val quantity: Int? = null,
    val price: Double? = null,
    val portfolio: String? = null,
)

data class OrderResponse(
    val status: String,
    val action: String?,
    val symbol: String?,
    val quantity: Int?,
    val price: Double?,
    val portfolio: String?,
    val summary: PortfolioSummary?,
    val message: String? = null,
)

// ── Logs ──

data class LogDatesResponse(
    val status: String,
    val dates: List<String>?,
    val message: String?
)

data class LogsResponse(
    val status: String,
    val logs: List<String>?,
    val date: String?,
    val message: String?
)

// ── Journal ──

data class JournalResponse(
    val status: String,
    val entries: List<JournalEntry>?,
    val message: String?
)

data class JournalEntry(
    val id: Int? = null,
    val symbol: String?,
    val action: String?,
    val price: Double?,
    val quantity: Int?,
    val timestamp: String?,
    @SerializedName("ai_signal") val aiSignal: Map<String, Any>?,
    val outcome: Map<String, Any>?
)

// ── Market regime ──

data class RegimeResponse(
    val status: String,
    val regime: String?,
    val message: String?
)

// ── Prices ──

data class PricesResponse(
    val status: String,
    val prices: Map<String, Double>?,
    val message: String?
)

data class Candle(
    val t: String,
    val o: Double,
    val h: Double,
    val l: Double,
    val c: Double,
    val v: Long,
)

data class CandlesResponse(
    val status: String,
    val symbol: String?,
    val timeframe: String?,
    val candles: List<Candle>?,
    val message: String? = null
)

// ── Chat ──

data class ChatRequest(
    val message: String,
    val history: List<ChatHistoryItem>? = null
)

data class ChatHistoryItem(
    val role: String,
    val content: String
)

data class ChatResponse(
    val status: String,
    val reply: String?,
    val action: String?,
    val message: String?,
    @SerializedName("job_id") val jobId: String? = null
)

data class ChatJobStatusResponse(
    val status: String,
    val reply: String?,
    val action: String?,
    val error: String?
)

// ── WebSocket log message ──

data class LogMessage(
    val type: String,
    val message: String,
    val timestamp: String?
)

package com.aitrader.app.repository

import com.aitrader.app.AppPreferences
import com.aitrader.app.api.ApiService
import com.aitrader.app.db.AppDatabase
import com.aitrader.app.db.CacheEntity
import com.aitrader.app.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TradingRepository {
    private val STATUS_CACHE_KEY: String
        get() {
            val mode = AppPreferences.getMarketMode()
            return if (mode == "NSE") {
                "status_NSE_${AppPreferences.getSelectedPortfolio()}"
            } else {
                "status_$mode"
            }
        }
    private val SCAN_SIGNALS_CACHE_KEY: String
        get() = "scan_signals_${AppPreferences.getMarketMode()}"

    private val api: ApiService
        get() = ApiService.create()
        
    private val db by lazy { AppDatabase.getDatabase(AppPreferences.context) }
    private val dao by lazy { db.cacheDao() }
    private val gson = Gson()

    suspend fun getCachedStatus(): StatusResponse? {
        return try {
            val cache = dao.getCache(STATUS_CACHE_KEY)
            if (cache != null) {
                gson.fromJson(cache.jsonPayload, StatusResponse::class.java)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStatus(portfolio: String? = null): StatusResponse {
        // Only send ?portfolio= for NSE. The forex backend doesn't know about it,
        // and passing an unknown query breaks nothing but adds noise to logs.
        val q = if (AppPreferences.getMarketMode() == "NSE") {
            (portfolio ?: AppPreferences.getSelectedPortfolio()).takeIf { it.isNotBlank() }
        } else null
        val response = api.getStatus(q)
        if (response.status == "ok") {
            try {
                // STATUS_CACHE_KEY already encodes marketMode + selectedPortfolio,
                // so switching portfolios uses a different cache slot automatically.
                dao.insertCache(CacheEntity(STATUS_CACHE_KEY, gson.toJson(response)))
            } catch (e: Exception) {}
        }
        return response
    }

    suspend fun cacheScanSignals(signals: List<Signal>) {
        try {
            dao.insertCache(CacheEntity(SCAN_SIGNALS_CACHE_KEY, gson.toJson(signals)))
        } catch (_: Exception) {}
    }

    suspend fun getCachedScanSignals(): List<Signal>? {
        return try {
            val cache = dao.getCache(SCAN_SIGNALS_CACHE_KEY) ?: return null
            val type = object : TypeToken<List<Signal>>() {}.type
            gson.fromJson<List<Signal>>(cache.jsonPayload, type)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun placeOrder(
        symbol: String,
        side: String,
        quantity: Int? = null,
        price: Double? = null,
    ): OrderResponse {
        val portfolio = if (AppPreferences.getMarketMode() == "NSE")
            AppPreferences.getSelectedPortfolio().takeIf { it.isNotBlank() }
        else null
        return api.placeOrder(
            OrderRequest(
                symbol = symbol,
                side = side,
                quantity = quantity,
                price = price,
                portfolio = portfolio,
            ),
        )
    }

    suspend fun getPrices(): PricesResponse = api.getPrices()
    suspend fun getMarketRegime(): RegimeResponse = api.getMarketRegime()
    suspend fun getCandles(symbol: String, timeframe: String, limit: Int = 300): CandlesResponse =
        api.getCandles(symbol, timeframe, limit)
    suspend fun runScan(): ScanResponse = api.runScan()
    suspend fun runAiScan(): ScanResponse = api.runAiScan()
    suspend fun getScanJobStatus(jobId: String): ScanJobStatusResponse = api.getScanJobStatus(jobId)
    suspend fun runTrade(request: TradeRequest): TradeResponse = api.runTrade(request)
    suspend fun executeTrade(request: DirectTradeRequest): DirectTradeResponse = api.executeTrade(request)
    suspend fun applyAiSignals(request: ApplySignalsRequest): TradeResponse = api.applyAiSignals(request)
    suspend fun startAutopilot(request: AutopilotRequest): AutopilotResponse = api.startAutopilot(request)
    suspend fun stopAutopilot(): AutopilotResponse = api.stopAutopilot()
    suspend fun forceWatchlistScan(): WatchlistResponse = api.forceWatchlistScan()
    suspend fun getWatchlist(): WatchlistResponse = api.getWatchlist()
    suspend fun getJournal(portfolio: String? = null): JournalResponse {
        val q = if (AppPreferences.getMarketMode() == "NSE") {
            (portfolio ?: AppPreferences.getSelectedPortfolio()).takeIf { it.isNotBlank() }
        } else null
        return api.getJournal(q)
    }
    suspend fun getLogDates(): LogDatesResponse = api.getLogDates()
    suspend fun getRecentLogs(lines: Int = 500, date: String? = null): LogsResponse = api.getRecentLogs(lines, date)
    suspend fun getTrainingLog(): Map<String, Any> = api.getTrainingLog()
    suspend fun chat(request: ChatRequest): ChatResponse = api.chat(request)
    suspend fun getChatJobStatus(jobId: String): ChatJobStatusResponse = api.getChatJobStatus(jobId)
}

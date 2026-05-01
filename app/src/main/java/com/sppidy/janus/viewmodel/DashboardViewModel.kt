package com.sppidy.janus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.repository.TradingRepository
import com.sppidy.janus.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = TradingRepository
    private var lastMarketMode: String = AppPreferences.getMarketMode()
    private var lastPortfolio: String = AppPreferences.getSelectedPortfolio()

    private val _summary = MutableStateFlow<PortfolioSummary?>(null)
    val summary: StateFlow<PortfolioSummary?> = _summary

    private val _positions = MutableStateFlow<List<Position>>(emptyList())
    val positions: StateFlow<List<Position>> = _positions

    private val _recentTrades = MutableStateFlow<List<Trade>>(emptyList())
    val recentTrades: StateFlow<List<Trade>> = _recentTrades

    private val _autopilot = MutableStateFlow<AutopilotInfo?>(null)
    val autopilot: StateFlow<AutopilotInfo?> = _autopilot

    private val _regime = MutableStateFlow("--")
    val regime: StateFlow<String> = _regime

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage

    init {
        refresh()
        // Auto-refresh every 30 seconds
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                refresh()
            }
        }
        // Watch for market mode changes — clear stale data and refetch
        viewModelScope.launch {
            AppPreferences.marketMode.collectLatest { mode ->
                if (mode != lastMarketMode) {
                    lastMarketMode = mode
                    _summary.value = null
                    _positions.value = emptyList()
                    _recentTrades.value = emptyList()
                    _autopilot.value = null
                    _regime.value = "--"
                    _error.value = null
                    refresh()
                }
            }
        }
        // Watch for portfolio toggle (NSE only) — reset state and refetch for new portfolio.
        viewModelScope.launch {
            AppPreferences.selectedPortfolio.collectLatest { portfolio ->
                if (portfolio != lastPortfolio) {
                    lastPortfolio = portfolio
                    _summary.value = null
                    _positions.value = emptyList()
                    _recentTrades.value = emptyList()
                    _error.value = null
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            var hasCache = false
            try {
                val cached = repository.getCachedStatus()
                if (cached != null) {
                    hasCache = true
                    _summary.value = cached.summary
                    _positions.value = cached.positions ?: emptyList()
                    _recentTrades.value = cached.recentTrades ?: emptyList()
                    _autopilot.value = cached.autopilot
                }
            } catch (e: Exception) {}

            try {
                val response = repository.getStatus()
                if (response.status == "ok") {
                    _summary.value = response.summary
                    _positions.value = response.positions ?: emptyList()
                    _recentTrades.value = response.recentTrades ?: emptyList()
                    _autopilot.value = response.autopilot
                } else {
                    if (!hasCache) _error.value = response.message ?: "Unknown error"
                }
            } catch (e: java.net.ConnectException) {
                if (!hasCache) _error.value = "Server unreachable — check if your VPN is on"
            } catch (e: java.net.SocketTimeoutException) {
                if (!hasCache) _error.value = "Connection timed out — is your VPN active?"
            } catch (e: Exception) {
                if (!hasCache) _error.value = "Connection failed: ${e.message}"
            }
            _isLoading.value = false
        }

        viewModelScope.launch {
            try {
                val r = repository.getMarketRegime()
                if (r.status == "ok") _regime.value = r.regime ?: "UNKNOWN"
            } catch (_: Exception) {}
        }
    }

    fun startAutopilot(interval: Int = 15, useAi: Boolean = true, force: Boolean = false) {
        viewModelScope.launch {
            try {
                val resp = repository.startAutopilot(AutopilotRequest(interval, useAi, force))
                _actionMessage.value = resp.message
                delay(1000)
                refresh()
            } catch (e: Exception) {
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun stopAutopilot() {
        viewModelScope.launch {
            try {
                val resp = repository.stopAutopilot()
                _actionMessage.value = resp.message
                delay(1000)
                refresh()
            } catch (e: Exception) {
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun runTrade(useAi: Boolean = true) {
        viewModelScope.launch {
            _actionMessage.value = "Running trade cycle..."
            try {
                val resp = repository.runTrade(TradeRequest(useAi))
                val count = resp.trades?.size ?: 0
                _actionMessage.value = if (count > 0) {
                    "$count trade(s) executed"
                } else {
                    "No trades triggered"
                }
                delay(500)
                refresh()
            } catch (e: java.net.SocketTimeoutException) {
                _actionMessage.value = "Trade request timed out — AI cycle may still be running, try again in a moment."
            } catch (e: Exception) {
                _actionMessage.value = "Error: ${e.message}"
            }
        }
    }

    fun forceWatchlistScan() {
        viewModelScope.launch {
            _actionMessage.value = "Scanning NSE for trending stocks..."
            try {
                val resp = repository.forceWatchlistScan()
                val added = resp.added
                val total = resp.watchlist.size
                _actionMessage.value = if (added.isNotEmpty()) {
                    "Watchlist refreshed: +${added.size} (${added.take(3).joinToString()}${if (added.size > 3) "…" else ""}). Now $total stocks."
                } else {
                    "Scan complete — no rotation triggered. Watchlist: $total stocks."
                }
            } catch (e: java.net.SocketTimeoutException) {
                _actionMessage.value = "Scan timed out — yfinance may be slow. Try again in a moment."
            } catch (e: Exception) {
                _actionMessage.value = "Scan failed: ${e.message}"
            }
        }
    }

    fun clearAction() {
        _actionMessage.value = null
    }
}

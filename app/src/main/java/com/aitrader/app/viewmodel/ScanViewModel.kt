package com.aitrader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitrader.app.AppPreferences
import com.aitrader.app.repository.TradingRepository
import com.aitrader.app.model.Signal
import com.aitrader.app.model.ApplySignalPayload
import com.aitrader.app.model.ApplySignalsRequest
import com.aitrader.app.service.BackgroundMonitorService
import com.aitrader.app.util.PollingPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

class ScanViewModel : ViewModel() {

    private val repository = TradingRepository

    private val _signals = MutableStateFlow<List<Signal>>(emptyList())
    val signals: StateFlow<List<Signal>> = _signals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _applyMessage = MutableStateFlow<String?>(null)
    val applyMessage: StateFlow<String?> = _applyMessage

    private val _applyError = MutableStateFlow<String?>(null)
    val applyError: StateFlow<String?> = _applyError

    private val _applyingSymbols = MutableStateFlow<Set<String>>(emptySet())
    val applyingSymbols: StateFlow<Set<String>> = _applyingSymbols

    private val _scanType = MutableStateFlow("AI")
    val scanType: StateFlow<String> = _scanType
    private val scanPollingPolicy = PollingPolicy(maxAttempts = 45)

    init {
        viewModelScope.launch {
            val cached = repository.getCachedScanSignals()
            if (!cached.isNullOrEmpty()) {
                _signals.value = cached
            }
        }
    }

    fun runScan(useAi: Boolean = true) {
        _scanType.value = if (useAi) "AI" else "Rule"
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _applyMessage.value = null
            _applyError.value = null
            try {
                val resp = if (useAi) repository.runAiScan() else repository.runScan()
                
                if (resp.status == "accepted" && resp.jobId != null) {
                    AppPreferences.setActiveScanJobId(resp.jobId)
                    BackgroundMonitorService.start(AppPreferences.context)
                    // Poll for job completion with timeout/backoff.
                    var jobDone = false
                    var attempts = 0
                    var waitMs = scanPollingPolicy.initialDelayMs
                    while (!jobDone) {
                        if (!currentCoroutineContext().isActive) break
                        if (attempts >= scanPollingPolicy.maxAttempts) {
                            _error.value = "Scan timeout: server is taking too long"
                            AppPreferences.clearActiveScanJobId()
                            break
                        }
                        delay(waitMs)
                        val statusResp = repository.getScanJobStatus(resp.jobId)
                        when (statusResp.status) {
                            "completed" -> {
                                val completedSignals = statusResp.signals ?: emptyList()
                                _signals.value = completedSignals
                                repository.cacheScanSignals(completedSignals)
                                AppPreferences.clearActiveScanJobId()
                                if (completedSignals.isEmpty()) {
                                    _error.value = statusResp.error ?: "Scan completed but returned no signals"
                                }
                                jobDone = true
                            }
                            "failed" -> {
                                _error.value = statusResp.error ?: "Scan failed"
                                AppPreferences.clearActiveScanJobId()
                                jobDone = true
                            }
                            else -> {
                                attempts += 1
                                waitMs = scanPollingPolicy.nextDelay(waitMs)
                            }
                        }
                    }
                } else if (resp.status == "ok") {
                    val completedSignals = resp.signals ?: emptyList()
                    _signals.value = completedSignals
                    repository.cacheScanSignals(completedSignals)
                    AppPreferences.clearActiveScanJobId()
                } else {
                    _error.value = resp.message ?: "Scan failed"
                    AppPreferences.clearActiveScanJobId()
                }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
                AppPreferences.clearActiveScanJobId()
            }
            _isLoading.value = false
        }
    }

    fun applySignal(signal: Signal) {
        if (!signal.isActionableForApply()) return
        if (_applyingSymbols.value.contains(signal.symbol)) return

        viewModelScope.launch {
            _applyMessage.value = null
            _applyError.value = null
            _applyingSymbols.value = _applyingSymbols.value + signal.symbol
            try {
                val payload = signal.toApplySignalPayload()
                val resp = repository.applyAiSignals(
                    ApplySignalsRequest(
                        signals = listOf(payload),
                        minConfidence = 0.0
                    )
                )
                if (resp.status == "ok") {
                    val trade = resp.trades?.firstOrNull()
                    _applyMessage.value = if (trade != null) {
                        "${trade.action} applied for ${trade.symbol.removeSuffix(".NS").removeSuffix("=X")}"
                    } else {
                        "Signal applied for ${signal.symbol.removeSuffix(".NS").removeSuffix("=X")}"
                    }
                    _signals.value = _signals.value.filterNot { it.symbol == signal.symbol && it.signal == signal.signal }
                } else {
                    _applyError.value = resp.message ?: "Apply failed"
                }
            } catch (e: Exception) {
                _applyError.value = "Apply failed: ${e.message}"
            }
            _applyingSymbols.value = _applyingSymbols.value - signal.symbol
        }
    }
}

internal fun Signal.isActionableForApply(): Boolean {
    return signal.uppercase() == "BUY" || signal.uppercase() == "SELL"
}

internal fun Signal.toApplySignalPayload(): ApplySignalPayload {
    return ApplySignalPayload(
        symbol = symbol,
        signal = signal.uppercase(),
        price = price,
        confidence = confidence,
        reason = reason,
        stopLoss = stopLoss,
        target = target
    )
}

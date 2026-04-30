package com.aitrader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitrader.app.repository.TradingRepository
import com.aitrader.app.api.LogWebSocket
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LogViewModel : ViewModel() {

    private val repository = TradingRepository
    private val logWebSocket = LogWebSocket()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter

    private val _availableDates = MutableStateFlow<List<String>>(emptyList())
    val availableDates: StateFlow<List<String>> = _availableDates

    private val _selectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val selectedDate: StateFlow<String> = _selectedDate

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val maxLines = 1000

    val isViewingToday: Boolean
        get() = _selectedDate.value == LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    init {
        loadAvailableDates()
        loadRecentLogs()
        connectWebSocket()
    }

    private fun loadAvailableDates() {
        viewModelScope.launch {
            try {
                val resp = repository.getLogDates()
                if (resp.status == "ok" && resp.dates != null) {
                    _availableDates.value = resp.dates
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadRecentLogs(date: String? = null) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val resp = repository.getRecentLogs(500, date)
                if (resp.status == "ok" && resp.logs != null) {
                    _logs.value = resp.logs
                }
            } catch (_: Exception) {}
            _isLoading.value = false
        }
    }

    private fun connectWebSocket() {
        logWebSocket.connect()

        viewModelScope.launch {
            logWebSocket.connectionState.collect { state ->
                _isConnected.value = state == LogWebSocket.ConnectionState.CONNECTED
            }
        }

        viewModelScope.launch {
            logWebSocket.messages.collect { msg ->
                // Only append live messages when viewing today
                if (!isViewingToday) return@collect

                val current = _logs.value.toMutableList()
                current.add(msg.message)
                if (current.size > maxLines) {
                    _logs.value = current.takeLast(maxLines)
                } else {
                    _logs.value = current
                }
            }
        }
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
        _logs.value = emptyList()
        loadRecentLogs(date)
    }

    fun setFilter(text: String) {
        _filter.value = text
    }

    fun toggleAutoScroll() {
        _autoScroll.value = !_autoScroll.value
    }

    fun refreshLogs() {
        loadAvailableDates()
        loadRecentLogs(if (isViewingToday) null else _selectedDate.value)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun reconnect() {
        logWebSocket.disconnect()
        _logs.value = emptyList()
        _selectedDate.value = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        loadAvailableDates()
        loadRecentLogs()
        connectWebSocket()
    }

    override fun onCleared() {
        super.onCleared()
        logWebSocket.disconnect()
    }
}

package com.aitrader.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aitrader.app.repository.TradingRepository
import com.aitrader.app.model.ChatHistoryItem
import com.aitrader.app.model.ChatRequest
import com.aitrader.app.util.PollingPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

data class ChatMessage(
    val role: String,  // "user" or "assistant"
    val content: String,
    val isAction: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val repository = TradingRepository

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = if (com.aitrader.app.AppPreferences.getMarketMode() == "FOREX")
                    "Hey! I'm your forex trading assistant. Ask me about currency pairs, your portfolio, market sessions — or use commands like `buy EURUSD 1000` or `sell GBPUSD`."
                else
                    "Hey! I'm your AI trading assistant. Ask me about stocks, your portfolio, market conditions — or use commands like `buy SBIN 10` or `sell ITC`."
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val chatPollingPolicy = PollingPolicy(maxAttempts = 45)

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(role = "user", content = text.trim())
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val history = _messages.value
                    .filter { it.role in listOf("user", "assistant") && !it.isAction }
                    .takeLast(10)
                    .map { ChatHistoryItem(role = it.role, content = it.content) }

                val resp = repository.chat(ChatRequest(message = text.trim(), history = history))

                if (resp.status == "accepted" && resp.jobId != null) {
                    var jobDone = false
                    var attempts = 0
                    var waitMs = chatPollingPolicy.initialDelayMs
                    while (!jobDone) {
                        if (!currentCoroutineContext().isActive) break
                        if (attempts >= chatPollingPolicy.maxAttempts) {
                            _messages.value = _messages.value + ChatMessage(
                                role = "assistant",
                                content = "Request timed out. Please try again."
                            )
                            break
                        }
                        delay(waitMs)
                        val statusResp = repository.getChatJobStatus(resp.jobId)
                        when (statusResp.status) {
                            "completed" -> {
                                val isCmd = statusResp.action == "command"
                                _messages.value = _messages.value + ChatMessage(
                                    role = "assistant",
                                    content = statusResp.reply ?: "No reply.",
                                    isAction = isCmd
                                )
                                jobDone = true
                            }
                            "failed" -> {
                                _messages.value = _messages.value + ChatMessage(
                                    role = "assistant",
                                    content = "Connection error: ${statusResp.error}"
                                )
                                jobDone = true
                            }
                            else -> {
                                attempts += 1
                                waitMs = chatPollingPolicy.nextDelay(waitMs)
                            }
                        }
                    }
                } else if (resp.status == "ok" && resp.reply != null) {
                    val isCmd = resp.action == "command"
                    _messages.value = _messages.value + ChatMessage(
                        role = "assistant",
                        content = resp.reply,
                        isAction = isCmd
                    )
                } else {
                    _messages.value = _messages.value + ChatMessage(
                        role = "assistant",
                        content = resp.message ?: "Something went wrong."
                    )
                }
            } catch (e: Exception) {
                _messages.value = _messages.value + ChatMessage(
                    role = "assistant",
                    content = "Connection error: ${e.message}"
                )
            }
            _isLoading.value = false
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                role = "assistant",
                content = "Chat cleared. How can I help?"
            )
        )
    }
}

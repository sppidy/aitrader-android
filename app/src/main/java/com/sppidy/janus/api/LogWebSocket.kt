package com.sppidy.janus.api

import android.os.Handler
import android.os.Looper
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.model.LogMessage
import com.google.gson.Gson
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket client that connects to the backend and emits log lines as a Flow.
 */
class LogWebSocket {
    /** Read URL fresh each connect so market mode switches take effect. */
    private val wsUrl: String
        get() = "${AppPreferences.wsUrl}/ws/logs"
    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No timeout for WebSocket
        .pingInterval(30, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val reconnectHandler = Handler(Looper.getMainLooper())
    private var reconnectAttempts = 0
    private var shouldReconnect = true

    private val _messages = MutableSharedFlow<LogMessage>(
        replay = 0,
        extraBufferCapacity = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<LogMessage> = _messages

    private val _connectionState = MutableSharedFlow<ConnectionState>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val connectionState: SharedFlow<ConnectionState> = _connectionState

    enum class ConnectionState {
        CONNECTING, CONNECTED, DISCONNECTED, ERROR
    }

    fun connect() {
        shouldReconnect = true
        reconnectAttempts = 0
        doConnect()
    }

    private fun doConnect() {
        _connectionState.tryEmit(ConnectionState.CONNECTING)

        val request = Request.Builder()
            .url(wsUrl)
            .addHeader("X-API-Key", AppPreferences.apiKey)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                reconnectAttempts = 0
                _connectionState.tryEmit(ConnectionState.CONNECTED)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val msg = gson.fromJson(text, LogMessage::class.java)
                    if (msg.type == "log") {
                        _messages.tryEmit(msg)
                    }
                } catch (_: Exception) {
                    // Ignore malformed messages
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                _connectionState.tryEmit(ConnectionState.DISCONNECTED)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _connectionState.tryEmit(ConnectionState.ERROR)
                scheduleReconnect()
            }
        })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return
        reconnectAttempts++
        val delay = minOf(10_000L * (1L shl minOf(reconnectAttempts - 1, 4)), 60_000L)
        reconnectHandler.postDelayed({ if (shouldReconnect) doConnect() }, delay)
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectHandler.removeCallbacksAndMessages(null)
        webSocket?.close(1000, "Client closing")
        webSocket = null
        _connectionState.tryEmit(ConnectionState.DISCONNECTED)
    }

    fun sendPing() {
        webSocket?.send("ping")
    }
}

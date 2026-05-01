package com.sppidy.janus.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.MainActivity
import com.sppidy.janus.R
import com.sppidy.janus.repository.TradingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class BackgroundMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var lastScanStatusLogAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        AppPreferences.init(applicationContext)
        createChannels()
        startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification())
        startMonitoringLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!AppPreferences.isBackgroundMonitorEnabled()) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    pollScanStatus()
                } catch (_: Exception) {}
                try {
                    pollJournalForTradeNotifications()
                } catch (_: Exception) {}
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollScanStatus() {
        val jobId = AppPreferences.getActiveScanJobId() ?: return
        val status = TradingRepository.getScanJobStatus(jobId)
        when (status.status.lowercase()) {
            "completed" -> {
                val signals = status.signals ?: emptyList()
                TradingRepository.cacheScanSignals(signals)
                AppPreferences.clearActiveScanJobId()
                showEventNotification(
                    id = EVENT_SCAN_COMPLETE_ID,
                    title = "AI Scan completed",
                    text = if (signals.isEmpty()) "Scan finished with no signals." else "Scan finished with ${signals.size} signal(s).",
                )
            }
            "failed" -> {
                AppPreferences.clearActiveScanJobId()
                showEventNotification(
                    id = EVENT_SCAN_FAILED_ID,
                    title = "AI Scan failed",
                    text = status.error ?: "Scan did not complete. Open app to retry.",
                )
            }
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastScanStatusLogAtMs > SCAN_STATUS_REFRESH_MS) {
                    lastScanStatusLogAtMs = now
                    val manager = NotificationManagerCompat.from(this)
                    manager.notify(ONGOING_NOTIFICATION_ID, buildOngoingNotification("Monitoring scan and trade events..."))
                }
            }
        }
    }

    private suspend fun pollJournalForTradeNotifications() {
        val response = TradingRepository.getJournal()
        if (response.status != "ok") return
        val entries = response.entries.orEmpty()
        if (entries.isEmpty()) return

        var maxActionId = AppPreferences.getLastJournalActionId()
        entries.sortedBy { it.id ?: Int.MAX_VALUE }.forEach { entry ->
            val id = entry.id ?: return@forEach
            val action = entry.action?.uppercase() ?: ""

            if (id > maxActionId && (action == "BUY" || action == "SELL")) {
                showActionNotification(entryId = id, action = action, symbol = entry.symbol, qty = entry.quantity, price = entry.price)
                maxActionId = maxOf(maxActionId, id)
            }

            val outcome = entry.outcome
            if (outcome != null && !AppPreferences.isOutcomeNotified(id)) {
                showOutcomeNotification(symbol = entry.symbol, outcome = outcome)
                AppPreferences.markOutcomeNotified(id)
            }
        }

        if (maxActionId > AppPreferences.getLastJournalActionId()) {
            AppPreferences.setLastJournalActionId(maxActionId)
        }
    }

    private fun showActionNotification(entryId: Int, action: String, symbol: String?, qty: Int?, price: Double?) {
        val cleanSymbol = (symbol ?: "--").removeSuffix(".NS").removeSuffix("=X")
        val quantityPart = qty?.let { "${it}x " } ?: ""
        val pricePart = price?.let { " @ ${AppPreferences.currencySymbol}${"%.2f".format(it)}" } ?: ""
        val title = if (action == "BUY") "BUY executed" else "SELL executed"
        val text = "$quantityPart$cleanSymbol$pricePart"
        showEventNotification(EVENT_BASE_ACTION_ID + entryId, title, text)
    }

    private fun showOutcomeNotification(symbol: String?, outcome: Map<String, Any>) {
        val cleanSymbol = (symbol ?: "--").removeSuffix(".NS").removeSuffix("=X")
        val result = outcome["result"]?.toString()?.uppercase()
        val pnl = toDouble(outcome["pnl"])
        val pnlPct = toDouble(outcome["pnl_pct"])

        val isProfit = result == "WIN" || (pnl != null && pnl >= 0.0)
        val title = if (isProfit) "Profit booked" else "Stop-loss / loss exit"
        val pnlText = buildString {
            if (pnl != null) {
                val sign = if (pnl >= 0.0) "+" else "-"
                append("$sign ${AppPreferences.currencySymbol}${"%.2f".format(abs(pnl))}")
            }
            if (pnlPct != null) {
                if (isNotEmpty()) append(" | ")
                append(if (pnlPct >= 0.0) "+" else "-")
                append("${"%.2f".format(abs(pnlPct))}%")
            }
        }.ifBlank { "Trade closed for $cleanSymbol" }

        showEventNotification(
            id = EVENT_BASE_OUTCOME_ID + cleanSymbol.hashCode().let { abs(it % 100000) },
            title = title,
            text = "$cleanSymbol  $pnlText",
        )
    }

    private fun toDouble(value: Any?): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    private fun showEventNotification(id: Int, title: String, text: String) {
        if (!hasNotificationPermission()) return
        val notification = NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(appLaunchIntent())
            .build()
        NotificationManagerCompat.from(this).notify(id, notification)
    }

    private fun buildOngoingNotification(content: String = "Monitoring AI scans and trade activity") =
        NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("AI Trader background service")
            .setContentText(content)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(appLaunchIntent())
            .build()

    private fun appLaunchIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val ongoing = NotificationChannel(
            ONGOING_CHANNEL_ID,
            "Background Service",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Persistent service status for scan/trade monitoring."
            setShowBadge(false)
        }
        val events = NotificationChannel(
            EVENT_CHANNEL_ID,
            "Trade & Scan Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Alerts for BUY/SELL, stop-loss, profit booking, and scan completion."
        }
        manager.createNotificationChannel(ongoing)
        manager.createNotificationChannel(events)
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val ONGOING_CHANNEL_ID = "ai_trader_bg_service"
        private const val EVENT_CHANNEL_ID = "ai_trader_trade_events"
        private const val ONGOING_NOTIFICATION_ID = 1001
        private const val EVENT_SCAN_COMPLETE_ID = 2001
        private const val EVENT_SCAN_FAILED_ID = 2002
        private const val EVENT_BASE_ACTION_ID = 300000
        private const val EVENT_BASE_OUTCOME_ID = 400000
        private const val POLL_INTERVAL_MS = 15_000L
        private const val SCAN_STATUS_REFRESH_MS = 60_000L

        fun start(service: Service) {
            val context = service.applicationContext
            start(context)
        }

        fun start(context: android.content.Context) {
            val intent = Intent(context, BackgroundMonitorService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (_: SecurityException) {
            } catch (_: IllegalStateException) {
            }
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, BackgroundMonitorService::class.java))
        }
    }
}

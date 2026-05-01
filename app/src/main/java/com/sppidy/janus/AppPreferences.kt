package com.sppidy.janus

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persisted app settings via SharedPreferences.
 * Singleton — initialize once from Application/Activity context.
 */
object AppPreferences {

    private const val PREFS_NAME = "ai_trader_prefs"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_WS_URL = "ws_url"
    private const val KEY_API_KEY = "api_key"
    private const val KEY_CONNECTIVITY_ALERTS = "connectivity_alerts"
    private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"
    private const val KEY_BACKGROUND_MONITOR_ENABLED = "background_monitor_enabled"
    private const val KEY_ACTIVE_SCAN_JOB_ID = "active_scan_job_id"
    private const val KEY_LAST_JOURNAL_ACTION_ID = "last_journal_action_id"
    private const val KEY_NOTIFIED_OUTCOME_IDS = "notified_outcome_ids"
    private const val KEY_MARKET_MODE = "market_mode"
    private const val KEY_FOREX_SERVER_URL = "forex_server_url"
    private const val KEY_FOREX_API_KEY = "forex_api_key"
    private const val KEY_SELECTED_PORTFOLIO = "selected_portfolio"

    private lateinit var prefs: SharedPreferences
    lateinit var context: Context

    private val _connectivityAlerts = MutableStateFlow(true)
    val connectivityAlerts: StateFlow<Boolean> = _connectivityAlerts
    private val _appLockEnabled = MutableStateFlow(false)
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled
    private val _backgroundMonitorEnabled = MutableStateFlow(true)
    val backgroundMonitorEnabled: StateFlow<Boolean> = _backgroundMonitorEnabled
    private val _marketMode = MutableStateFlow("NSE")
    val marketMode: StateFlow<String> = _marketMode
    // Parallel paper portfolios (NSE only): 'main' (data-harvesting) or 'eval' (Rs.10k evaluation).
    private val _selectedPortfolio = MutableStateFlow("main")
    val selectedPortfolio: StateFlow<String> = _selectedPortfolio

    fun init(context: Context) {
        this.context = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _connectivityAlerts.value = prefs.getBoolean(KEY_CONNECTIVITY_ALERTS, true)
        _appLockEnabled.value = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        _backgroundMonitorEnabled.value = prefs.getBoolean(KEY_BACKGROUND_MONITOR_ENABLED, true)
        _marketMode.value = prefs.getString(KEY_MARKET_MODE, "NSE") ?: "NSE"
        _selectedPortfolio.value = prefs.getString(KEY_SELECTED_PORTFOLIO, "main") ?: "main"
    }

    fun setSelectedPortfolio(name: String) {
        val normalized = if (name == "eval") "eval" else "main"
        _selectedPortfolio.value = normalized
        prefs.edit().putString(KEY_SELECTED_PORTFOLIO, normalized).apply()
    }

    fun getSelectedPortfolio(): String = _selectedPortfolio.value

    val baseUrl: String
        get() {
            if (!::prefs.isInitialized) return BuildConfig.DEFAULT_BASE_URL
            return if (_marketMode.value == "FOREX") {
                prefs.getString(KEY_FOREX_SERVER_URL, null)?.trim().takeUnless { it.isNullOrBlank() }
                    ?: BuildConfig.DEFAULT_FOREX_URL
            } else {
                prefs.getString(KEY_SERVER_URL, null)?.trim().takeUnless { it.isNullOrBlank() }
                    ?: BuildConfig.DEFAULT_BASE_URL
            }
        }

    val wsUrl: String
        get() {
            if (!::prefs.isInitialized) return BuildConfig.DEFAULT_WS_URL
            val base = baseUrl
            return base.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
        }

    val apiKey: String
        get() {
            if (!::prefs.isInitialized) return BuildConfig.DEFAULT_API_KEY
            return if (_marketMode.value == "FOREX") {
                prefs.getString(KEY_FOREX_API_KEY, null)?.trim().takeUnless { it.isNullOrBlank() } ?: ""
            } else {
                prefs.getString(KEY_API_KEY, null)?.trim().takeUnless { it.isNullOrBlank() }
                    ?: BuildConfig.DEFAULT_API_KEY
            }
        }

    fun saveServerUrl(httpUrl: String) {
        if (_marketMode.value == "FOREX") {
            prefs.edit().putString(KEY_FOREX_SERVER_URL, httpUrl).apply()
        } else {
            val wsVersion = httpUrl.replaceFirst("http://", "ws://").replaceFirst("https://", "wss://")
            prefs.edit()
                .putString(KEY_SERVER_URL, httpUrl)
                .putString(KEY_WS_URL, wsVersion)
                .apply()
        }
    }

    fun saveApiKey(value: String) {
        if (_marketMode.value == "FOREX") {
            prefs.edit().putString(KEY_FOREX_API_KEY, value.trim()).apply()
        } else {
            prefs.edit().putString(KEY_API_KEY, value.trim()).apply()
        }
    }

    fun setConnectivityAlerts(enabled: Boolean) {
        _connectivityAlerts.value = enabled
        prefs.edit().putBoolean(KEY_CONNECTIVITY_ALERTS, enabled).apply()
    }

    fun isConnectivityAlertsEnabled(): Boolean = _connectivityAlerts.value

    fun setAppLockEnabled(enabled: Boolean) {
        _appLockEnabled.value = enabled
        prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, enabled).apply()
    }

    fun isAppLockEnabled(): Boolean = _appLockEnabled.value

    fun setBackgroundMonitorEnabled(enabled: Boolean) {
        _backgroundMonitorEnabled.value = enabled
        prefs.edit().putBoolean(KEY_BACKGROUND_MONITOR_ENABLED, enabled).apply()
    }

    fun isBackgroundMonitorEnabled(): Boolean = _backgroundMonitorEnabled.value

    fun setMarketMode(mode: String) {
        _marketMode.value = mode
        prefs.edit().putString(KEY_MARKET_MODE, mode).apply()
    }

    fun getMarketMode(): String = _marketMode.value

    /** API path prefix based on market mode: "" for NSE (default), "forex/" for Forex */
    val apiPrefix: String
        get() = if (_marketMode.value == "FOREX") "forex/" else ""

    /** Currency symbol: Rs. for NSE, $ for Forex */
    val currencySymbol: String
        get() = if (_marketMode.value == "FOREX") "$" else "Rs."

    fun setActiveScanJobId(jobId: String?) {
        prefs.edit().putString(KEY_ACTIVE_SCAN_JOB_ID, jobId).apply()
    }

    fun getActiveScanJobId(): String? =
        prefs.getString(KEY_ACTIVE_SCAN_JOB_ID, null)?.trim().takeUnless { it.isNullOrBlank() }

    fun clearActiveScanJobId() {
        prefs.edit().remove(KEY_ACTIVE_SCAN_JOB_ID).apply()
    }

    fun getLastJournalActionId(): Int = prefs.getInt(KEY_LAST_JOURNAL_ACTION_ID, 0)

    fun setLastJournalActionId(value: Int) {
        prefs.edit().putInt(KEY_LAST_JOURNAL_ACTION_ID, value).apply()
    }

    fun isOutcomeNotified(entryId: Int): Boolean {
        val ids = prefs.getStringSet(KEY_NOTIFIED_OUTCOME_IDS, emptySet()) ?: emptySet()
        return ids.contains(entryId.toString())
    }

    fun markOutcomeNotified(entryId: Int) {
        val current = (prefs.getStringSet(KEY_NOTIFIED_OUTCOME_IDS, emptySet()) ?: emptySet()).toMutableSet()
        current.add(entryId.toString())
        prefs.edit().putStringSet(KEY_NOTIFIED_OUTCOME_IDS, current).apply()
    }
}

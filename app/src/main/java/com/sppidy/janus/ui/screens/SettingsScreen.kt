package com.sppidy.janus.ui.screens

import androidx.biometric.BiometricManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.sppidy.janus.AppPreferences
import com.sppidy.janus.BuildConfig
import com.sppidy.janus.api.ApiService
import com.sppidy.janus.security.BiometricAuth
import com.sppidy.janus.service.BackgroundMonitorService
import com.sppidy.janus.ui.neon.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

@Composable
fun SettingsScreen() {
    val marketMode by AppPreferences.marketMode.collectAsState()
    val selectedPortfolio by AppPreferences.selectedPortfolio.collectAsState()
    val connectivityAlertsEnabled by AppPreferences.connectivityAlerts.collectAsState()
    val appLockEnabled by AppPreferences.appLockEnabled.collectAsState()
    val backgroundMonitorEnabled by AppPreferences.backgroundMonitorEnabled.collectAsState()
    var appLockUpdating by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var showBackendDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? FragmentActivity
    val scope = rememberCoroutineScope()
    val agentName by AppPreferences.agentName.collectAsState()
    val aiModel by AppPreferences.aiModel.collectAsState()
    val baseUrl by remember(marketMode) { mutableStateOf(AppPreferences.baseUrl) }
    val apiKey by remember(marketMode) { mutableStateOf(AppPreferences.apiKey) }
    // Recompose-on-save trigger
    var editCounter by remember { mutableStateOf(0) }
    val currentBaseUrl = remember(editCounter, marketMode) { AppPreferences.baseUrl }
    val currentApiKey = remember(editCounter, marketMode) { AppPreferences.apiKey }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeonTokens.Bg)
            .verticalScroll(rememberScrollState())
            .padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // ── Header ─────────────────────────────────────────
        Column {
            Text(
                "// SYSTEM",
                color = NeonTokens.TextMute,
                fontSize = 10.sp,
                letterSpacing = 2.5.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
            Text(
                "Settings",
                color = NeonTokens.Text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = NeonTokens.DisplayFamily,
            )
        }

        // ── Profile ────────────────────────────────────────
        Panel(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .border(1.dp, NeonTokens.Neon),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        agentName.take(2).uppercase(),
                        color = NeonTokens.Neon,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.DisplayFamily,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        agentName.lowercase(),
                        color = NeonTokens.Text,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    Row {
                        Text(
                            "Agent ID: ",
                            color = NeonTokens.TextMute,
                            fontSize = 10.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                        Text(
                            "0x${BuildConfig.VERSION_CODE.toString(16).uppercase().padStart(4, '0')}",
                            color = NeonTokens.Neon,
                            fontSize = 10.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
                Tag(text = "VERIFIED", color = NeonTokens.Bull)
            }
        }

        // ── Market mode ───────────────────────────────────
        SectionLabel(text = "market")
        Panel(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "MARKET MODE",
                    color = NeonTokens.TextMute,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("NSE", "FOREX").forEach { m ->
                        ModeButton(
                            label = m,
                            selected = marketMode == m,
                            onClick = {
                                if (marketMode != m) {
                                    AppPreferences.setMarketMode(m)
                                    ApiService.invalidate()
                                    editCounter++
                                }
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }

        // ── Portfolio (NSE only) ──────────────────────────
        if (marketMode == "NSE") {
            SectionLabel(text = "portfolio")
            Panel(modifier = Modifier.fillMaxWidth()) {
                Column {
                    PortfolioRow(
                        value = "main",
                        label = "MAIN",
                        sub = "Rs. 1 Cr allocated",
                        amount = "Rs. 1Cr",
                        selected = selectedPortfolio == "main",
                        firstRow = true,
                    ) { AppPreferences.setSelectedPortfolio("main") }
                    PortfolioRow(
                        value = "eval",
                        label = "EVAL",
                        sub = "Rs. 1 Cr model-only",
                        amount = "Rs. 1Cr",
                        selected = selectedPortfolio == "eval",
                        firstRow = false,
                    ) { AppPreferences.setSelectedPortfolio("eval") }
                }
            }
        }

        // ── Agent config ──────────────────────────────────
        SectionLabel(text = "agent config")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SettingRow(
                label = "AI MODEL",
                value = aiModel,
            )
            SettingRow(
                label = "BACKEND",
                value = trimUrl(currentBaseUrl),
                mono = true,
                onClick = { showBackendDialog = true },
            )
            SettingRow(
                label = "API KEY",
                value = if (currentApiKey.isEmpty()) "— not set —" else "•".repeat(minOf(8, currentApiKey.length)),
                mono = true,
                onClick = { showApiKeyDialog = true },
            )
            SettingRow(
                label = "POLL INTERVAL",
                value = "15 MIN",
            )
        }

        // ── Security ──────────────────────────────────────
        SectionLabel(text = "security")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleRow(
                label = "BIOMETRIC AUTH",
                desc = "Fingerprint on app open",
                on = appLockEnabled,
                enabled = !appLockUpdating,
                onChange = { enable ->
                    statusMsg = null
                    if (!enable) {
                        AppPreferences.setAppLockEnabled(false)
                        statusMsg = "App lock disabled"
                    } else {
                        val a = activity
                        if (a == null) {
                            statusMsg = "Cannot start biometric authentication."
                            return@ToggleRow
                        }
                        val availability = BiometricAuth.canAuthenticate(a)
                        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                            statusMsg = BiometricAuth.availabilityMessage(availability)
                            return@ToggleRow
                        }
                        appLockUpdating = true
                        BiometricAuth.authenticate(
                            activity = a,
                            title = "Enable App Lock",
                            subtitle = "Confirm biometric authentication",
                            onSuccess = {
                                appLockUpdating = false
                                AppPreferences.setAppLockEnabled(true)
                                statusMsg = "App lock enabled"
                            },
                            onFailure = { m ->
                                appLockUpdating = false
                                AppPreferences.setAppLockEnabled(false)
                                statusMsg = m
                            },
                        )
                    }
                },
            )
            ToggleRow(
                label = "BACKGROUND MONITOR",
                desc = "Runs when app is closed",
                on = backgroundMonitorEnabled,
                onChange = { enabled ->
                    AppPreferences.setBackgroundMonitorEnabled(enabled)
                    if (enabled) BackgroundMonitorService.start(AppPreferences.context)
                    else BackgroundMonitorService.stop(AppPreferences.context)
                },
            )
            ToggleRow(
                label = "CONNECTIVITY ALERTS",
                desc = "Show server-unreachable banner",
                on = connectivityAlertsEnabled,
                onChange = { AppPreferences.setConnectivityAlerts(it) },
            )
        }

        statusMsg?.let { msg ->
            Panel(
                modifier = Modifier.fillMaxWidth(),
                accent = if (msg.contains("enabled", true) || msg.contains("disabled", true)) NeonTokens.Neon else NeonTokens.Bear,
            ) {
                Text(
                    msg,
                    modifier = Modifier.padding(12.dp),
                    color = NeonTokens.Text,
                    fontSize = 11.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }

        // ── About footer ──────────────────────────────────
        Spacer(Modifier.height(8.dp))
        Text(
            "JANUS v${BuildConfig.VERSION_NAME} · BUILD 0x${BuildConfig.VERSION_CODE.toString(16).uppercase().padStart(4, '0')}",
            color = NeonTokens.TextDim,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
            fontFamily = NeonTokens.MonoFamily,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }

    if (showBackendDialog) {
        EditStringDialog(
            title = "BACKEND URL",
            initialValue = currentBaseUrl,
            hint = "e.g. https://100.x.y.z:8443",
            allowTestConnection = true,
            currentApiKey = currentApiKey,
            onDismiss = { showBackendDialog = false },
            onSave = { newUrl ->
                AppPreferences.saveServerUrl(newUrl)
                ApiService.invalidate()
                editCounter++
                showBackendDialog = false
                statusMsg = "Backend URL saved"
            },
        )
    }

    if (showApiKeyDialog) {
        EditStringDialog(
            title = "API KEY",
            initialValue = currentApiKey,
            hint = "Bearer token from backend",
            password = true,
            onDismiss = { showApiKeyDialog = false },
            onSave = { newKey ->
                AppPreferences.saveApiKey(newKey)
                ApiService.invalidate()
                editCounter++
                showApiKeyDialog = false
                statusMsg = "API key saved"
            },
        )
    }
}

// ─── Components ───────────────────────────────────────────────

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(if (selected) NeonTokens.Neon else Color.Transparent)
            .border(1.dp, if (selected) NeonTokens.Neon else NeonTokens.Border)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) Color.Black else NeonTokens.TextMute,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun PortfolioRow(
    value: String,
    label: String,
    sub: String,
    amount: String,
    selected: Boolean,
    firstRow: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) NeonTokens.NeonSoft else Color.Transparent)
            .border(
                width = if (!firstRow) 1.dp else 0.dp,
                color = if (!firstRow) NeonTokens.Border else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .border(1.dp, NeonTokens.Neon),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(NeonTokens.Neon),
                    )
                }
            }
            Column {
                Text(
                    label,
                    color = NeonTokens.Text,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
                Text(
                    sub,
                    color = NeonTokens.TextMute,
                    fontSize = 9.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        }
        Text(
            amount,
            color = NeonTokens.Text,
            fontSize = 11.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
    }
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    mono: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTokens.BgElev1)
            .border(1.dp, NeonTokens.Border)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            color = NeonTokens.TextMute,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
            fontFamily = NeonTokens.MonoFamily,
        )
        Text(
            value,
            color = NeonTokens.Text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = if (mono) NeonTokens.MonoFamily else NeonTokens.DisplayFamily,
            maxLines = 1,
        )
    }
}

@Composable
private fun ToggleRow(
    label: String,
    desc: String,
    on: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NeonTokens.BgElev1)
            .border(1.dp, NeonTokens.Border)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                label,
                color = NeonTokens.Text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
            Text(
                desc,
                color = NeonTokens.TextMute,
                fontSize = 9.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        }
        NeonToggle(on = on, enabled = enabled, onChange = onChange)
    }
}

@Composable
private fun NeonToggle(
    on: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    // Track 44x22, knob 14x14 with 3dp inset → knob slides 3dp → 27dp
    val knobOffset by animateDpAsState(
        targetValue = if (on) 27.dp else 3.dp,
        animationSpec = tween(durationMillis = 180),
        label = "toggleKnob",
    )
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 22.dp)
            .background(if (on) NeonTokens.NeonSoft else NeonTokens.BgElev2)
            .border(1.dp, if (on) NeonTokens.Neon else NeonTokens.Border)
            .clickable(enabled = enabled) { onChange(!on) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = knobOffset)
                .size(14.dp)
                .background(if (on) NeonTokens.Neon else NeonTokens.TextMute),
        )
    }
}

@Composable
private fun EditStringDialog(
    title: String,
    initialValue: String,
    hint: String = "",
    password: Boolean = false,
    allowTestConnection: Boolean = false,
    currentApiKey: String = "",
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initialValue) }
    var visible by remember { mutableStateOf(!password) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonTokens.BgElev1,
        tonalElevation = 0.dp,
        title = {
            Text(
                title,
                color = NeonTokens.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                fontFamily = NeonTokens.MonoFamily,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .background(NeonTokens.Bg)
                        .border(1.dp, NeonTokens.Border)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        ">",
                        color = NeonTokens.Neon,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = NeonTokens.MonoFamily,
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty()) {
                            Text(
                                hint,
                                color = NeonTokens.TextDim,
                                fontSize = 12.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = { value = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = NeonTokens.Text,
                                fontSize = 12.sp,
                                fontFamily = NeonTokens.MonoFamily,
                            ),
                            cursorBrush = SolidColor(NeonTokens.Neon),
                            visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password) else KeyboardOptions.Default,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (password) {
                        Text(
                            if (visible) "HIDE" else "SHOW",
                            color = NeonTokens.TextMute,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = NeonTokens.MonoFamily,
                            modifier = Modifier.clickable { visible = !visible },
                        )
                    }
                }
                if (allowTestConnection) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeonButton(
                            text = if (testing) "TESTING..." else "TEST",
                            onClick = {
                                testing = true
                                testResult = null
                                scope.launch {
                                    testResult = testConnection(value, currentApiKey)
                                    testing = false
                                }
                            },
                            variant = NeonButtonVariant.Ghost,
                            small = true,
                            enabled = !testing,
                        )
                        NeonButton(
                            text = "DEFAULT",
                            onClick = {
                                value = BuildConfig.DEFAULT_BASE_URL
                                testResult = null
                            },
                            variant = NeonButtonVariant.Muted,
                            small = true,
                        )
                    }
                    testResult?.let { r ->
                        Text(
                            r,
                            color = if (r.startsWith("Connected")) NeonTokens.Bull else NeonTokens.Bear,
                            fontSize = 10.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
            }
        },
        confirmButton = {
            NeonButton(
                text = "SAVE",
                onClick = { onSave(value) },
                variant = NeonButtonVariant.Primary,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "CANCEL",
                    color = NeonTokens.TextMute,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = NeonTokens.MonoFamily,
                )
            }
        },
    )
}

private fun trimUrl(url: String): String {
    val clean = url.removePrefix("https://").removePrefix("http://").trimEnd('/')
    return if (clean.length > 24) clean.take(22) + ".." else clean
}

private suspend fun testConnection(url: String, apiKey: String): String = withContext(Dispatchers.IO) {
    try {
        val trimmed = url.trim()
        val normalizedBaseUrl = if (trimmed.isBlank()) BuildConfig.DEFAULT_BASE_URL
        else if (trimmed.startsWith("http", ignoreCase = true)) trimmed
        else "https://$trimmed"
        val target = if (normalizedBaseUrl.endsWith("/")) "${normalizedBaseUrl}api/status" else "$normalizedBaseUrl/api/status"
        val conn = URL(target).openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        val key = apiKey.trim()
        if (key.isNotEmpty()) conn.setRequestProperty("X-API-Key", key)
        val code = conn.responseCode
        conn.disconnect()
        when {
            code in 200..299 -> "Connected — server running"
            code == 401 -> "Unauthorized — check API key"
            else -> "HTTP $code"
        }
    } catch (e: java.net.ConnectException) { "Connection refused" }
    catch (e: java.net.SocketTimeoutException) { "Timed out — VPN?" }
    catch (e: java.net.UnknownHostException) { "Host not found — VPN?" }
    catch (e: SSLPeerUnverifiedException) { "TLS hostname mismatch" }
    catch (e: SSLHandshakeException) { "TLS handshake failed" }
    catch (e: MalformedURLException) { "Invalid URL" }
    catch (e: Exception) { "Error: ${e.message}" }
}

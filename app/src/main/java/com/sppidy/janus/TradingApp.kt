package com.sppidy.janus

import androidx.biometric.BiometricManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sppidy.janus.ui.neon.NeonTokens
import com.sppidy.janus.ui.neon.ScanlineOverlay
import com.sppidy.janus.ui.neon.TickerTape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sppidy.janus.navigation.ChartDetailRoute
import com.sppidy.janus.navigation.ChartsRoute
import com.sppidy.janus.navigation.ChatRoute
import com.sppidy.janus.navigation.DashboardRoute
import com.sppidy.janus.navigation.LogsRoute
import com.sppidy.janus.navigation.PortfolioRoute
import com.sppidy.janus.navigation.ScannerRoute
import com.sppidy.janus.navigation.SettingsRoute
import com.sppidy.janus.navigation.TopLevelBackStack
import com.sppidy.janus.navigation.normalizeSymbolInput
import com.sppidy.janus.security.BiometricAuth
import com.sppidy.janus.ui.screens.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.min

sealed class Screen(
    val route: NavKey,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    data object Dashboard : Screen(DashboardRoute, "Dash", Icons.Filled.Dashboard, Icons.Outlined.Dashboard)
    data object Portfolio : Screen(PortfolioRoute, "Book", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
    data object Scanner : Screen(ScannerRoute, "Scan", Icons.Filled.Radar, Icons.Outlined.Radar)
    data object Charts : Screen(ChartsRoute, "Chart", Icons.AutoMirrored.Filled.ShowChart, Icons.AutoMirrored.Outlined.ShowChart)
    data object Chat : Screen(ChatRoute, "Agent", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome)
    data object Logs : Screen(LogsRoute, "Log", Icons.Filled.Terminal, Icons.Outlined.Terminal)
    data object Settings : Screen(SettingsRoute, "Cfg", Icons.Filled.Settings, Icons.Outlined.Settings)
}

val screens = listOf(
    Screen.Dashboard,
    Screen.Portfolio,
    Screen.Scanner,
    Screen.Charts,
    Screen.Chat,
    Screen.Logs,
    Screen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingApp() {
    val topLevelBackStack = remember { TopLevelBackStack<NavKey>(DashboardRoute) }
    val connectivityAlertsEnabled by AppPreferences.connectivityAlerts.collectAsState()

    // Connection check with exponential backoff
    var serverReachable by remember { mutableStateOf<Boolean?>(null) }
    var consecutiveFailures by remember { mutableIntStateOf(0) }
    var isChecking by remember { mutableStateOf(false) }
    // Increment to force an immediate re-check
    var refreshTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshTrigger, connectivityAlertsEnabled) {
        if (!connectivityAlertsEnabled) {
            serverReachable = null
            isChecking = false
            return@LaunchedEffect
        }
        while (true) {
            isChecking = true
            val reachable = checkServerReachable()
            serverReachable = reachable
            isChecking = false

            if (reachable) {
                consecutiveFailures = 0
                delay(10_000) // 10s when healthy
            } else {
                consecutiveFailures++
                // Exponential backoff: 10s, 20s, 40s, 60s (capped)
                val backoffMs = min(10_000L * (1L shl min(consecutiveFailures - 1, 3)), 60_000L)
                delay(backoffMs)
            }
        }
    }

    Scaffold(
        bottomBar = { NeonBottomBar(topLevelBackStack) },
        containerColor = NeonTokens.Bg,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            // NEON ticker tape
            TickerTape()

            // VPN / connection status banner
            AnimatedVisibility(
                visible = connectivityAlertsEnabled && serverReachable == false,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFB71C1C))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.VpnLock,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Server unreachable — is your VPN on?",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Retry now",
                            tint = Color.White,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    consecutiveFailures = 0
                                    refreshTrigger++
                                }
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                NavDisplay(
                    backStack = topLevelBackStack.backStack,
                    onBack = { topLevelBackStack.removeLast() },
                    entryProvider = entryProvider {
                        entry<DashboardRoute> {
                            DashboardScreen(
                                onOpenChart = { symbol ->
                                    val normalized = normalizeSymbolInput(symbol)
                                    if (normalized.isNotBlank()) {
                                        topLevelBackStack.add(ChartDetailRoute(normalized))
                                    }
                                }
                            )
                        }
                        entry<PortfolioRoute> { PortfolioScreen() }
                        entry<ChatRoute> { ChatScreen() }
                        entry<ChartsRoute> {
                            // Top-level Chart tab: no back, has search bar
                            ChartScreen(showBack = false, showSearch = true)
                        }
                        entry<ChartDetailRoute> { key ->
                            // Pushed from Dashboard position/trade: show back, no search
                            ChartScreen(
                                initialSymbol = key.symbol.ifBlank { null },
                                showBack = true,
                                showSearch = false,
                                onBack = { topLevelBackStack.removeLast() },
                            )
                        }
                        entry<LogsRoute> { LogScreen() }
                        entry<ScannerRoute> { ScanScreen() }
                        entry<SettingsRoute> {
                            SettingsAccessGate { SettingsScreen() }
                        }
                    }
                )
                // NEON scanline overlay
                ScanlineOverlay(modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun SettingsAccessGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var unlocked by rememberSaveable { mutableStateOf(false) }
    var unlockInProgress by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    // Track whether we've already fired the auto-prompt so recompositions
    // (e.g., rotation) don't re-trigger the biometric dialog.
    var autoPromptFired by rememberSaveable { mutableStateOf(false) }

    fun requestUnlock() {
        if (unlockInProgress) return
        if (activity == null) {
            error = "Unable to start biometric authentication."
            return
        }
        val availability = BiometricAuth.canAuthenticate(activity)
        if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
            error = BiometricAuth.availabilityMessage(availability)
            return
        }
        unlockInProgress = true
        BiometricAuth.authenticate(
            activity = activity,
            title = "Unlock Settings",
            subtitle = "Authenticate to open settings",
            onSuccess = {
                unlockInProgress = false
                unlocked = true
                error = null
            },
            onFailure = { message ->
                unlockInProgress = false
                unlocked = false
                error = message
            },
        )
    }

    LaunchedEffect(Unit) {
        if (!autoPromptFired && !unlocked) {
            autoPromptFired = true
            requestUnlock()
        }
    }

    if (unlocked) {
        content()
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Text("Settings Locked", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Biometric authentication is required.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { requestUnlock() }, enabled = !unlockInProgress) {
                    if (unlockInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Unlock Settings")
                    }
                }
            }
        }
    }
}

@Composable
private fun NeonBottomBar(backStack: com.sppidy.janus.navigation.TopLevelBackStack<NavKey>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(NeonTokens.BgElev1)
                .border(1.dp, NeonTokens.Border, RoundedCornerShape(18.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            screens.forEach { screen ->
                val selected = screen.route == backStack.topLevelKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { backStack.addTopLevel(screen.route) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    // Top pill indicator when selected
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-4).dp)
                                .size(width = 18.dp, height = 2.dp)
                                .background(NeonTokens.Neon)
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(
                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                            contentDescription = screen.label,
                            tint = if (selected) NeonTokens.Neon else NeonTokens.TextMute,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = screen.label.uppercase(),
                            color = if (selected) NeonTokens.Neon else NeonTokens.TextMute,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            fontFamily = NeonTokens.MonoFamily,
                        )
                    }
                }
            }
        }
    }
}

private suspend fun checkServerReachable(): Boolean = withContext(Dispatchers.IO) {
    try {
        val baseUrl = AppPreferences.baseUrl.trim().trimEnd('/')
        val url = URL("$baseUrl/api/status")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10000
        conn.readTimeout = 10000
        conn.requestMethod = "GET"
        val key = AppPreferences.apiKey.trim()
        if (key.isNotEmpty()) {
            conn.setRequestProperty("X-API-Key", key)
        }
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299 || code == 401 || code == 403
    } catch (_: Exception) {
        false
    }
}

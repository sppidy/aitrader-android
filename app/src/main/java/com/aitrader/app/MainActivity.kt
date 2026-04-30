package com.aitrader.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aitrader.app.security.BiometricAuth
import com.aitrader.app.service.BackgroundMonitorService
import com.aitrader.app.ui.theme.AITraderTheme

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppPreferences.init(this)
        requestNotificationPermissionIfNeeded()
        maybeStartBackgroundMonitor()
        // App is always dark — force light system-bar icons regardless of device theme
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            val appLockEnabled by AppPreferences.appLockEnabled.collectAsState()
            var appUnlocked by rememberSaveable { mutableStateOf(!appLockEnabled) }
            var lockError by rememberSaveable { mutableStateOf<String?>(null) }
            var unlockInProgress by remember { mutableStateOf(false) }
            val lifecycleOwner = LocalLifecycleOwner.current

            fun requestUnlock() {
                if (!appLockEnabled || unlockInProgress) return
                val availability = BiometricAuth.canAuthenticate(this@MainActivity)
                if (availability != BiometricManager.BIOMETRIC_SUCCESS) {
                    // Avoid locking users out if biometrics become unavailable after enabling.
                    AppPreferences.setAppLockEnabled(false)
                    appUnlocked = true
                    lockError = BiometricAuth.availabilityMessage(availability)
                    return
                }
                unlockInProgress = true
                BiometricAuth.authenticate(
                    activity = this@MainActivity,
                    title = "Unlock AI Trader",
                    subtitle = "Authenticate to continue",
                    onSuccess = {
                        unlockInProgress = false
                        appUnlocked = true
                        lockError = null
                    },
                    onFailure = { message ->
                        unlockInProgress = false
                        appUnlocked = false
                        lockError = message
                    },
                )
            }

            LaunchedEffect(appLockEnabled) {
                if (!appLockEnabled) {
                    appUnlocked = true
                    lockError = null
                } else if (appUnlocked) {
                    appUnlocked = false
                    requestUnlock()
                }
            }

            DisposableEffect(lifecycleOwner, appLockEnabled) {
                val observer = LifecycleEventObserver { _, event ->
                    if (!appLockEnabled) return@LifecycleEventObserver
                    when (event) {
                        Lifecycle.Event.ON_STOP -> appUnlocked = false
                        Lifecycle.Event.ON_START -> if (!appUnlocked) requestUnlock()
                        else -> Unit
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            AITraderTheme {
                // Always keep TradingApp alive to preserve ViewModel state (chat, scan).
                // Overlay the lock screen on top when locked instead of replacing TradingApp.
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                    TradingApp()
                    if (appLockEnabled && !appUnlocked) {
                        AppLockScreen(
                            error = lockError,
                            unlockInProgress = unlockInProgress,
                            onUnlock = { requestUnlock() },
                        )
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            maybeStartBackgroundMonitor()
        }
    }

    private fun maybeStartBackgroundMonitor() {
        if (!AppPreferences.isBackgroundMonitorEnabled()) return
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        if (!notificationsGranted) return
        BackgroundMonitorService.start(this)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQ_POST_NOTIFICATIONS,
            )
        }
    }

    companion object {
        private const val REQ_POST_NOTIFICATIONS = 101
    }
}

@Composable
private fun AppLockScreen(
    error: String?,
    unlockInProgress: Boolean,
    onUnlock: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Lock, contentDescription = null)
        Spacer(Modifier.height(12.dp))
        Text("App Locked", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Unlock with biometrics to access AI Trader.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onUnlock, enabled = !unlockInProgress) {
            if (unlockInProgress) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(18.dp))
            } else {
                Text("Unlock")
            }
        }
    }
}

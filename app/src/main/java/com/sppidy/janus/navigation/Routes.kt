package com.sppidy.janus.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object DashboardRoute : NavKey

@Serializable
data object PortfolioRoute : NavKey

@Serializable
data object ChatRoute : NavKey

@Serializable
data object ChartsRoute : NavKey

@Serializable
data class ChartDetailRoute(val symbol: String) : NavKey

@Serializable
data object LogsRoute : NavKey

@Serializable
data object ScannerRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

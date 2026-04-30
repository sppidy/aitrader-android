package com.aitrader.app.navigation

import java.util.Locale

fun normalizeSymbolInput(raw: String): String =
    raw.trim()
        .uppercase(Locale.US)
        .removeSuffix(".NS")
        .removeSuffix("=X")

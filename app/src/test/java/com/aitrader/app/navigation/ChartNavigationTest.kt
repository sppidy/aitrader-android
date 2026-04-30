package com.aitrader.app.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class ChartNavigationTest {
    @Test
    fun `normalizeSymbolInput trims uppercase and strips ns suffix`() {
        assertEquals("SBIN", normalizeSymbolInput("  sbin.ns "))
    }

    @Test
    fun `normalizeSymbolInput returns empty for blank input`() {
        assertEquals("", normalizeSymbolInput("   "))
    }
}

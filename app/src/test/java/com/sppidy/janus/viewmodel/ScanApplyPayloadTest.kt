package com.sppidy.janus.viewmodel

import com.sppidy.janus.model.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanApplyPayloadTest {
    @Test
    fun `isActionableForApply is true for buy and sell`() {
        assertTrue(Signal(symbol = "SBIN.NS", signal = "BUY", price = 100.0, confidence = null, reason = null, stopLoss = null, target = null).isActionableForApply())
        assertTrue(Signal(symbol = "INFY.NS", signal = "SELL", price = 1500.0, confidence = null, reason = null, stopLoss = null, target = null).isActionableForApply())
    }

    @Test
    fun `isActionableForApply is false for hold`() {
        assertFalse(Signal(symbol = "TCS.NS", signal = "HOLD", price = 3500.0, confidence = null, reason = null, stopLoss = null, target = null).isActionableForApply())
    }

    @Test
    fun `toApplySignalPayload maps signal fields`() {
        val signal = Signal(
            symbol = "RELIANCE.NS",
            signal = "BUY",
            price = 2500.0,
            confidence = 0.78,
            reason = "Breakout",
            stopLoss = 2420.0,
            target = 2700.0
        )

        val payload = signal.toApplySignalPayload()

        assertEquals("RELIANCE.NS", payload.symbol)
        assertEquals("BUY", payload.signal)
        assertEquals(2500.0, payload.price)
        assertEquals(0.78, payload.confidence)
        assertEquals(2420.0, payload.stopLoss)
        assertEquals(2700.0, payload.target)
        assertEquals("Breakout", payload.reason)
    }
}

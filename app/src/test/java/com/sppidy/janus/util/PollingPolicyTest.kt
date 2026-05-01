package com.sppidy.janus.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PollingPolicyTest {
    @Test
    fun nextDelay_capsAtMax() {
        val policy = PollingPolicy(maxAttempts = 10, initialDelayMs = 1000, maxDelayMs = 2000, stepMs = 700)
        assertEquals(1700, policy.nextDelay(1000))
        assertEquals(2000, policy.nextDelay(1700))
        assertEquals(2000, policy.nextDelay(2000))
    }
}

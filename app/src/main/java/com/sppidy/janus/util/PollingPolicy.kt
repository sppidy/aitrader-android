package com.sppidy.janus.util

data class PollingPolicy(
    val maxAttempts: Int,
    val initialDelayMs: Long = 1500L,
    val maxDelayMs: Long = 5000L,
    val stepMs: Long = 300L,
) {
    fun nextDelay(currentDelayMs: Long): Long = minOf(currentDelayMs + stepMs, maxDelayMs)
}

package com.example.engine

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure JVM-testable utilities for CarDeck driver assistance features.
 */
object DriverAssistUtils {

    private val CARDINALS = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")

    fun formatCardinalHeading(bearingDegrees: Float): String {
        val normalized = ((bearingDegrees % 360f) + 360f) % 360f
        val index = ((normalized + 22.5f) / 45f).toInt() % CARDINALS.size
        return "${CARDINALS[index]} ${normalized.roundToInt()}°"
    }

    fun cardinalShort(bearingDegrees: Float): String {
        val normalized = ((bearingDegrees % 360f) + 360f) % 360f
        val index = ((normalized + 22.5f) / 45f).toInt() % CARDINALS.size
        return CARDINALS[index]
    }

    fun shouldTriggerBreakReminder(
        tripStartTimeMs: Long,
        nowMs: Long,
        thresholdMinutes: Int,
        dismissedUntilMs: Long
    ): Boolean {
        if (thresholdMinutes <= 0) return false
        if (nowMs < dismissedUntilMs) return false
        val elapsedMs = (nowMs - tripStartTimeMs).coerceAtLeast(0L)
        return elapsedMs >= thresholdMinutes * 60_000L
    }

    fun shouldHighwayFocusBeActive(
        manualOverride: Boolean?,
        autoEnabled: Boolean,
        speedKmh: Float,
        currentlyActive: Boolean
    ): Boolean {
        if (manualOverride != null) return manualOverride
        if (!autoEnabled) return false
        // Hysteresis: engage above 72 km/h, disengage below 55 km/h
        return if (currentlyActive) speedKmh >= 55f else speedKmh >= 72f
    }

    fun distanceMetersBetween(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadius * c
    }

    fun formatParkedAgeMinutes(parkedTimestampMs: Long, nowMs: Long): Int {
        return ((nowMs - parkedTimestampMs).coerceAtLeast(0L) / 60_000L).toInt()
    }
}

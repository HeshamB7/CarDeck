package com.example

import com.example.engine.DriverAssistUtils
import com.example.model.CarWidgetType
import com.example.model.defaultDashboardPages
import com.example.ui.components.computeSuperellipsePoint
import com.example.ui.theme.buildCarPlayTypography
import com.example.viewmodel.DashboardPageMutator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class ExampleUnitTest {
    @Test
    fun superellipsePoints_stayWithinBoundsAndMatchCardinalEdges() {
        val steps = 120
        for (i in 0..steps) {
            val theta = (2.0 * PI * i) / steps
            val (x, y) = computeSuperellipsePoint(theta, 100f, 100f)
            assertTrue(x in -0.01f..100.01f)
            assertTrue(y in -0.01f..100.01f)
        }
        val (rightX, rightY) = computeSuperellipsePoint(0.0, 100f, 100f)
        assertEquals(100f, rightX, 0.01f)
        assertEquals(50f, rightY, 0.01f)
    }

    @Test
    fun carPlayTypography_scalesWithFontScaleAndScreenScale() {
        val base = buildCarPlayTypography(fontScale = 1f, screenScale = 1f)
        val large = buildCarPlayTypography(fontScale = 1.2f, screenScale = 1.15f)
        assertTrue(large.displaySpeed.fontSize > base.displaySpeed.fontSize)
        assertTrue(large.appLabel.fontSize > base.appLabel.fontSize)
        assertTrue(large.dockClock.fontSize > base.dockClock.fontSize)
    }

    @Test
    fun dashboardPageMutator_supportsAddDeleteAndWidgetReorder() {
        val initial = defaultDashboardPages()
        assertEquals(2, initial.size)

        // Add new page
        val withExtraPage = DashboardPageMutator.addPage(initial, newId = "p3")
        assertEquals(3, withExtraPage.size)
        assertEquals("p3", withExtraPage[2].id)

        // Add widgets to page 2
        val addedW1 = DashboardPageMutator.addWidget(withExtraPage, 2, CarWidgetType.WEATHER, widgetId = "w1")
        val addedW2 = DashboardPageMutator.addWidget(addedW1, 2, CarWidgetType.ANALOG_CLOCK, widgetId = "w2")
        assertEquals(listOf("w1", "w2"), addedW2[2].widgets.map { it.id })

        // Swap widgets
        val swapped = DashboardPageMutator.swapWidgets(addedW2, 2, "w1", "w2")
        assertEquals(listOf("w2", "w1"), swapped[2].widgets.map { it.id })

        // Move widget left
        val moved = DashboardPageMutator.moveWidget(swapped, 2, "w1", -1)
        assertEquals(listOf("w1", "w2"), moved[2].widgets.map { it.id })

        // Remove widget
        val removed = DashboardPageMutator.removeWidget(moved, 2, "w1")
        assertEquals(listOf("w2"), removed[2].widgets.map { it.id })

        // Delete down to 1 page and ensure last page cannot be deleted
        val twoPages = DashboardPageMutator.deletePage(removed, 2)
        val onePage = DashboardPageMutator.deletePage(twoPages, 1)
        val stillOne = DashboardPageMutator.deletePage(onePage, 0)
        assertEquals(1, stillOne.size)
    }

    @Test
    fun driverAssistUtils_formatsHeadingsCorrectly() {
        assertEquals("N", DriverAssistUtils.cardinalShort(0f))
        assertEquals("N", DriverAssistUtils.cardinalShort(359f))
        assertEquals("NE", DriverAssistUtils.cardinalShort(45f))
        assertEquals("E", DriverAssistUtils.cardinalShort(90f))
        assertEquals("SW", DriverAssistUtils.cardinalShort(225f))
        assertEquals("NW 315°", DriverAssistUtils.formatCardinalHeading(-45f))
    }

    @Test
    fun driverAssistUtils_highwayFocusHysteresisPreventsFlicker() {
        assertFalse(DriverAssistUtils.shouldHighwayFocusBeActive(null, autoEnabled = true, speedKmh = 68f, currentlyActive = false))
        assertTrue(DriverAssistUtils.shouldHighwayFocusBeActive(null, autoEnabled = true, speedKmh = 75f, currentlyActive = false))
        assertTrue(DriverAssistUtils.shouldHighwayFocusBeActive(null, autoEnabled = true, speedKmh = 60f, currentlyActive = true))
        assertFalse(DriverAssistUtils.shouldHighwayFocusBeActive(null, autoEnabled = true, speedKmh = 50f, currentlyActive = true))
        assertTrue(DriverAssistUtils.shouldHighwayFocusBeActive(manualOverride = true, autoEnabled = false, speedKmh = 0f, currentlyActive = false))
        assertFalse(DriverAssistUtils.shouldHighwayFocusBeActive(manualOverride = false, autoEnabled = true, speedKmh = 120f, currentlyActive = true))
    }

    @Test
    fun driverAssistUtils_breakReminderAndHaversineDistance() {
        val start = 1_000_000L
        assertFalse(DriverAssistUtils.shouldTriggerBreakReminder(start, start + 60 * 60_000L, thresholdMinutes = 90, dismissedUntilMs = 0L))
        assertTrue(DriverAssistUtils.shouldTriggerBreakReminder(start, start + 95 * 60_000L, thresholdMinutes = 90, dismissedUntilMs = 0L))
        assertFalse(DriverAssistUtils.shouldTriggerBreakReminder(start, start + 95 * 60_000L, thresholdMinutes = 90, dismissedUntilMs = start + 110 * 60_000L))

        val zeroDist = DriverAssistUtils.distanceMetersBetween(25.2048, 55.2708, 25.2048, 55.2708)
        assertEquals(0.0, zeroDist, 0.01)

        val shortDist = DriverAssistUtils.distanceMetersBetween(25.2048, 55.2708, 25.2058, 55.2708)
        assertTrue(shortDist in 105.0..118.0)

        assertEquals(12, DriverAssistUtils.formatParkedAgeMinutes(100_000L, 100_000L + 12 * 60_000L))
    }
}

package com.nickallport.findmevinyl.shops

import org.junit.Assert.assertEquals
import org.junit.Test

class DistanceCalculatorTest {

    @Test
    fun `distance between a point and itself is zero`() {
        val distance = DistanceCalculator.distanceKm(51.5074, -0.1278, 51.5074, -0.1278)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `distance between London and Paris is approximately 344 km`() {
        // London: 51.5074, -0.1278    Paris: 48.8566, 2.3522
        val distance = DistanceCalculator.distanceKm(51.5074, -0.1278, 48.8566, 2.3522)
        assertEquals(344.0, distance, 5.0)
    }
}

package com.tinyhis.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * GeoUtils Unit Tests
 */
class GeoUtilsTest {

    @Test
    void testCalculateDistance_SamePoint() {
        double distance = GeoUtils.calculateDistance(39.9042, 116.4074, 39.9042, 116.4074);
        assertEquals(0, distance, 0.01);
    }

    @Test
    void testCalculateDistance_NearbyPoints() {
        // Approximately 100 meters apart
        double distance = GeoUtils.calculateDistance(39.9042, 116.4074, 39.9051, 116.4074);
        assertTrue(distance > 0 && distance < 200);
    }

    @Test
    void testIsWithinRadius_Inside() {
        boolean result = GeoUtils.isWithinRadius(39.9042, 116.4074, 39.9042, 116.4074, 500);
        assertTrue(result);
    }

    @Test
    void testIsWithinRadius_Outside() {
        // Points that are farther than 500 meters
        boolean result = GeoUtils.isWithinRadius(39.9, 116.4, 40.0, 116.5, 500);
        assertFalse(result);
    }
}

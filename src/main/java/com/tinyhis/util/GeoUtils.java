package com.tinyhis.util;

/**
 * Geographic Utility for GPS-based check-in
 * Uses Haversine formula to calculate distance between two points
 */
public class GeoUtils {

    private static final double EARTH_RADIUS = 6371000; // meters

    /**
     * Calculate distance between two GPS coordinates using Haversine formula
     * @param lat1 Latitude of point 1
     * @param lon1 Longitude of point 1
     * @param lat2 Latitude of point 2
     * @param lon2 Longitude of point 2
     * @return Distance in meters
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS * c;
    }

    /**
     * Check if a location is within the specified radius
     * @param userLat User's latitude
     * @param userLon User's longitude
     * @param targetLat Target latitude (e.g., hospital)
     * @param targetLon Target longitude
     * @param radiusMeters Allowed radius in meters
     * @return true if within radius
     */
    public static boolean isWithinRadius(double userLat, double userLon, 
                                          double targetLat, double targetLon, 
                                          double radiusMeters) {
        return calculateDistance(userLat, userLon, targetLat, targetLon) <= radiusMeters;
    }
}

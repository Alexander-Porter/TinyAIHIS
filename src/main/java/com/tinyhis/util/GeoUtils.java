package com.tinyhis.util;

/**
 * 基于GPS签到的地理位置工具类
 * 使用Haversine公式计算两点之间的距离
 */
public class GeoUtils {

    private static final double EARTH_RADIUS = 6371000; // 地球半径（米）

    /**
     * 使用Haversine公式计算两个GPS坐标点之间的距离
     * @param lat1 第一个点的纬度
     * @param lon1 第一个点的经度
     * @param lat2 第二个点的纬度
     * @param lon2 第二个点的经度
     * @return 两点之间的距离（米）
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
     * 检查位置是否在指定半径范围内
     * @param userLat 用户纬度
     * @param userLon 用户经度
     * @param targetLat 目标纬度（例如：医院）
     * @param targetLon 目标经度
     * @param radiusMeters 允许的半径（米）
     * @return 如果在半径范围内返回true
     */
    public static boolean isWithinRadius(double userLat, double userLon, 
                                          double targetLat, double targetLon, 
                                          double radiusMeters) {
        return calculateDistance(userLat, userLon, targetLat, targetLon) <= radiusMeters;
    }
}

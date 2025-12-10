package com.tinyhis.service;

import java.util.Map;

public interface DashboardService {
    /**
     * Get dashboard statistics including 7-day flow and top doctors
     * @return Map containing statistics data
     */
    Map<String, Object> getDashboardStats();
    
    /**
     * Refresh dashboard cache manually
     */
    void refreshDashboardStats();
}

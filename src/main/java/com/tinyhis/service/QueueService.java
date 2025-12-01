package com.tinyhis.service;

import com.tinyhis.dto.QueueInfo;

/**
 * Queue Service Interface for WebSocket-based queue display
 */
public interface QueueService {

    /**
     * Add patient to queue
     */
    void addToQueue(Long doctorId, Long regId);

    /**
     * Remove patient from queue
     */
    void removeFromQueue(Long doctorId, Long regId);

    /**
     * Get next patient from queue
     */
    Long getNextFromQueue(Long doctorId);

    /**
     * Get current queue info for department display
     */
    QueueInfo getQueueInfo(Long deptId);

    /**
     * Broadcast queue update to WebSocket clients
     */
    void broadcastQueueUpdate(Long deptId);
}

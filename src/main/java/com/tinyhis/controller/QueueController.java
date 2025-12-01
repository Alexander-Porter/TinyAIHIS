package com.tinyhis.controller;

import com.tinyhis.dto.QueueInfo;
import com.tinyhis.dto.Result;
import com.tinyhis.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Queue Controller for display screen
 */
@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    /**
     * Get current queue info for department
     */
    @GetMapping("/{deptId}")
    public Result<QueueInfo> getQueueInfo(@PathVariable Long deptId) {
        QueueInfo info = queueService.getQueueInfo(deptId);
        return Result.success(info);
    }

    /**
     * Manually trigger queue broadcast (for testing)
     */
    @PostMapping("/broadcast/{deptId}")
    public Result<Void> broadcast(@PathVariable Long deptId) {
        queueService.broadcastQueueUpdate(deptId);
        return Result.success();
    }
}

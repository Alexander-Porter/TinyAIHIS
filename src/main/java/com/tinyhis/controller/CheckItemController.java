package com.tinyhis.controller;

import com.tinyhis.dto.Result;
import com.tinyhis.entity.CheckItem;
import com.tinyhis.service.CheckItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Check Item Controller
 */
@RestController
@RequestMapping("/api/check-item")
@RequiredArgsConstructor
public class CheckItemController {

    private final CheckItemService checkItemService;

    /**
     * Get all active check items
     */
    @GetMapping("/list")
    public Result<List<CheckItem>> getAllCheckItems() {
        return Result.success(checkItemService.getAllCheckItems());
    }

    /**
     * Search check items
     */
    @GetMapping("/search")
    public Result<List<CheckItem>> searchCheckItems(@RequestParam String keyword) {
        return Result.success(checkItemService.searchCheckItems(keyword));
    }
}

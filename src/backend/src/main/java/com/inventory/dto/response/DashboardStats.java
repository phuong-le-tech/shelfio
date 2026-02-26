package com.inventory.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardStats(
        long totalItems,
        long totalQuantity,
        long lowStockCount,
        long outOfStockCount,
        Map<String, Long> countByStatus,
        Map<String, Long> countByCategory,
        List<ListOverviewDto> listsOverview,
        List<RecentItemDto> recentlyUpdated) {

    public record ListOverviewDto(
            String listName,
            long itemsCount,
            long totalQuantity) {
    }

    public record RecentItemDto(
            String id,
            String name,
            String listName,
            String sku,
            long quantity,
            String status,
            java.time.LocalDateTime lastUpdated) {
    }
}

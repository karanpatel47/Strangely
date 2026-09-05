package com.strangerchat.controller;

import com.strangerchat.dto.ActiveUserStatsResponse;
import com.strangerchat.service.ActiveUserTracker;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class StatsController {

    private final ActiveUserTracker activeUserTracker;

    public StatsController(ActiveUserTracker activeUserTracker) {
        this.activeUserTracker = activeUserTracker;
    }

    @GetMapping("/api/stats/active-users")
    public Map<String, Integer> activeUsers() {
        return Map.of("activeUsers", activeUserTracker.getActiveUserCount());
    }

    /**
     * Admin endpoint returning aggregate active-user statistics grouped by gender.
     * Returns only counts — no user IDs, session IDs, or personal information.
     *
     * Currently publicly accessible. Security should be added when a full
     * authentication/authorization system is implemented.
     */
    @GetMapping("/api/admin/stats/active-users")
    public ActiveUserStatsResponse activeUserStats() {
        return activeUserTracker.getActiveUserStats();
    }
}
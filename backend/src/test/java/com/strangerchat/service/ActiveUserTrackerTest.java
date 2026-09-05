package com.strangerchat.service;

import com.strangerchat.dto.ActiveUserStatsResponse;
import com.strangerchat.dto.Gender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveUserTrackerTest {

    private final ActiveUserTracker tracker = new ActiveUserTracker();

    // ---------------------------------------------------------------
    // Existing behavior (backward-compatible)
    // ---------------------------------------------------------------

    @Test
    void duplicateConnectionsForOneUserCountOnce() {
        tracker.register("session-1", "user-1");
        tracker.register("session-2", "user-1");

        assertEquals(1, tracker.getActiveUserCount());

        tracker.unregister("session-1");
        assertEquals(1, tracker.getActiveUserCount());

        tracker.unregister("session-2");
        assertEquals(0, tracker.getActiveUserCount());
    }

    @Test
    void repeatedDisconnectDoesNotMakeCountNegative() {
        tracker.register("session-1", "user-1");

        tracker.unregister("session-1");
        tracker.unregister("session-1");
        tracker.unregister("unknown-session");

        assertEquals(0, tracker.getActiveUserCount());
    }

    @Test
    void concurrentUsersAreTrackedSafely() throws InterruptedException {
        int userCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Runnable> registrations = new ArrayList<>();

        for (int index = 0; index < userCount; index++) {
            String sessionId = "session-" + index;
            String userId = "user-" + index;
            registrations.add(() -> {
                await(start);
                tracker.register(sessionId, userId);
            });
        }

        registrations.forEach(executor::execute);
        start.countDown();
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.yield();
        }

        assertEquals(userCount, tracker.getActiveUserCount());
        for (int index = 0; index < userCount; index++) {
            tracker.unregister("session-" + index);
        }
        assertEquals(0, tracker.getActiveUserCount());
    }

    // ---------------------------------------------------------------
    // Gender-aware tracking
    // ---------------------------------------------------------------

    @Test
    void maleConnectionIncrementsMaleCount() {
        tracker.register("session-1", "user-1", Gender.MALE);

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(1, stats.getMale());
        assertEquals(0, stats.getFemale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void femaleConnectionIncrementsFemaleCount() {
        tracker.register("session-1", "user-1", Gender.FEMALE);

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(1, stats.getFemale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void disconnectDecrementsCorrectGender() {
        tracker.register("session-m", "user-m", Gender.MALE);
        tracker.register("session-f", "user-f", Gender.FEMALE);

        tracker.unregister("session-m");

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(1, stats.getFemale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void duplicateDisconnectDoesNotDecrementTwice() {
        tracker.register("session-1", "user-1", Gender.MALE);

        tracker.unregister("session-1");
        tracker.unregister("session-1");

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(0, stats.getFemale());
        assertEquals(0, stats.getTotalActiveUsers());
    }

    @Test
    void noCountBecomesNegative() {
        tracker.unregister("nonexistent-session");

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(0, stats.getFemale());
        assertEquals(0, stats.getTotalActiveUsers());
    }

    @Test
    void totalActiveUsersAlwaysEqualsMalePlusFemale() {
        tracker.register("session-1", "user-1", Gender.MALE);
        tracker.register("session-2", "user-2", Gender.FEMALE);
        tracker.register("session-3", "user-3", Gender.MALE);
        tracker.register("session-4", "user-4", Gender.FEMALE);
        tracker.register("session-5", "user-5", Gender.FEMALE);

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(stats.getMale() + stats.getFemale(), stats.getTotalActiveUsers());
        assertEquals(2, stats.getMale());
        assertEquals(3, stats.getFemale());
        assertEquals(5, stats.getTotalActiveUsers());
    }

    @Test
    void nullGenderIsHandledGracefully() {
        tracker.register("session-1", "user-1", null);

        // Session is still tracked as an active user
        assertEquals(1, tracker.getActiveUserCount());

        // But not counted in gender stats
        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(0, stats.getFemale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void reconnectionWorksCorrectly() {
        // User connects as MALE
        tracker.register("session-1", "user-1", Gender.MALE);
        assertEquals(1, tracker.getActiveUserStats().getMale());

        // User disconnects
        tracker.unregister("session-1");
        assertEquals(0, tracker.getActiveUserStats().getMale());

        // User reconnects with a new session (same user) as FEMALE
        tracker.register("session-2", "user-1", Gender.FEMALE);
        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(0, stats.getMale());
        assertEquals(1, stats.getFemale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void multipleConcurrentUsersWithGenderTrackedCorrectly() throws InterruptedException {
        int maleCount = 50;
        int femaleCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < maleCount; i++) {
            String sessionId = "session-m-" + i;
            String userId = "user-m-" + i;
            tasks.add(() -> {
                await(start);
                tracker.register(sessionId, userId, Gender.MALE);
            });
        }

        for (int i = 0; i < femaleCount; i++) {
            String sessionId = "session-f-" + i;
            String userId = "user-f-" + i;
            tasks.add(() -> {
                await(start);
                tracker.register(sessionId, userId, Gender.FEMALE);
            });
        }

        tasks.forEach(executor::execute);
        start.countDown();
        executor.shutdown();
        while (!executor.isTerminated()) {
            Thread.yield();
        }

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(maleCount, stats.getMale());
        assertEquals(femaleCount, stats.getFemale());
        assertEquals(maleCount + femaleCount, stats.getTotalActiveUsers());

        // Unregister all
        for (int i = 0; i < maleCount; i++) {
            tracker.unregister("session-m-" + i);
        }
        for (int i = 0; i < femaleCount; i++) {
            tracker.unregister("session-f-" + i);
        }

        ActiveUserStatsResponse afterStats = tracker.getActiveUserStats();
        assertEquals(0, afterStats.getMale());
        assertEquals(0, afterStats.getFemale());
        assertEquals(0, afterStats.getTotalActiveUsers());
    }

    @Test
    void duplicateConnectDoesNotDoubleCount() {
        tracker.register("session-1", "user-1", Gender.MALE);
        tracker.register("session-1", "user-1", Gender.MALE);

        ActiveUserStatsResponse stats = tracker.getActiveUserStats();
        assertEquals(1, stats.getMale());
        assertEquals(1, stats.getTotalActiveUsers());
    }

    @Test
    void setUserGenderUpdatesGenderForConnectedUser() {
        tracker.register("session-1", "user-1"); // initially connected without gender
        assertEquals(0, tracker.getActiveUserStats().getMale());
        assertEquals(1, tracker.getActiveUserStats().getTotalActiveUsers());

        tracker.setUserGender("user-1", Gender.MALE); // updated later when match/find is called
        assertEquals(1, tracker.getActiveUserStats().getMale());
        assertEquals(0, tracker.getActiveUserStats().getFemale());
        assertEquals(1, tracker.getActiveUserStats().getTotalActiveUsers());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Test thread was interrupted", exception);
        }
    }
}
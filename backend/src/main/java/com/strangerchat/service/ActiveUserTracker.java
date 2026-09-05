package com.strangerchat.service;

import com.strangerchat.dto.ActiveUserStatsResponse;
import com.strangerchat.dto.Gender;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActiveUserTracker {

    private final ConcurrentHashMap<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> userBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Gender> genderBySession = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Gender> genderByUser = new ConcurrentHashMap<>();

    public synchronized void register(String sessionId, String userId) {
        register(sessionId, userId, null);
    }

    public synchronized void register(String sessionId, String userId, Gender gender) {
        if (sessionId == null || userId == null || userId.isBlank()) {
            return;
        }

        userBySession.put(sessionId, userId);
        sessionsByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet())
                .add(sessionId);

        if (gender != null) {
            genderBySession.put(sessionId, gender);
            genderByUser.put(userId, gender);
        }
    }

    public synchronized void setUserGender(String userId, Gender gender) {
        if (userId == null || userId.isBlank() || gender == null) {
            return;
        }
        genderByUser.put(userId, gender);
        Set<String> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            for (String sessionId : sessions) {
                genderBySession.put(sessionId, gender);
            }
        }
    }

    public synchronized void unregister(String sessionId) {
        if (sessionId == null) {
            return;
        }

        String userId = userBySession.remove(sessionId);
        if (userId == null) {
            return;
        }

        genderBySession.remove(sessionId);

        sessionsByUser.computeIfPresent(userId, (ignored, sessions) -> {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                genderByUser.remove(userId);
                return null;
            }
            return sessions;
        });
    }

    public int getActiveUserCount() {
        return sessionsByUser.size();
    }

    /**
     * Returns aggregate active-user statistics grouped by gender.
     * Computed from the in-memory session and user gender maps.
     */
    public synchronized ActiveUserStatsResponse getActiveUserStats() {
        int male = 0;
        int female = 0;
        for (String userId : sessionsByUser.keySet()) {
            Gender gender = genderByUser.get(userId);
            if (gender == null) {
                Set<String> sessions = sessionsByUser.get(userId);
                if (sessions != null) {
                    for (String s : sessions) {
                        Gender g = genderBySession.get(s);
                        if (g != null) {
                            gender = g;
                            break;
                        }
                    }
                }
            }
            if (gender == Gender.MALE) {
                male++;
            } else if (gender == Gender.FEMALE) {
                female++;
            }
        }
        return new ActiveUserStatsResponse(getActiveUserCount(), male, female);
    }
}
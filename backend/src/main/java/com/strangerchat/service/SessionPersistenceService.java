package com.strangerchat.service;

import com.strangerchat.dto.Gender;
import com.strangerchat.entity.SessionEntity;
import com.strangerchat.entity.UserEntity;
import com.strangerchat.repository.SessionRepository;
import com.strangerchat.repository.UserRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessionPersistenceService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    // in-memory map of roomId -> SessionEntity id kept only long enough to close it out;
    // avoids a DB read-before-write on the hot path of ending a call.
    private final Map<String, SessionEntity> openSessions = new ConcurrentHashMap<>();

    public SessionPersistenceService(SessionRepository sessionRepository, UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
    }

    @Async
    public void touchUser(String userId, Gender gender) {
        userRepository.findById(userId).ifPresentOrElse(
                u -> { 
                    u.setLastSeen(Instant.now());
                    u.setGender(gender);
                    userRepository.save(u); 
                },
                () -> {
                    UserEntity user = new UserEntity(userId);
                    user.setGender(gender);
                    userRepository.save(user);
                }
        );
    }

    @Async
    public void recordSessionStart(String roomId, String userA, String userB, Gender userAGender, Gender userBGender) {
        SessionEntity session = new SessionEntity(roomId, userA, userB);
        session.setUserAGender(userAGender);
        session.setUserBGender(userBGender);
        sessionRepository.save(session);
        openSessions.put(roomId, session);
    }

    @Async
    public void recordSessionEnd(String roomId, SessionEntity.EndReason reason) {
        SessionEntity session = openSessions.remove(roomId);
        if (session == null) return;
        session.setEndedAt(Instant.now());
        session.setEndReason(reason);
        sessionRepository.save(session);
    }
}

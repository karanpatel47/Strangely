package com.strangerchat.controller;

import com.strangerchat.config.WebSocketSessionInterceptor;
import com.strangerchat.dto.Gender;
import com.strangerchat.dto.RoomEventMessage;
import com.strangerchat.entity.SessionEntity;
import com.strangerchat.service.MatchmakingService;
import com.strangerchat.service.SessionPersistenceService;
import com.strangerchat.service.ActiveUserTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.context.event.EventListener;

import java.security.Principal;
import java.util.Map;
import java.util.Optional;

/**
 * Covers the "user closes browser / refreshes / connection drops" cases that
 * the explicit /app/call/end and /app/match/next handlers can't catch, since
 * no message is sent when a socket just dies. Spring fires
 * SessionDisconnectEvent for both graceful STOMP DISCONNECT frames and
 * abrupt TCP-level drops, so this is the single place that guarantees a
 * left-behind peer is always notified and the room/queue state is cleaned up.
 */
@Component
public class DisconnectListener {

    private static final Logger log = LoggerFactory.getLogger(DisconnectListener.class);

    private final MatchmakingService matchmakingService;
    private final SessionPersistenceService persistenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ActiveUserTracker activeUserTracker;

    public DisconnectListener(MatchmakingService matchmakingService,
                               SessionPersistenceService persistenceService,
                               SimpMessagingTemplate messagingTemplate,
                               ActiveUserTracker activeUserTracker) {
        this.matchmakingService = matchmakingService;
        this.persistenceService = persistenceService;
        this.messagingTemplate = messagingTemplate;
        this.activeUserTracker = activeUserTracker;
    }

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        Gender gender = null;
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
        if (sessionAttributes != null) {
            Object genderAttr = sessionAttributes.get(WebSocketSessionInterceptor.GENDER_ATTR);
            if (genderAttr instanceof Gender g) {
                gender = g;
            }
        }

        if (gender == null) {
            org.springframework.messaging.simp.stomp.StompHeaderAccessor stompAccessor =
                    org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(event.getMessage());
            String genderHeader = stompAccessor.getFirstNativeHeader("gender");
            if (genderHeader != null && !genderHeader.isBlank()) {
                try {
                    gender = Gender.valueOf(genderHeader.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            }
        }

        activeUserTracker.register(accessor.getSessionId(), principal.getName(), gender);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        activeUserTracker.unregister(accessor.getSessionId());
        Principal principal = accessor.getUser();
        if (principal == null) return;

        String userId = principal.getName();
        log.debug("WS disconnect for user {}", userId);

        matchmakingService.removeFromQueue(userId);
        matchmakingService.clearPresence(userId);
        matchmakingService.clearUserGender(userId);

        Optional<String> roomId = matchmakingService.getCurrentRoom(userId);
        if (roomId.isEmpty()) return;

        Optional<String> peer = matchmakingService.getPeer(roomId.get(), userId);
        matchmakingService.leaveRoom(roomId.get(), userId);
        persistenceService.recordSessionEnd(roomId.get(), SessionEntity.EndReason.DISCONNECT);

        peer.ifPresent(p -> messagingTemplate.convertAndSendToUser(
                p, "/queue/room",
                new RoomEventMessage(RoomEventMessage.EventType.PEER_LEFT, roomId.get(), userId, false)));
    }
}

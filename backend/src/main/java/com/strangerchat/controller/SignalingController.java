package com.strangerchat.controller;

import com.strangerchat.dto.*;
import com.strangerchat.entity.SessionEntity;
import com.strangerchat.service.MatchmakingService;
import com.strangerchat.service.SessionPersistenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * WebSocket/STOMP protocol (see WebSocketConfig for the full doc comment).
 *
 * Every inbound message must include the caller's roomId (except match/find,
 * which has none yet) so the server can verify the caller is actually a
 * participant of that room before relaying anything - this prevents a
 * malicious client from injecting signaling/chat traffic into a room it
 * isn't part of.
 */

@Controller
public class SignalingController {

    private static final Logger log = LoggerFactory.getLogger(SignalingController.class);

    private final MatchmakingService matchmakingService;
    private final SessionPersistenceService persistenceService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.strangerchat.service.RateLimitService rateLimitService;

    public SignalingController(MatchmakingService matchmakingService,
                                SessionPersistenceService persistenceService,
                                SimpMessagingTemplate messagingTemplate,
                                com.strangerchat.service.RateLimitService rateLimitService) {
        this.matchmakingService = matchmakingService;
        this.persistenceService = persistenceService;
        this.messagingTemplate = messagingTemplate;
        this.rateLimitService = rateLimitService;
    }

    // ---------------------------------------------------------------
    // Matchmaking
    // ---------------------------------------------------------------

    @MessageMapping("/match/find")
    public void find(Principal principal) {
        String userId = principal.getName();
        if (!rateLimitService.allow("find:" + userId, 10, Duration.ofSeconds(10))) {
            sendError(userId, "RATE_LIMITED", "Too many matchmaking requests, slow down");
            return;
        }
        persistenceService.touchUser(userId);

        // If already in a room (e.g. duplicate click), just re-announce it instead of re-matching.
        Optional<String> existingRoom = matchmakingService.getCurrentRoom(userId);
        if (existingRoom.isPresent()) {
            log.debug("User {} already in room {}, ignoring duplicate find", userId, existingRoom.get());
            return;
        }

        Optional<MatchmakingService.Match> match = matchmakingService.findMatch(userId);
        if (match.isEmpty()) {
            sendToUser(userId, RoomEventMessage.waiting());
            return;
        }

        MatchmakingService.Match m = match.get();
        persistenceService.recordSessionStart(m.roomId(), m.peerId(), userId);
        
        // The peer (who was already waiting) initiates the SDP offer; the newly
        // arrived user waits for it. This avoids a "glare" where both send offers.
        sendToUser(m.peerId(), new RoomEventMessage(
                RoomEventMessage.EventType.MATCH_FOUND, m.roomId(), userId, true));
        sendToUser(userId, new RoomEventMessage(
                RoomEventMessage.EventType.MATCH_FOUND, m.roomId(), m.peerId(), false));
    }

    @MessageMapping("/match/next")
    public void next(Principal principal) {
        String userId = principal.getName();
        leaveCurrentRoom(userId, SessionEntity.EndReason.NEXT, RoomEventMessage.EventType.PEER_NEXT);
        // Immediately re-enter matchmaking.
        find(principal);
    }

    // ---------------------------------------------------------------
    // WebRTC signaling relay (server never inspects SDP/ICE contents)
    // ---------------------------------------------------------------

    @MessageMapping("/call/offer")
    public void offer(@Valid @Payload SignalMessage message, Principal principal) {
        relaySignal(message, principal, SignalType.OFFER);
    }

    @MessageMapping("/call/answer")
    public void answer(@Valid @Payload SignalMessage message, Principal principal) {
        relaySignal(message, principal, SignalType.ANSWER);
    }

    @MessageMapping("/call/ice")
    public void ice(@Valid @Payload SignalMessage message, Principal principal) {
        relaySignal(message, principal, SignalType.ICE_CANDIDATE);
    }

    private void relaySignal(SignalMessage message, Principal principal, SignalType expectedType) {
        String userId = principal.getName();
        String roomId = message.getRoomId();

        if (!isParticipant(roomId, userId)) {
            sendError(userId, "NOT_IN_ROOM", "You are not a participant of room " + roomId);
            return;
        }

        Optional<String> peer = matchmakingService.getPeer(roomId, userId);
        if (peer.isEmpty()) {
            sendError(userId, "PEER_UNAVAILABLE", "Peer is no longer in the room");
            return;
        }

        SignalMessage forward = new SignalMessage(roomId, expectedType, message.getPayload());
        messagingTemplate.convertAndSendToUser(peer.get(), "/queue/signal", forward);
    }

    // ---------------------------------------------------------------
    // Chat
    // ---------------------------------------------------------------

    @MessageMapping("/chat/send")
    public void chat(@Valid @Payload ChatMessageDto message, Principal principal) {
        String userId = principal.getName();
        String roomId = message.getRoomId();

        if (!rateLimitService.allow("chat:" + userId, 20, Duration.ofSeconds(10))) {
            sendError(userId, "RATE_LIMITED", "You're sending messages too fast");
            return;
        }

        if (!isParticipant(roomId, userId)) {
            sendError(userId, "NOT_IN_ROOM", "You are not a participant of room " + roomId);
            return;
        }
        if (message.getContent() != null && message.getContent().length() > 1000) {
            sendError(userId, "MESSAGE_TOO_LONG", "Message exceeds max length");
            return;
        }
        // Strip control/HTML-unsafe characters; frontend also escapes on render.
        message.setContent(sanitize(message.getContent()));

        message.setSenderId(userId);
        message.setTimestamp(Instant.now().toEpochMilli());

        Optional<String> peer = matchmakingService.getPeer(roomId, userId);
        peer.ifPresent(p -> messagingTemplate.convertAndSendToUser(p, "/queue/chat", message));
        // Echo back to sender too, so both sides render from the same source of truth
        // and the sender's own message shows the server-assigned timestamp.
        sendToUser(userId, message);
    }

    // ---------------------------------------------------------------
    // End call
    // ---------------------------------------------------------------

    @MessageMapping("/call/end")
    public void end(Principal principal) {
        String userId = principal.getName();
        leaveCurrentRoom(userId, SessionEntity.EndReason.END_CALL, RoomEventMessage.EventType.CALL_ENDED);
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void leaveCurrentRoom(String userId, SessionEntity.EndReason reason, RoomEventMessage.EventType notifyType) {
        Optional<String> roomId = matchmakingService.getCurrentRoom(userId);
        matchmakingService.removeFromQueue(userId);

        if (roomId.isEmpty()) {
            return;
        }

        Optional<String> peer = matchmakingService.getPeer(roomId.get(), userId);
        matchmakingService.leaveRoom(roomId.get(), userId);
        persistenceService.recordSessionEnd(roomId.get(), reason);

        peer.ifPresent(p -> sendToUser(p, new RoomEventMessage(notifyType, roomId.get(), userId, false)));
    }

    private boolean isParticipant(String roomId, String userId) {
        if (roomId == null) return false;
        return matchmakingService.getPeer(roomId, userId).isPresent() || matchmakingService.roomExists(roomId);
    }

    private void sendToUser(String userId, Object payload) {
        String destination = payload instanceof ChatMessageDto ? "/queue/chat" : "/queue/room";
        messagingTemplate.convertAndSendToUser(userId, destination, payload);
    }

    private void sendError(String userId, String code, String message) {
        messagingTemplate.convertAndSendToUser(userId, "/queue/errors", new ErrorMessage(code, message));
    }

    private String sanitize(String content) {
        if (content == null) return null;
        // Remove control characters and trim; HTML-escaping for render is the frontend's job
        // (React escapes text content by default), this just strips raw control bytes.
        return content.replaceAll("\\p{Cntrl}", "").trim();
    }
}

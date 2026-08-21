package com.strangerchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP over WebSocket configuration.
 *
 * Protocol (application destinations, prefixed with /app):
 *   /app/match/find        -> enter matchmaking queue
 *   /app/match/next        -> leave current room, re-enter queue
 *   /app/call/offer        -> forward SDP offer to room peer
 *   /app/call/answer       -> forward SDP answer to room peer
 *   /app/call/ice          -> forward ICE candidate to room peer
 *   /app/call/end          -> end call, leave room
 *   /app/chat/send         -> send chat message to room
 *
 * Subscriptions (broker topics, prefixed with /topic):
 *   /topic/room/{roomId}   -> all room events (match found, signaling, chat, peer left)
 *   /user/queue/errors     -> user-specific error channel
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final WebSocketSessionInterceptor sessionInterceptor;

    public WebSocketConfig(WebSocketSessionInterceptor sessionInterceptor) {
        this.sessionInterceptor = sessionInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(sessionInterceptor)
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();

        // Native WebSocket endpoint (no SockJS) for clients that support it directly
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .addInterceptors(sessionInterceptor)
                .setHandshakeHandler(new UserHandshakeHandler());
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        // Caps well above what any legitimate SDP/ICE/chat payload needs, but
        // prevents a malicious client from sending huge frames to exhaust memory.
        registration.setMessageSizeLimit(128 * 1024);
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setSendTimeLimit(15_000);
    }
}

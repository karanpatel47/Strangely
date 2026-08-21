package com.strangerchat.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.UUID;

/**
 * Assigns each incoming WS handshake an anonymous userId (no login required for MVP).
 * Reuses a client-supplied id from a cookie/header if present so reconnects can be
 * recognized, otherwise mints a new UUID. This id becomes the STOMP Principal name
 * via DefaultHandshakeHandler + this attribute.
 */
@Component
public class WebSocketSessionInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            userId = httpRequest.getParameter("userId");
        }
        if (userId == null || userId.isBlank()) {
            userId = UUID.randomUUID().toString();
        }
        attributes.put(USER_ID_ATTR, userId);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}

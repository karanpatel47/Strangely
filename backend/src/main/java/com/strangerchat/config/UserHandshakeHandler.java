package com.strangerchat.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        String userId = (String) attributes.get(WebSocketSessionInterceptor.USER_ID_ATTR);
        if (userId == null) {
            userId = UUID.randomUUID().toString();
        }
        return new StompPrincipal(userId);
    }
}

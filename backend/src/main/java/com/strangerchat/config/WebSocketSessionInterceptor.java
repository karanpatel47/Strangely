package com.strangerchat.config;

import com.strangerchat.dto.Gender;
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
 * Assigns each incoming WebSocket handshake an anonymous userId.
 *
 * The frontend supplies the userId as a query parameter:
 *
 *   /ws?userId=<uuid>
 *
 * If the supplied value is missing or invalid, a new UUID is generated.
 *
 * The resulting ID is stored as a handshake attribute and is later used
 * by the handshake handler as the STOMP Principal name.
 *
 * Optionally extracts a "gender" query parameter (MALE or FEMALE) and
 * stores it as a handshake attribute for active-user statistics tracking.
 */
@Component
public class WebSocketSessionInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "userId";
    public static final String GENDER_ATTR = "gender";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {

        String userId = null;
        String genderParam = null;

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest =
                    servletRequest.getServletRequest();

            userId = httpRequest.getParameter("userId");
            genderParam = httpRequest.getParameter("gender");
        }

        if (!isValidUuid(userId)) {
            userId = UUID.randomUUID().toString();
        }

        attributes.put(USER_ID_ATTR, userId);

        Gender gender = parseGender(genderParam);
        if (gender != null) {
            attributes.put(GENDER_ATTR, gender);
        }

        return true;
    }

    private boolean isValidUuid(String userId) {
        if (userId == null || userId.isBlank()) {
            return false;
        }

        try {
            UUID.fromString(userId);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Validates the gender query parameter against the Gender enum.
     * Returns null if the value is missing, blank, or not a valid enum constant.
     */
    private Gender parseGender(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op.
    }
}
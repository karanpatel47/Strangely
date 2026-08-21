package com.strangerchat.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

/**
 * Prevents malformed/oversized STOMP frames or unhandled exceptions in
 * @MessageMapping handlers from tearing down the WebSocket session silently -
 * the client gets a STOMP ERROR frame it can react to instead of just
 * disconnecting with no explanation.
 */
@Component
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(StompErrorHandler.class);

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        log.warn("STOMP processing error: {}", ex.getMessage());
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}

package com.strangerchat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

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

        String[] origins = allowedOrigins
                .split(",")
                ;

        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .addInterceptors(sessionInterceptor)
                .setHandshakeHandler(new UserHandshakeHandler())
                .withSockJS();

        registry.addEndpoint("/ws")
                .setAllowedOrigins(origins)
                .addInterceptors(sessionInterceptor)
                .setHandshakeHandler(new UserHandshakeHandler());
    }

    @Override
    public void configureWebSocketTransport(
            WebSocketTransportRegistration registration) {

        registration.setMessageSizeLimit(128 * 1024);
        registration.setSendBufferSizeLimit(512 * 1024);
        registration.setSendTimeLimit(15_000);
    }
}
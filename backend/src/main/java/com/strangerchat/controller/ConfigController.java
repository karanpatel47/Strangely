package com.strangerchat.controller;

import com.strangerchat.dto.IceServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Exposes STUN/TURN server configuration to the frontend at runtime.
 *
 * TURN credentials are provided through backend environment variables and
 * are never hardcoded into the frontend build.
 */
@RestController
public class ConfigController {

    @Value("${app.webrtc.stun-server}")
    private String stunServer;

    @Value("${app.webrtc.turn-servers:}")
    private String turnServers;

    @Value("${app.webrtc.turn-username:}")
    private String turnUsername;

    @Value("${app.webrtc.turn-password:}")
    private String turnPassword;

    @GetMapping("/api/config/ice-servers")
    public IceServerConfig iceServers() {

        List<IceServerConfig.IceServer> servers = new ArrayList<>();

        // STUN
        if (stunServer != null && !stunServer.isBlank()) {
            servers.add(
                    new IceServerConfig.IceServer(
                            List.of(stunServer),
                            null,
                            null
                    )
            );
        }

        // TURN
        if (turnServers != null && !turnServers.isBlank()) {

            List<String> urls = Arrays.stream(turnServers.split(","))
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .toList();

            if (!urls.isEmpty()) {
                servers.add(
                        new IceServerConfig.IceServer(
                                urls,
                                turnUsername,
                                turnPassword
                        )
                );
            }
        }

        return new IceServerConfig(servers);
    }
}
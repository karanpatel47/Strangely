package com.strangerchat.controller;

import com.strangerchat.dto.IceServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

        System.out.println("========== WEBRTC CONFIG ==========");
        System.out.println("STUN: " + stunServer);
        System.out.println("TURN SERVERS PRESENT: "
                + (turnServers != null && !turnServers.isBlank()));
        System.out.println("TURN SERVER COUNT: "
                + (turnServers == null || turnServers.isBlank()
                ? 0
                : turnServers.split(",").length));
        System.out.println("TURN USERNAME PRESENT: "
                + (turnUsername != null && !turnUsername.isBlank()));
        System.out.println("TURN PASSWORD PRESENT: "
                + (turnPassword != null && !turnPassword.isBlank()));
        System.out.println("===================================");

        List<IceServerConfig.IceServer> servers = new ArrayList<>();

        if (stunServer != null && !stunServer.isBlank()) {
            servers.add(
                    new IceServerConfig.IceServer(
                            List.of(stunServer),
                            null,
                            null
                    )
            );
        }

        if (turnServers != null && !turnServers.isBlank()) {

            List<String> urls = Arrays.stream(turnServers.split(","))
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .toList();

            servers.add(
                    new IceServerConfig.IceServer(
                            urls,
                            turnUsername,
                            turnPassword
                    )
            );
        }

        return new IceServerConfig(servers);
    }
}
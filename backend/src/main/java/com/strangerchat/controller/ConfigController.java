package com.strangerchat.controller;

import com.strangerchat.dto.IceServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * Exposes STUN/TURN server config to the frontend at runtime, so ICE server
 * credentials never need to be hardcoded/bundled into the client build.
 */
@RestController
public class ConfigController {

    @Value("${app.webrtc.stun-server}")
    private String stunServer;

    @Value("${app.webrtc.turn-server:}")
    private String turnServer;

    @Value("${app.webrtc.turn-username:}")
    private String turnUsername;

    @Value("${app.webrtc.turn-password:}")
    private String turnPassword;

    @GetMapping("/api/config/ice-servers")
    public IceServerConfig iceServers() {
        List<IceServerConfig.IceServer> servers = new ArrayList<>();
        servers.add(new IceServerConfig.IceServer(List.of(stunServer), null, null));
        if (turnServer != null && !turnServer.isBlank()) {
            servers.add(new IceServerConfig.IceServer(List.of(turnServer), turnUsername, turnPassword));
        }
        return new IceServerConfig(servers);
    }
}

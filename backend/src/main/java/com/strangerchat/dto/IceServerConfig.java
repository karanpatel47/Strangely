package com.strangerchat.dto;

import java.util.List;

public class IceServerConfig {
    private List<IceServer> iceServers;

    public IceServerConfig() {}

    public IceServerConfig(List<IceServer> iceServers) {
        this.iceServers = iceServers;
    }

    public List<IceServer> getIceServers() { return iceServers; }
    public void setIceServers(List<IceServer> iceServers) { this.iceServers = iceServers; }

    public static class IceServer {
        private List<String> urls;
        private String username;
        private String credential;

        public IceServer() {}

        public IceServer(List<String> urls, String username, String credential) {
            this.urls = urls;
            this.username = username;
            this.credential = credential;
        }

        public List<String> getUrls() { return urls; }
        public void setUrls(List<String> urls) { this.urls = urls; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getCredential() { return credential; }
        public void setCredential(String credential) { this.credential = credential; }
    }
}

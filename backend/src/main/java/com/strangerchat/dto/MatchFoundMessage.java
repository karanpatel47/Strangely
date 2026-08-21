package com.strangerchat.dto;

public class MatchFoundMessage {
    private String roomId;
    private String peerId;
    private boolean initiator;

    public MatchFoundMessage() {}

    public MatchFoundMessage(String roomId, String peerId, boolean initiator) {
        this.roomId = roomId;
        this.peerId = peerId;
        this.initiator = initiator;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }
    public boolean isInitiator() { return initiator; }
    public void setInitiator(boolean initiator) { this.initiator = initiator; }
}

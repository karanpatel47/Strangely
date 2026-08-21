package com.strangerchat.dto;

public class RoomEventMessage {

    public enum EventType {
        MATCH_FOUND, PEER_LEFT, PEER_NEXT, CALL_ENDED, WAITING
    }

    private EventType type;
    private String roomId;
    private String peerId;
    private boolean initiator;

    public RoomEventMessage() {}

    public RoomEventMessage(EventType type, String roomId, String peerId, boolean initiator) {
        this.type = type;
        this.roomId = roomId;
        this.peerId = peerId;
        this.initiator = initiator;
    }

    public static RoomEventMessage waiting() {
        return new RoomEventMessage(EventType.WAITING, null, null, false);
    }

    public EventType getType() { return type; }
    public void setType(EventType type) { this.type = type; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getPeerId() { return peerId; }
    public void setPeerId(String peerId) { this.peerId = peerId; }
    public boolean isInitiator() { return initiator; }
    public void setInitiator(boolean initiator) { this.initiator = initiator; }
}

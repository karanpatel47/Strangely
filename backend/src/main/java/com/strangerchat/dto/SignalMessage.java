package com.strangerchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Generic WebRTC signaling envelope exchanged over /app/call/offer,
 * /app/call/answer, /app/call/ice. The "payload" carries the raw SDP
 * string or a serialized ICE candidate (JSON string) - the backend
 * never inspects it, only relays it to the other participant in the room.
 */
public class SignalMessage {

    @NotBlank
    private String roomId;

    @NotNull
    private SignalType type;

    @NotBlank
    private String payload;

    public SignalMessage() {}

    public SignalMessage(String roomId, SignalType type, String payload) {
        this.roomId = roomId;
        this.type = type;
        this.payload = payload;
    }

    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public SignalType getType() { return type; }
    public void setType(SignalType type) { this.type = type; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
}

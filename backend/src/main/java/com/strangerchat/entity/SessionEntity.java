package com.strangerchat.entity;

import com.strangerchat.dto.Gender;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * A completed or in-progress pairing between two users. Created when a
 * match is made, closed (endedAt set) when either user leaves/clicks
 * Next/ends the call. No media content is ever stored here - only
 * metadata needed for abuse investigation and basic analytics.
 */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String roomId;

    @Column(nullable = false)
    private String userAId;

    @Column(nullable = false)
    private String userBId;

    @Enumerated(EnumType.STRING)
    private Gender userAGender;

    @Enumerated(EnumType.STRING)
    private Gender userBGender;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    private EndReason endReason;

    public enum EndReason { NEXT, END_CALL, DISCONNECT }

    public SessionEntity() {}

    public SessionEntity(String roomId, String userAId, String userBId) {
        this.roomId = roomId;
        this.userAId = userAId;
        this.userBId = userBId;
        this.startedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getUserAId() { return userAId; }
    public void setUserAId(String userAId) { this.userAId = userAId; }
    public String getUserBId() { return userBId; }
    public void setUserBId(String userBId) { this.userBId = userBId; }
    public Gender getUserAGender() { return userAGender; }
    public void setUserAGender(Gender userAGender) { this.userAGender = userAGender; }
    public Gender getUserBGender() { return userBGender; }
    public void setUserBGender(Gender userBGender) { this.userBGender = userBGender; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public EndReason getEndReason() { return endReason; }
    public void setEndReason(EndReason endReason) { this.endReason = endReason; }
}

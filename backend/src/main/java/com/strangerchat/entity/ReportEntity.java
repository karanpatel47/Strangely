package com.strangerchat.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reports")
public class ReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String reporterId;

    @Column(nullable = false)
    private String reportedUserId;

    @Column(nullable = false)
    private String roomId;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private Instant createdAt;

    public ReportEntity() {}

    public ReportEntity(String reporterId, String reportedUserId, String roomId, String reason) {
        this.reporterId = reporterId;
        this.reportedUserId = reportedUserId;
        this.roomId = roomId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

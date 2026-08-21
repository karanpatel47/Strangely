package com.strangerchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportRequest {
    @NotBlank
    private String reportedUserId;

    @NotBlank
    private String roomId;

    @Size(max = 500)
    private String reason;

    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getRoomId() { return roomId; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

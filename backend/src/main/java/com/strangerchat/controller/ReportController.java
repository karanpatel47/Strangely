package com.strangerchat.controller;

import com.strangerchat.dto.ReportRequest;
import com.strangerchat.entity.ReportEntity;
import com.strangerchat.repository.ReportRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Lets a client flag a stranger for abusive behavior. Phase 1: just persists
 * the report for manual/later review - no automated moderation action yet.
 */
@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportRepository reportRepository;

    public ReportController(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    @PostMapping
    public ResponseEntity<Void> report(@Valid @RequestBody ReportRequest request,
                                        @RequestHeader(value = "X-User-Id", required = false) String reporterId) {
        String reporter = reporterId != null ? reporterId : "unknown";
        reportRepository.save(new ReportEntity(reporter, request.getReportedUserId(), request.getRoomId(), request.getReason()));
        return ResponseEntity.accepted().build();
    }
}

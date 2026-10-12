package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.response.AuditResponse;
import com.moveinsync.roster.dto.response.ConflictReportResponse;
import com.moveinsync.roster.dto.response.UtilizationResponse;
import com.moveinsync.roster.service.ReportingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reporting", description = "Conflict, audit history, and resource utilization reports")
public class ReportController {

    private final ReportingService reportingService;

    public ReportController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/conflicts")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Retrieve conflict report and unfilled duty analysis",
               description = "Breaks down assigned and unfilled duties with detailed rejection causes.")
    public ResponseEntity<ConflictReportResponse> getConflicts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ConflictReportResponse report = reportingService.getConflictReport(startTime, endTime);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/utilization")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Retrieve combined resource utilization report",
               description = "Calculates scheduled hours, utilization percentage against baseline, and overtime hours.")
    public ResponseEntity<UtilizationResponse> getUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        UtilizationResponse report = reportingService.getUtilizationReport(startTime, endTime);
        return ResponseEntity.ok(report);
    }

    @GetMapping("/utilization/drivers")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Retrieve driver utilization report",
               description = "Lists individual driver duty counts, scheduled hours, utilization %, and overtime.")
    public ResponseEntity<List<UtilizationResponse.DriverUtilization>> getDriverUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        UtilizationResponse report = reportingService.getUtilizationReport(startTime, endTime);
        return ResponseEntity.ok(report.getDriverUtilizations());
    }

    @GetMapping("/utilization/vehicles")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Retrieve vehicle utilization report",
               description = "Lists individual vehicle duty counts, scheduled hours, and utilization %.")
    public ResponseEntity<List<UtilizationResponse.VehicleUtilization>> getVehicleUtilization(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        UtilizationResponse report = reportingService.getUtilizationReport(startTime, endTime);
        return ResponseEntity.ok(report.getVehicleUtilizations());
    }

    @GetMapping("/duties/{dutyId}/audit")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Retrieve assignment audit history for a duty",
               description = "Returns complete chronological change history, acting user, and reasons.")
    public ResponseEntity<List<AuditResponse>> getDutyAuditHistory(@PathVariable Long dutyId) {
        List<AuditResponse> history = reportingService.getDutyAuditHistory(dutyId);
        return ResponseEntity.ok(history);
    }
}

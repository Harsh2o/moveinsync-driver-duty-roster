package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.request.DutyRequest;
import com.moveinsync.roster.dto.request.DutyStatusUpdateRequest;
import com.moveinsync.roster.dto.response.DutyResponse;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.service.DutyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/duties")
@Tag(name = "Duties", description = "Duty requirement definition and lifecycle status")
public class DutyController {

    private final DutyService dutyService;

    public DutyController(DutyService dutyService) {
        this.dutyService = dutyService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a duty requirement", description = "Admin only. Registers a duty requirement to be assigned.")
    public ResponseEntity<DutyResponse> createDuty(@Valid @RequestBody DutyRequest request) {
        DutyResponse response = dutyService.createDuty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'EMPLOYEE')")
    @Operation(summary = "Get duty by ID", description = "Returns duty details including current assignment state and assigned driver/vehicle.")
    public ResponseEntity<DutyResponse> getDuty(@PathVariable Long id) {
        DutyResponse response = dutyService.getDuty(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER', 'EMPLOYEE')")
    @Operation(summary = "List duties with filtering and pagination", description = "Filter by date range and assignment state (ASSIGNED/UNASSIGNED).")
    public ResponseEntity<PageResponse<DutyResponse>> listDuties(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) String assignmentState) {
        PageResponse<DutyResponse> response = dutyService.listDuties(page, size, startTime, endTime, assignmentState);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update duty status", description = "Admin only. Allowed transitions: ACTIVE -> COMPLETED, ACTIVE -> CANCELLED.")
    public ResponseEntity<DutyResponse> updateDutyStatus(
            @PathVariable Long id,
            @Valid @RequestBody DutyStatusUpdateRequest request) {
        DutyResponse response = dutyService.updateDutyStatus(id, request);
        return ResponseEntity.ok(response);
    }
}

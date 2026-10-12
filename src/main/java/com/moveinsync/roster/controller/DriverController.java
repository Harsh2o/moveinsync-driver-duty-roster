package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.request.DriverRequest;
import com.moveinsync.roster.dto.response.DriverResponse;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/drivers")
@Tag(name = "Drivers", description = "Driver management and lifecycle operations")
public class DriverController {

    private final DriverService driverService;

    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new driver", description = "Admin only. Creates a driver record with licence and health certificates.")
    public ResponseEntity<DriverResponse> createDriver(@Valid @RequestBody DriverRequest request) {
        DriverResponse response = driverService.createDriver(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "List drivers with pagination", description = "Supports filtering by active status.")
    public ResponseEntity<PageResponse<DriverResponse>> listDrivers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active) {
        PageResponse<DriverResponse> response = driverService.listDrivers(page, size, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Get driver details", description = "Returns driver profile excluding sensitive personal contact info.")
    public ResponseEntity<DriverResponse> getDriver(@PathVariable Long id) {
        DriverResponse response = driverService.getDriver(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update driver details", description = "Admin only. Updates driver credentials and validity dates.")
    public ResponseEntity<DriverResponse> updateDriver(
            @PathVariable Long id,
            @Valid @RequestBody DriverRequest request) {
        DriverResponse response = driverService.updateDriver(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate driver", description = "Admin only. Changes driver active availability status.")
    public ResponseEntity<DriverResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        DriverResponse response = driverService.toggleDriverActive(id, active);
        return ResponseEntity.ok(response);
    }
}

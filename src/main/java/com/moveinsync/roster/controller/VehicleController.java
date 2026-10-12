package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.request.VehicleRequest;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.dto.response.VehicleResponse;
import com.moveinsync.roster.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management and lifecycle operations")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new vehicle", description = "Admin only. Creates vehicle with type-policy-derived required licence.")
    public ResponseEntity<VehicleResponse> createVehicle(@Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "List vehicles with pagination", description = "Supports filtering by active status.")
    public ResponseEntity<PageResponse<VehicleResponse>> listVehicles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Boolean active) {
        PageResponse<VehicleResponse> response = vehicleService.listVehicles(page, size, active);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'APPROVER')")
    @Operation(summary = "Get vehicle details", description = "Returns vehicle configuration and registration details.")
    public ResponseEntity<VehicleResponse> getVehicle(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicle(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update vehicle details", description = "Admin only. Updates vehicle registration or type.")
    public ResponseEntity<VehicleResponse> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activate or deactivate vehicle", description = "Admin only. Toggles vehicle availability status.")
    public ResponseEntity<VehicleResponse> toggleStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        VehicleResponse response = vehicleService.toggleVehicleActive(id, active);
        return ResponseEntity.ok(response);
    }
}

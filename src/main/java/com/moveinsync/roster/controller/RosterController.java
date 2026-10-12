package com.moveinsync.roster.controller;

import com.moveinsync.roster.dto.request.ReassignmentRequest;
import com.moveinsync.roster.dto.request.RosterGenerationRequest;
import com.moveinsync.roster.dto.response.ReassignmentResponse;
import com.moveinsync.roster.dto.response.RosterGenerationResponse;
import com.moveinsync.roster.service.RosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/roster")
@Tag(name = "Roster Operations", description = "Roster generation and emergency driver reassignment")
public class RosterController {

    private final RosterService rosterService;

    public RosterController(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate duty roster using constrained scarcity-first algorithm",
               description = "Assigns eligible drivers and vehicles to unassigned active duties in the specified period.")
    public ResponseEntity<RosterGenerationResponse> generateRoster(
            @Valid @RequestBody RosterGenerationRequest request) {
        RosterGenerationResponse response = rosterService.generateRoster(
                request.getStartTime(), request.getEndTime());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reassign duty due to driver dropout",
               description = "Finds eligible replacement driver and vehicle, records audit entry, and maintains state consistency.")
    public ResponseEntity<ReassignmentResponse> reassignDuty(
            @Valid @RequestBody ReassignmentRequest request) {
        ReassignmentResponse response = rosterService.reassignDuty(request);
        return ResponseEntity.ok(response);
    }
}

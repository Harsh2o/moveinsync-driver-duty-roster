package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class DutyRequest {
    @NotBlank(message = "Vehicle type is required")
    private String requiredVehicleType;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private Long assignedDriverId;
    private Long assignedVehicleId;

    public DutyRequest() {
    }

    public DutyRequest(String requiredVehicleType, LocalDateTime startTime, LocalDateTime endTime, Long assignedDriverId, Long assignedVehicleId) {
        this.requiredVehicleType = requiredVehicleType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.assignedDriverId = assignedDriverId;
        this.assignedVehicleId = assignedVehicleId;
    }

    public String getRequiredVehicleType() {
        return requiredVehicleType;
    }

    public void setRequiredVehicleType(String requiredVehicleType) {
        this.requiredVehicleType = requiredVehicleType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getAssignedDriverId() {
        return assignedDriverId;
    }

    public void setAssignedDriverId(Long assignedDriverId) {
        this.assignedDriverId = assignedDriverId;
    }

    public Long getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(Long assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }
}

package com.moveinsync.roster.dto.response;

import java.time.LocalDateTime;

public class DutyResponse {
    private Long id;
    private String requiredVehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long assignedDriverId;
    private String assignedDriverName;
    private Long assignedVehicleId;
    private String assignedVehicleRegistration;
    private String dutyStatus;
    private String assignmentState;
    private String unfilledReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DutyResponse() {
    }

    public DutyResponse(Long id, String requiredVehicleType, LocalDateTime startTime, LocalDateTime endTime, Long assignedDriverId, String assignedDriverName, Long assignedVehicleId, String assignedVehicleRegistration, String dutyStatus, String assignmentState, String unfilledReason, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.requiredVehicleType = requiredVehicleType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.assignedDriverId = assignedDriverId;
        this.assignedDriverName = assignedDriverName;
        this.assignedVehicleId = assignedVehicleId;
        this.assignedVehicleRegistration = assignedVehicleRegistration;
        this.dutyStatus = dutyStatus;
        this.assignmentState = assignmentState;
        this.unfilledReason = unfilledReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAssignedDriverName() {
        return assignedDriverName;
    }

    public void setAssignedDriverName(String assignedDriverName) {
        this.assignedDriverName = assignedDriverName;
    }

    public Long getAssignedVehicleId() {
        return assignedVehicleId;
    }

    public void setAssignedVehicleId(Long assignedVehicleId) {
        this.assignedVehicleId = assignedVehicleId;
    }

    public String getAssignedVehicleRegistration() {
        return assignedVehicleRegistration;
    }

    public void setAssignedVehicleRegistration(String assignedVehicleRegistration) {
        this.assignedVehicleRegistration = assignedVehicleRegistration;
    }

    public String getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(String dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public String getAssignmentState() {
        return assignmentState;
    }

    public void setAssignmentState(String assignmentState) {
        this.assignmentState = assignmentState;
    }

    public String getUnfilledReason() {
        return unfilledReason;
    }

    public void setUnfilledReason(String unfilledReason) {
        this.unfilledReason = unfilledReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

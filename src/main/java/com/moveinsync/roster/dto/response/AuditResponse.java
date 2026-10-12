package com.moveinsync.roster.dto.response;

import java.time.LocalDateTime;

public class AuditResponse {
    private Long id;
    private Long dutyId;
    private Long previousDriverId;
    private Long previousVehicleId;
    private Long newDriverId;
    private Long newVehicleId;
    private String action;
    private String reason;
    private String actingUser;
    private LocalDateTime createdAt;

    public AuditResponse() {
    }

    public AuditResponse(Long id, Long dutyId, Long previousDriverId, Long previousVehicleId, Long newDriverId, Long newVehicleId, String action, String reason, String actingUser, LocalDateTime createdAt) {
        this.id = id;
        this.dutyId = dutyId;
        this.previousDriverId = previousDriverId;
        this.previousVehicleId = previousVehicleId;
        this.newDriverId = newDriverId;
        this.newVehicleId = newVehicleId;
        this.action = action;
        this.reason = reason;
        this.actingUser = actingUser;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDutyId() {
        return dutyId;
    }

    public void setDutyId(Long dutyId) {
        this.dutyId = dutyId;
    }

    public Long getPreviousDriverId() {
        return previousDriverId;
    }

    public void setPreviousDriverId(Long previousDriverId) {
        this.previousDriverId = previousDriverId;
    }

    public Long getPreviousVehicleId() {
        return previousVehicleId;
    }

    public void setPreviousVehicleId(Long previousVehicleId) {
        this.previousVehicleId = previousVehicleId;
    }

    public Long getNewDriverId() {
        return newDriverId;
    }

    public void setNewDriverId(Long newDriverId) {
        this.newDriverId = newDriverId;
    }

    public Long getNewVehicleId() {
        return newVehicleId;
    }

    public void setNewVehicleId(Long newVehicleId) {
        this.newVehicleId = newVehicleId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActingUser() {
        return actingUser;
    }

    public void setActingUser(String actingUser) {
        this.actingUser = actingUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

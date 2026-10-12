package com.moveinsync.roster.dto.response;

public class ReassignmentResponse {
    private Long dutyId;
    private boolean reassigned;
    private Long previousDriverId;
    private Long newDriverId;
    private Long previousVehicleId;
    private Long newVehicleId;
    private String status;
    private String message;

    public ReassignmentResponse() {
    }

    public ReassignmentResponse(Long dutyId, boolean reassigned, Long previousDriverId, Long newDriverId, Long previousVehicleId, Long newVehicleId, String status, String message) {
        this.dutyId = dutyId;
        this.reassigned = reassigned;
        this.previousDriverId = previousDriverId;
        this.newDriverId = newDriverId;
        this.previousVehicleId = previousVehicleId;
        this.newVehicleId = newVehicleId;
        this.status = status;
        this.message = message;
    }

    public Long getDutyId() {
        return dutyId;
    }

    public void setDutyId(Long dutyId) {
        this.dutyId = dutyId;
    }

    public boolean isReassigned() {
        return reassigned;
    }

    public void setReassigned(boolean reassigned) {
        this.reassigned = reassigned;
    }

    public Long getPreviousDriverId() {
        return previousDriverId;
    }

    public void setPreviousDriverId(Long previousDriverId) {
        this.previousDriverId = previousDriverId;
    }

    public Long getNewDriverId() {
        return newDriverId;
    }

    public void setNewDriverId(Long newDriverId) {
        this.newDriverId = newDriverId;
    }

    public Long getPreviousVehicleId() {
        return previousVehicleId;
    }

    public void setPreviousVehicleId(Long previousVehicleId) {
        this.previousVehicleId = previousVehicleId;
    }

    public Long getNewVehicleId() {
        return newVehicleId;
    }

    public void setNewVehicleId(Long newVehicleId) {
        this.newVehicleId = newVehicleId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

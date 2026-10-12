package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ReassignmentRequest {
    @NotNull(message = "Duty ID is required")
    private Long dutyId;

    @NotNull(message = "Unavailable driver ID is required")
    private Long unavailableDriverId;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String idempotencyKey;

    public ReassignmentRequest() {
    }

    public ReassignmentRequest(Long dutyId, Long unavailableDriverId, String reason, String idempotencyKey) {
        this.dutyId = dutyId;
        this.unavailableDriverId = unavailableDriverId;
        this.reason = reason;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getDutyId() {
        return dutyId;
    }

    public void setDutyId(Long dutyId) {
        this.dutyId = dutyId;
    }

    public Long getUnavailableDriverId() {
        return unavailableDriverId;
    }

    public void setUnavailableDriverId(Long unavailableDriverId) {
        this.unavailableDriverId = unavailableDriverId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}

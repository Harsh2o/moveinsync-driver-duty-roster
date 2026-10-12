package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DutyStatusUpdateRequest {
    @NotBlank(message = "Status is required")
    private String status;

    public DutyStatusUpdateRequest() {
    }

    public DutyStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public class RosterGenerationRequest {
    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private List<DutyRequest> duties;

    public RosterGenerationRequest() {
    }

    public RosterGenerationRequest(LocalDateTime startTime, LocalDateTime endTime, List<DutyRequest> duties) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duties = duties;
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

    public List<DutyRequest> getDuties() {
        return duties;
    }

    public void setDuties(List<DutyRequest> duties) {
        this.duties = duties;
    }
}

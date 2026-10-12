package com.moveinsync.roster.dto.response;

import java.util.List;

public class RosterGenerationResponse {
    private int totalDuties;
    private int assignedDuties;
    private int unfilledDuties;
    private List<DutyResponse> assignments;
    private List<DutyResponse> unfilled;
    private String generationTimeMs;

    public RosterGenerationResponse() {
    }

    public RosterGenerationResponse(int totalDuties, int assignedDuties, int unfilledDuties, List<DutyResponse> assignments, List<DutyResponse> unfilled, String generationTimeMs) {
        this.totalDuties = totalDuties;
        this.assignedDuties = assignedDuties;
        this.unfilledDuties = unfilledDuties;
        this.assignments = assignments;
        this.unfilled = unfilled;
        this.generationTimeMs = generationTimeMs;
    }

    public int getTotalDuties() {
        return totalDuties;
    }

    public void setTotalDuties(int totalDuties) {
        this.totalDuties = totalDuties;
    }

    public int getAssignedDuties() {
        return assignedDuties;
    }

    public void setAssignedDuties(int assignedDuties) {
        this.assignedDuties = assignedDuties;
    }

    public int getUnfilledDuties() {
        return unfilledDuties;
    }

    public void setUnfilledDuties(int unfilledDuties) {
        this.unfilledDuties = unfilledDuties;
    }

    public List<DutyResponse> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<DutyResponse> assignments) {
        this.assignments = assignments;
    }

    public List<DutyResponse> getUnfilled() {
        return unfilled;
    }

    public void setUnfilled(List<DutyResponse> unfilled) {
        this.unfilled = unfilled;
    }

    public String getGenerationTimeMs() {
        return generationTimeMs;
    }

    public void setGenerationTimeMs(String generationTimeMs) {
        this.generationTimeMs = generationTimeMs;
    }
}

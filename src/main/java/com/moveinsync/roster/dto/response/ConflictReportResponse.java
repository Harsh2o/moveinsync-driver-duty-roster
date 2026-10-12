package com.moveinsync.roster.dto.response;

import java.util.List;
import java.util.Map;

public class ConflictReportResponse {
    private int totalDuties;
    private int assignedDuties;
    private int unfilledDuties;
    private List<UnfilledDutyDetail> unfilledDetails;
    private Map<String, Integer> rejectionCountsByReason;
    private String summary;

    public ConflictReportResponse() {
    }

    public ConflictReportResponse(int totalDuties, int assignedDuties, int unfilledDuties, List<UnfilledDutyDetail> unfilledDetails, Map<String, Integer> rejectionCountsByReason, String summary) {
        this.totalDuties = totalDuties;
        this.assignedDuties = assignedDuties;
        this.unfilledDuties = unfilledDuties;
        this.unfilledDetails = unfilledDetails;
        this.rejectionCountsByReason = rejectionCountsByReason;
        this.summary = summary;
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

    public List<UnfilledDutyDetail> getUnfilledDetails() {
        return unfilledDetails;
    }

    public void setUnfilledDetails(List<UnfilledDutyDetail> unfilledDetails) {
        this.unfilledDetails = unfilledDetails;
    }

    public Map<String, Integer> getRejectionCountsByReason() {
        return rejectionCountsByReason;
    }

    public void setRejectionCountsByReason(Map<String, Integer> rejectionCountsByReason) {
        this.rejectionCountsByReason = rejectionCountsByReason;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public static class UnfilledDutyDetail {
        private Long dutyId;
        private String requiredVehicleType;
        private String startTime;
        private String endTime;
        private String reason;

        public UnfilledDutyDetail() {
        }

        public UnfilledDutyDetail(Long dutyId, String requiredVehicleType, String startTime, String endTime, String reason) {
            this.dutyId = dutyId;
            this.requiredVehicleType = requiredVehicleType;
            this.startTime = startTime;
            this.endTime = endTime;
            this.reason = reason;
        }

        public Long getDutyId() {
            return dutyId;
        }

        public void setDutyId(Long dutyId) {
            this.dutyId = dutyId;
        }

        public String getRequiredVehicleType() {
            return requiredVehicleType;
        }

        public void setRequiredVehicleType(String requiredVehicleType) {
            this.requiredVehicleType = requiredVehicleType;
        }

        public String getStartTime() {
            return startTime;
        }

        public void setStartTime(String startTime) {
            this.startTime = startTime;
        }

        public String getEndTime() {
            return endTime;
        }

        public void setEndTime(String endTime) {
            this.endTime = endTime;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }
}

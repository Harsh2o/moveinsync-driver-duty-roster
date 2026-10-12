package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.response.AuditResponse;
import com.moveinsync.roster.dto.response.ConflictReportResponse;
import com.moveinsync.roster.dto.response.UtilizationResponse;
import com.moveinsync.roster.model.entity.AssignmentAudit;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.repository.AssignmentAuditRepository;
import com.moveinsync.roster.repository.DriverRepository;
import com.moveinsync.roster.repository.DutyRepository;
import com.moveinsync.roster.repository.VehicleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reporting service for conflict summaries, utilization, and audit history.
 * 
 * Utilization baseline: configurable regular-work-hours-per-day (default 8).
 * Capacity = regularHours * number of calendar days in the reporting period.
 * Overtime = max(0, totalScheduledHours - capacity).
 * Cancelled duties are excluded from active scheduled work.
 */
@Service
public class ReportingService {

    private final DutyRepository dutyRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AssignmentAuditRepository auditRepository;
    private final int regularWorkHoursPerDay;

    public ReportingService(DutyRepository dutyRepository,
                            DriverRepository driverRepository,
                            VehicleRepository vehicleRepository,
                            AssignmentAuditRepository auditRepository,
                            @Value("${app.roster.regular-work-hours-per-day:8}") int regularWorkHoursPerDay) {
        this.dutyRepository = dutyRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.auditRepository = auditRepository;
        this.regularWorkHoursPerDay = regularWorkHoursPerDay;
    }

    /**
     * Generate conflict report for a date range.
     * Shows total duties, assigned, unfilled, reasons, and rejection counts.
     */
    @Transactional(readOnly = true)
    public ConflictReportResponse getConflictReport(LocalDateTime startTime, LocalDateTime endTime) {
        List<Duty> allDuties = dutyRepository.findActiveDutiesInRange(startTime, endTime);
        
        List<Duty> assigned = new ArrayList<>();
        List<Duty> unfilled = new ArrayList<>();
        Map<String, Integer> rejectionCounts = new HashMap<>();

        for (Duty duty : allDuties) {
            if (duty.getAssignmentState() == AssignmentState.ASSIGNED) {
                assigned.add(duty);
            } else {
                unfilled.add(duty);
                if (duty.getUnfilledReason() != null) {
                    // Parse rejection reason categories
                    String reason = duty.getUnfilledReason();
                    if (reason.contains("REST_RULE")) rejectionCounts.merge("REST_RULE", 1, Integer::sum);
                    if (reason.contains("DRIVER_OVERLAP")) rejectionCounts.merge("DRIVER_OVERLAP", 1, Integer::sum);
                    if (reason.contains("VEHICLE_CONFLICT")) rejectionCounts.merge("VEHICLE_CONFLICT", 1, Integer::sum);
                    if (reason.contains("LICENCE")) rejectionCounts.merge("LICENCE", 1, Integer::sum);
                    if (reason.contains("MEDICAL")) rejectionCounts.merge("MEDICAL_EXPIRED", 1, Integer::sum);
                    if (reason.contains("TRAINING")) rejectionCounts.merge("TRAINING_EXPIRED", 1, Integer::sum);
                    if (reason.contains("INACTIVE")) rejectionCounts.merge("INACTIVE", 1, Integer::sum);
                    if (reason.contains("No active vehicles")) rejectionCounts.merge("NO_VEHICLES", 1, Integer::sum);
                    if (reason.contains("No eligible")) rejectionCounts.merge("NO_ELIGIBLE_COMBO", 1, Integer::sum);
                }
            }
        }

        ConflictReportResponse response = new ConflictReportResponse();
        response.setTotalDuties(allDuties.size());
        response.setAssignedDuties(assigned.size());
        response.setUnfilledDuties(unfilled.size());
        response.setRejectionCountsByReason(rejectionCounts);

        List<ConflictReportResponse.UnfilledDutyDetail> details = unfilled.stream()
                .map(d -> {
                    ConflictReportResponse.UnfilledDutyDetail detail = new ConflictReportResponse.UnfilledDutyDetail();
                    detail.setDutyId(d.getId());
                    detail.setRequiredVehicleType(d.getRequiredVehicleType().name());
                    detail.setStartTime(d.getStartTime().toString());
                    detail.setEndTime(d.getEndTime().toString());
                    detail.setReason(d.getUnfilledReason() != null ? d.getUnfilledReason() : "Unknown");
                    return detail;
                }).toList();
        response.setUnfilledDetails(details);

        response.setSummary(String.format("%d of %d duties assigned. %d unfilled.",
                assigned.size(), allDuties.size(), unfilled.size()));

        return response;
    }

    /**
     * Generate utilization report for drivers and vehicles in a date range.
     * 
     * Capacity baseline: regularWorkHoursPerDay * calendar days in period.
     * Overtime = max(0, totalScheduledHours - capacity).
     * Utilization = (totalScheduledHours / capacity) * 100.
     */
    @Transactional(readOnly = true)
    public UtilizationResponse getUtilizationReport(LocalDateTime startTime, LocalDateTime endTime) {
        long calendarDays = Duration.between(startTime.toLocalDate().atStartOfDay(),
                endTime.toLocalDate().atStartOfDay()).toDays();
        if (calendarDays < 1) calendarDays = 1;
        double capacityHours = (double) regularWorkHoursPerDay * calendarDays;

        // Driver utilization
        List<Driver> allDrivers = driverRepository.findAll();
        List<UtilizationResponse.DriverUtilization> driverUtils = new ArrayList<>();

        for (Driver driver : allDrivers) {
            List<Duty> duties = dutyRepository.findAssignedDutiesForDriverInRange(
                    driver.getId(), startTime, endTime);

            double totalHours = duties.stream()
                    .mapToDouble(d -> Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0)
                    .sum();

            double utilization = capacityHours > 0 ? (totalHours / capacityHours) * 100.0 : 0;
            double overtime = Math.max(0, totalHours - capacityHours);

            UtilizationResponse.DriverUtilization du = new UtilizationResponse.DriverUtilization();
            du.setDriverId(driver.getId());
            du.setDriverName(driver.getName());
            du.setLicenceClass(driver.getLicenceClass().name());
            du.setAssignedDuties(duties.size());
            du.setTotalScheduledHours(Math.round(totalHours * 100.0) / 100.0);
            du.setUtilizationPercentage(Math.round(utilization * 100.0) / 100.0);
            du.setOvertimeHours(Math.round(overtime * 100.0) / 100.0);
            du.setAvailable(driver.isActive());
            driverUtils.add(du);
        }

        // Vehicle utilization
        List<Vehicle> allVehicles = vehicleRepository.findAll();
        List<UtilizationResponse.VehicleUtilization> vehicleUtils = new ArrayList<>();

        for (Vehicle vehicle : allVehicles) {
            List<Duty> duties = dutyRepository.findAssignedDutiesForVehicleInRange(
                    vehicle.getId(), startTime, endTime);

            double totalHours = duties.stream()
                    .mapToDouble(d -> Duration.between(d.getStartTime(), d.getEndTime()).toMinutes() / 60.0)
                    .sum();

            double utilization = capacityHours > 0 ? (totalHours / capacityHours) * 100.0 : 0;

            UtilizationResponse.VehicleUtilization vu = new UtilizationResponse.VehicleUtilization();
            vu.setVehicleId(vehicle.getId());
            vu.setRegistrationNumber(vehicle.getRegistrationNumber());
            vu.setVehicleType(vehicle.getVehicleType().name());
            vu.setAssignedDuties(duties.size());
            vu.setTotalScheduledHours(Math.round(totalHours * 100.0) / 100.0);
            vu.setUtilizationPercentage(Math.round(utilization * 100.0) / 100.0);
            vu.setAvailable(vehicle.isActive());
            vehicleUtils.add(vu);
        }

        UtilizationResponse response = new UtilizationResponse();
        response.setDriverUtilizations(driverUtils);
        response.setVehicleUtilizations(vehicleUtils);
        return response;
    }

    /**
     * Get audit history for a specific duty.
     */
    @Transactional(readOnly = true)
    public List<AuditResponse> getDutyAuditHistory(Long dutyId) {
        List<AssignmentAudit> audits = auditRepository.findByDutyIdOrderByCreatedAtDesc(dutyId);
        return audits.stream().map(this::toAuditResponse).toList();
    }

    private AuditResponse toAuditResponse(AssignmentAudit audit) {
        AuditResponse response = new AuditResponse();
        response.setId(audit.getId());
        response.setDutyId(audit.getDuty().getId());
        response.setPreviousDriverId(audit.getPreviousDriverId());
        response.setPreviousVehicleId(audit.getPreviousVehicleId());
        response.setNewDriverId(audit.getNewDriverId());
        response.setNewVehicleId(audit.getNewVehicleId());
        response.setAction(audit.getAction().name());
        response.setReason(audit.getReason());
        response.setActingUser(audit.getActingUser());
        response.setCreatedAt(audit.getCreatedAt());
        return response;
    }
}

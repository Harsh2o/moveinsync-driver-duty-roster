package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.response.ConflictReportResponse;
import com.moveinsync.roster.dto.response.UtilizationResponse;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.AssignmentAuditRepository;
import com.moveinsync.roster.repository.DriverRepository;
import com.moveinsync.roster.repository.DutyRepository;
import com.moveinsync.roster.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportingServiceTest {

    @Mock
    private DutyRepository dutyRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AssignmentAuditRepository auditRepository;

    private ReportingService reportingService;

    @BeforeEach
    void setUp() {
        // 8 hours per day regular baseline
        reportingService = new ReportingService(dutyRepository, driverRepository, vehicleRepository, auditRepository, 8);
    }

    @Test
    @DisplayName("Scenario 16: Unfilled duty is returned in conflict report with exact cause and rejection summary")
    void testConflictReportReturnsUnfilledDutiesWithCauses() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 23, 59);

        Duty assignedDuty = new Duty();
        assignedDuty.setId(1L);
        assignedDuty.setRequiredVehicleType(VehicleType.BUS);
        assignedDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        assignedDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        assignedDuty.setDutyStatus(DutyStatus.ACTIVE);
        assignedDuty.setAssignmentState(AssignmentState.ASSIGNED);

        Duty unfilledDuty = new Duty();
        unfilledDuty.setId(2L);
        unfilledDuty.setRequiredVehicleType(VehicleType.BUS);
        unfilledDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 10, 30));
        unfilledDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 14, 30));
        unfilledDuty.setDutyStatus(DutyStatus.ACTIVE);
        unfilledDuty.setAssignmentState(AssignmentState.UNASSIGNED);
        unfilledDuty.setUnfilledReason("No eligible assignment: REST_RULE=2, LICENCE=1");

        when(dutyRepository.findActiveDutiesInRange(start, end))
                .thenReturn(List.of(assignedDuty, unfilledDuty));

        ConflictReportResponse report = reportingService.getConflictReport(start, end);

        assertThat(report.getTotalDuties()).isEqualTo(2);
        assertThat(report.getAssignedDuties()).isEqualTo(1);
        assertThat(report.getUnfilledDuties()).isEqualTo(1);
        assertThat(report.getUnfilledDetails()).hasSize(1);
        assertThat(report.getUnfilledDetails().get(0).getDutyId()).isEqualTo(2L);
        assertThat(report.getUnfilledDetails().get(0).getReason()).contains("REST_RULE");
        assertThat(report.getRejectionCountsByReason()).containsEntry("REST_RULE", 1);
        assertThat(report.getRejectionCountsByReason()).containsEntry("LICENCE", 1);
    }

    @Test
    @DisplayName("Scenario 17: Utilization and overtime calculations are correct based on 8h baseline")
    void testUtilizationAndOvertimeCalculation() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 23, 59);

        Driver driver = new Driver();
        driver.setId(5L);
        driver.setName("Overtime Driver");
        driver.setLicenceClass(LicenceClass.HMV);
        driver.setActive(true);

        when(driverRepository.findAll()).thenReturn(List.of(driver));
        when(vehicleRepository.findAll()).thenReturn(Collections.emptyList());

        // Driver worked 2 duties: 06:00-11:00 (5 hrs) and 19:00-24:00 (5 hrs) = 10 hrs total
        // Baseline capacity: 8 hours (1 calendar day * 8h)
        // Utilization: (10 / 8) * 100 = 125%
        // Overtime: 10 - 8 = 2.0 hours
        Duty duty1 = new Duty();
        duty1.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        duty1.setEndTime(LocalDateTime.of(2026, 10, 15, 11, 0));

        Duty duty2 = new Duty();
        duty2.setStartTime(LocalDateTime.of(2026, 10, 15, 19, 0));
        duty2.setEndTime(LocalDateTime.of(2026, 10, 16, 0, 0));

        when(dutyRepository.findAssignedDutiesForDriverInRange(eq(5L), eq(start), eq(end)))
                .thenReturn(List.of(duty1, duty2));

        UtilizationResponse response = reportingService.getUtilizationReport(start, end);

        assertThat(response.getDriverUtilizations()).hasSize(1);
        UtilizationResponse.DriverUtilization du = response.getDriverUtilizations().get(0);
        assertThat(du.getDriverId()).isEqualTo(5L);
        assertThat(du.getAssignedDuties()).isEqualTo(2);
        assertThat(du.getTotalScheduledHours()).isEqualTo(10.0);
        assertThat(du.getUtilizationPercentage()).isEqualTo(125.0);
        assertThat(du.getOvertimeHours()).isEqualTo(2.0);
    }
}

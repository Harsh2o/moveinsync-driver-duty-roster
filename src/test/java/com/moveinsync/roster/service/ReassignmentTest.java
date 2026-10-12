package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.dto.request.ReassignmentRequest;
import com.moveinsync.roster.dto.response.ReassignmentResponse;
import com.moveinsync.roster.model.entity.AssignmentAudit;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.AuditAction;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.AssignmentAuditRepository;
import com.moveinsync.roster.repository.DriverRepository;
import com.moveinsync.roster.repository.DutyRepository;
import com.moveinsync.roster.repository.VehicleRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReassignmentTest {

    @Mock
    private DutyRepository dutyRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private AssignmentAuditRepository auditRepository;
    @Mock
    private EligibilityService eligibilityService;

    private VehicleLicencePolicy licencePolicy;
    private Clock fixedClock;
    private SimpleMeterRegistry meterRegistry;
    private RosterService rosterService;

    @BeforeEach
    void setUp() {
        licencePolicy = new VehicleLicencePolicy();
        fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 10, 12, 0, 0, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant(),
                ZoneId.of("Asia/Kolkata"));
        meterRegistry = new SimpleMeterRegistry();
        rosterService = new RosterService(
                dutyRepository, driverRepository, vehicleRepository, auditRepository,
                eligibilityService, licencePolicy, fixedClock, 8, meterRegistry);
    }

    private Driver createDriver(Long id, String name, LicenceClass licenceClass) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName(name);
        driver.setPhoneNumber("9999999999");
        driver.setLicenceClass(licenceClass);
        driver.setLicenceExpiryDate(LocalDate.of(2027, 1, 1));
        driver.setMedicalValidityDate(LocalDate.of(2027, 1, 1));
        driver.setTrainingValidityDate(LocalDate.of(2027, 1, 1));
        driver.setActive(true);
        return driver;
    }

    private Vehicle createVehicle(Long id, String reg, VehicleType type, LicenceClass requiredClass) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber(reg);
        vehicle.setVehicleType(type);
        vehicle.setRequiredLicenceClass(requiredClass);
        vehicle.setActive(true);
        return vehicle;
    }

    private Duty createDuty(Long id, VehicleType type, Driver driver, Vehicle vehicle) {
        Duty duty = new Duty();
        duty.setId(id);
        duty.setRequiredVehicleType(type);
        duty.setStartTime(LocalDateTime.of(2026, 10, 15, 8, 0));
        duty.setEndTime(LocalDateTime.of(2026, 10, 15, 12, 0));
        duty.setDutyStatus(DutyStatus.ACTIVE);
        duty.setAssignmentState(AssignmentState.ASSIGNED);
        duty.setAssignedDriver(driver);
        duty.setAssignedVehicle(vehicle);
        return duty;
    }

    @Test
    @DisplayName("Scenario 14 & 15: Unavailable driver excluded from reassignment; successful reassignment retains old and new details in audit trail")
    void testReassignmentExcludesUnavailableDriverAndAudits() {
        Driver originalDriver = createDriver(1L, "Sick Driver", LicenceClass.HMV);
        Driver replacementDriver = createDriver(2L, "Healthy Replacement", LicenceClass.HMV);
        Vehicle bus = createVehicle(10L, "BUS-01", VehicleType.BUS, LicenceClass.HMV);

        Duty duty = createDuty(100L, VehicleType.BUS, originalDriver, bus);

        when(dutyRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(duty));
        when(driverRepository.findByActiveTrue()).thenReturn(List.of(originalDriver, replacementDriver));
        when(vehicleRepository.findByActiveTrueAndVehicleType(VehicleType.BUS)).thenReturn(List.of(bus));

        // Replacement driver is eligible
        when(eligibilityService.checkEligibility(eq(replacementDriver), eq(bus), eq(duty.getStartTime()), eq(duty.getEndTime()), eq(VehicleType.BUS), eq(100L)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.save(any(Duty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReassignmentRequest request = new ReassignmentRequest();
        request.setDutyId(100L);
        request.setUnavailableDriverId(1L);
        request.setReason("Driver reported sick with fever");

        ReassignmentResponse response = rosterService.reassignDuty(request);

        assertThat(response.isReassigned()).isTrue();
        assertThat(response.getPreviousDriverId()).isEqualTo(1L);
        assertThat(response.getNewDriverId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo("REASSIGNED");

        // Verify audit record was created with old and new driver
        ArgumentCaptor<AssignmentAudit> auditCaptor = ArgumentCaptor.forClass(AssignmentAudit.class);
        verify(auditRepository).save(auditCaptor.capture());
        AssignmentAudit savedAudit = auditCaptor.getValue();

        assertThat(savedAudit.getPreviousDriverId()).isEqualTo(1L);
        assertThat(savedAudit.getNewDriverId()).isEqualTo(2L);
        assertThat(savedAudit.getAction()).isEqualTo(AuditAction.REASSIGNMENT);
        assertThat(savedAudit.getReason()).isEqualTo("Driver reported sick with fever");
    }

    @Test
    @DisplayName("Scenario 21: Reassignment failure leaves duty unassigned without partial state and creates audit trail")
    void testReassignmentFailureLeavesDutyUnassigned() {
        Driver originalDriver = createDriver(1L, "Sick Driver", LicenceClass.HMV);
        Vehicle bus = createVehicle(10L, "BUS-01", VehicleType.BUS, LicenceClass.HMV);

        Duty duty = createDuty(100L, VehicleType.BUS, originalDriver, bus);

        when(dutyRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(duty));
        // Only the sick driver is in the repository
        when(driverRepository.findByActiveTrue()).thenReturn(List.of(originalDriver));
        when(vehicleRepository.findByActiveTrueAndVehicleType(VehicleType.BUS)).thenReturn(List.of(bus));
        when(dutyRepository.save(any(Duty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReassignmentRequest request = new ReassignmentRequest();
        request.setDutyId(100L);
        request.setUnavailableDriverId(1L);
        request.setReason("Driver emergency");

        ReassignmentResponse response = rosterService.reassignDuty(request);

        assertThat(response.isReassigned()).isFalse();
        assertThat(response.getStatus()).isEqualTo("UNASSIGNED");
        assertThat(duty.getAssignmentState()).isEqualTo(AssignmentState.UNASSIGNED);
        assertThat(duty.getAssignedDriver()).isNull();
        assertThat(duty.getUnfilledReason()).contains("Reassignment failed");

        verify(auditRepository).save(any(AssignmentAudit.class));
    }
}

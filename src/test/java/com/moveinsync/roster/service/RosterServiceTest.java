package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.dto.response.RosterGenerationResponse;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RosterServiceTest {

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

    private Duty createDuty(Long id, VehicleType type, LocalDateTime start, LocalDateTime end) {
        Duty duty = new Duty();
        duty.setId(id);
        duty.setRequiredVehicleType(type);
        duty.setStartTime(start);
        duty.setEndTime(end);
        duty.setDutyStatus(DutyStatus.ACTIVE);
        duty.setAssignmentState(AssignmentState.UNASSIGNED);
        return duty;
    }

    @Test
    @DisplayName("Scarcity-first greedy assignment: scarce duties assigned first and successful")
    void testScarcityFirstAssignment() {
        LocalDateTime startRange = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime endRange = LocalDateTime.of(2026, 10, 15, 22, 0);

        Duty busDuty = createDuty(1L, VehicleType.BUS,
                LocalDateTime.of(2026, 10, 15, 6, 0), LocalDateTime.of(2026, 10, 15, 10, 0));
        Duty cabDuty = createDuty(2L, VehicleType.CAB,
                LocalDateTime.of(2026, 10, 15, 6, 0), LocalDateTime.of(2026, 10, 15, 10, 0));

        List<Duty> duties = new ArrayList<>(List.of(busDuty, cabDuty));
        when(dutyRepository.findUnassignedActiveDuties(startRange, endRange)).thenReturn(duties);

        Driver hmvDriver = createDriver(10L, "HMV Driver", LicenceClass.HMV);
        Driver lmvDriver = createDriver(20L, "LMV Driver", LicenceClass.LMV);
        when(driverRepository.findByActiveTrue()).thenReturn(List.of(hmvDriver, lmvDriver));

        Vehicle bus = createVehicle(100L, "BUS-01", VehicleType.BUS, LicenceClass.HMV);
        Vehicle cab = createVehicle(200L, "CAB-01", VehicleType.CAB, LicenceClass.LMV);
        when(vehicleRepository.findByActiveTrue()).thenReturn(List.of(bus, cab));

        // Eligibility simulation:
        // Bus duty: Only HMV driver is eligible (1 combination) -> SCARCE
        // Cab duty: Both HMV and LMV drivers are eligible (2 combinations) -> ABUNDANT
        when(eligibilityService.checkEligibility(eq(hmvDriver), eq(bus), eq(busDuty.getStartTime()), eq(busDuty.getEndTime()), eq(VehicleType.BUS), isNull()))
                .thenReturn(Collections.emptyList());
        when(eligibilityService.checkEligibility(eq(lmvDriver), eq(bus), eq(busDuty.getStartTime()), eq(busDuty.getEndTime()), eq(VehicleType.BUS), isNull()))
                .thenReturn(List.of("LMV cannot drive BUS"));

        when(eligibilityService.checkEligibility(eq(lmvDriver), eq(cab), eq(cabDuty.getStartTime()), eq(cabDuty.getEndTime()), eq(VehicleType.CAB), isNull()))
                .thenReturn(Collections.emptyList());
        when(eligibilityService.checkEligibility(eq(hmvDriver), eq(cab), eq(cabDuty.getStartTime()), eq(cabDuty.getEndTime()), eq(VehicleType.CAB), isNull()))
                .thenReturn(Collections.emptyList(), List.of("Driver 10 has 1 overlapping duty(ies)"));

        when(dutyRepository.save(any(Duty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RosterGenerationResponse response = rosterService.generateRoster(startRange, endRange);

        assertThat(response.getTotalDuties()).isEqualTo(2);
        assertThat(response.getAssignedDuties()).isEqualTo(2);
        assertThat(response.getUnfilledDuties()).isEqualTo(0);
        assertThat(busDuty.getAssignmentState()).isEqualTo(AssignmentState.ASSIGNED);
        assertThat(busDuty.getAssignedDriver()).isEqualTo(hmvDriver);
        assertThat(cabDuty.getAssignmentState()).isEqualTo(AssignmentState.ASSIGNED);
        assertThat(cabDuty.getAssignedDriver()).isEqualTo(lmvDriver);
    }

    @Test
    @DisplayName("Unfilled duty retained with clear rejection reason when no vehicle is available")
    void testUnfilledDutyWhenNoVehicleAvailable() {
        LocalDateTime startRange = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime endRange = LocalDateTime.of(2026, 10, 15, 22, 0);

        Duty busDuty = createDuty(1L, VehicleType.BUS,
                LocalDateTime.of(2026, 10, 15, 6, 0), LocalDateTime.of(2026, 10, 15, 10, 0));

        when(dutyRepository.findUnassignedActiveDuties(startRange, endRange)).thenReturn(new ArrayList<>(List.of(busDuty)));
        when(driverRepository.findByActiveTrue()).thenReturn(List.of(createDriver(1L, "HMV Driver", LicenceClass.HMV)));
        // No vehicles available
        when(vehicleRepository.findByActiveTrue()).thenReturn(Collections.emptyList());
        when(dutyRepository.save(any(Duty.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RosterGenerationResponse response = rosterService.generateRoster(startRange, endRange);

        assertThat(response.getTotalDuties()).isEqualTo(1);
        assertThat(response.getAssignedDuties()).isEqualTo(0);
        assertThat(response.getUnfilledDuties()).isEqualTo(1);
        assertThat(busDuty.getAssignmentState()).isEqualTo(AssignmentState.UNASSIGNED);
        assertThat(busDuty.getUnfilledReason()).contains("No active vehicles of type BUS available");
    }
}

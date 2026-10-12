package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.DutyRepository;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EligibilityServiceTest {

    @Mock
    private DutyRepository dutyRepository;

    private VehicleLicencePolicy licencePolicy;
    private Clock fixedClock;
    private EligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        licencePolicy = new VehicleLicencePolicy();
        // Fixed clock for deterministic testing: 2026-10-12T00:00:00Z
        fixedClock = Clock.fixed(
                LocalDateTime.of(2026, 10, 12, 0, 0, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant(),
                ZoneId.of("Asia/Kolkata"));
        eligibilityService = new EligibilityService(dutyRepository, licencePolicy, fixedClock, 8);
    }

    private Driver createDriver(Long id, LicenceClass licenceClass, LocalDate licenceExpiry,
                                LocalDate medicalExpiry, LocalDate trainingExpiry, boolean active) {
        Driver driver = new Driver();
        driver.setId(id);
        driver.setName("Driver " + id);
        driver.setPhoneNumber("9999999999");
        driver.setLicenceClass(licenceClass);
        driver.setLicenceExpiryDate(licenceExpiry);
        driver.setMedicalValidityDate(medicalExpiry);
        driver.setTrainingValidityDate(trainingExpiry);
        driver.setActive(active);
        return driver;
    }

    private Vehicle createVehicle(Long id, VehicleType type, LicenceClass requiredClass, boolean active) {
        Vehicle vehicle = new Vehicle();
        vehicle.setId(id);
        vehicle.setRegistrationNumber("KA01AB" + (1000 + id));
        vehicle.setVehicleType(type);
        vehicle.setRequiredLicenceClass(requiredClass);
        vehicle.setActive(active);
        return vehicle;
    }

    @Test
    @DisplayName("Scenario 1: A valid HMV driver receives a BUS duty")
    void validHmvDriverAcceptedForBusDuty() {
        Driver driver = createDriver(1L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);

        when(dutyRepository.findOverlappingDutiesForDriver(eq(1L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(10L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(1L), eq(start)))
                .thenReturn(Optional.empty());
        when(dutyRepository.findEarliestDutyStartingAfter(eq(1L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).isEmpty();
    }

    @Test
    @DisplayName("Scenario 2: An LMV driver is rejected for a BUS duty")
    void lmvDriverRejectedForBusDuty() {
        Driver driver = createDriver(2L, LicenceClass.LMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).isNotEmpty();
        assertThat(rejections).anyMatch(r -> r.contains("LMV") && r.contains("BUS"));
    }

    @Test
    @DisplayName("Scenario 3: An expired licence prevents assignment")
    void expiredLicencePreventsAssignment() {
        Driver driver = createDriver(3L, LicenceClass.HMV,
                LocalDate.of(2026, 10, 14), // Expired before duty on Oct 15
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("licence expires"));
    }

    @Test
    @DisplayName("Scenario 4: Expired medical clearance prevents assignment")
    void expiredMedicalClearancePreventsAssignment() {
        Driver driver = createDriver(4L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 10, 14), // Expired medical
                LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("medical clearance expires"));
    }

    @Test
    @DisplayName("Scenario 5: Expired training prevents assignment")
    void expiredTrainingPreventsAssignment() {
        Driver driver = createDriver(5L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31),
                LocalDate.of(2026, 10, 14), // Expired training
                true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 6, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 10, 0);

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("training expires"));
    }

    @Test
    @DisplayName("Scenario 6: Credentials that expire before cross-midnight duty ends prevent assignment")
    void credentialsExpiringBeforeCrossMidnightDutyEndsPreventAssignment() {
        // Duty starts on Oct 15 at 22:00 and ends on Oct 16 at 04:00
        // Driver's licence is valid on Oct 15 but expires Oct 15 (so NOT valid on Oct 16)
        Driver driver = createDriver(6L, LicenceClass.HMV,
                LocalDate.of(2026, 10, 15), // Valid on Oct 15, but duty ends Oct 16
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 22, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 16, 4, 0);

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("licence expires") && r.contains("2026-10-16"));
    }

    @Test
    @DisplayName("Scenario 7: Exactly eight hours of rest is accepted")
    void exactlyEightHoursOfRestIsAccepted() {
        Driver driver = createDriver(7L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        // Previous duty ends at 10:00. New duty starts at exactly 18:00 (exactly 8h gap)
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 22, 0);

        Duty prevDuty = new Duty();
        prevDuty.setId(100L);
        prevDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        prevDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        prevDuty.setDutyStatus(DutyStatus.COMPLETED);

        when(dutyRepository.findOverlappingDutiesForDriver(eq(7L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(10L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(7L), eq(start)))
                .thenReturn(Optional.of(prevDuty));
        when(dutyRepository.findEarliestDutyStartingAfter(eq(7L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).isEmpty();
    }

    @Test
    @DisplayName("Scenario 8: Less than eight hours of rest is rejected")
    void lessThanEightHoursOfRestIsRejected() {
        Driver driver = createDriver(8L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        // Previous duty ends at 10:00. New duty starts at 17:59 (7h 59m gap - less than 8h!)
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 17, 59);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 22, 0);

        Duty prevDuty = new Duty();
        prevDuty.setId(101L);
        prevDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        prevDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        prevDuty.setDutyStatus(DutyStatus.COMPLETED);

        when(dutyRepository.findOverlappingDutiesForDriver(eq(8L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(10L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(8L), eq(start)))
                .thenReturn(Optional.of(prevDuty));
        when(dutyRepository.findEarliestDutyStartingAfter(eq(8L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("Insufficient rest") && r.contains("7h 59m"));
    }

    @Test
    @DisplayName("Scenario 9: Overlapping duties for a driver are rejected")
    void overlappingDutiesForDriverAreRejected() {
        Driver driver = createDriver(9L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 12, 0);

        Duty existingDuty = new Duty();
        existingDuty.setId(200L);
        existingDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        existingDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 14, 0));

        when(dutyRepository.findOverlappingDutiesForDriver(eq(9L), eq(start), eq(end)))
                .thenReturn(List.of(existingDuty));
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(10L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("overlapping duty(ies)"));
    }

    @Test
    @DisplayName("Scenario 10: Overlapping duties for a vehicle are rejected")
    void overlappingDutiesForVehicleAreRejected() {
        Driver driver = createDriver(10L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(10L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 12, 0);

        Duty vehicleExistingDuty = new Duty();
        vehicleExistingDuty.setId(201L);
        vehicleExistingDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 11, 0));
        vehicleExistingDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 15, 0));

        when(dutyRepository.findOverlappingDutiesForDriver(eq(10L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(10L), eq(start), eq(end)))
                .thenReturn(List.of(vehicleExistingDuty));
        when(dutyRepository.findLatestDutyEndingBefore(eq(10L), eq(start)))
                .thenReturn(Optional.empty());
        when(dutyRepository.findEarliestDutyStartingAfter(eq(10L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("Vehicle") && r.contains("overlapping duty(ies)"));
    }

    @Test
    @DisplayName("Scenario 11: Back-to-back duties work when rest requirement is satisfied")
    void backToBackDutiesWorkWhenRestRequirementIsSatisfied() {
        Driver driver = createDriver(11L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(11L, VehicleType.BUS, LicenceClass.HMV, true);

        // Duty 1: Oct 15, 06:00 - 10:00
        // Duty 2: Oct 15, 19:00 - 23:00 (gap: 9 hours > 8 hours)
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 19, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 23, 0);

        Duty prevDuty = new Duty();
        prevDuty.setId(301L);
        prevDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        prevDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));

        when(dutyRepository.findOverlappingDutiesForDriver(eq(11L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(11L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(11L), eq(start)))
                .thenReturn(Optional.of(prevDuty));
        when(dutyRepository.findEarliestDutyStartingAfter(eq(11L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).isEmpty();
    }

    @Test
    @DisplayName("Scenario 12: Cancelled duties no longer block an otherwise valid assignment")
    void cancelledDutiesDoNotBlockAssignment() {
        Driver driver = createDriver(12L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(12L, VehicleType.BUS, LicenceClass.HMV, true);

        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 12, 0);

        // Cancelled duties are filtered out by repository query (dutyStatus <> 'CANCELLED')
        when(dutyRepository.findOverlappingDutiesForDriver(eq(12L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(12L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(12L), eq(start)))
                .thenReturn(Optional.empty());
        when(dutyRepository.findEarliestDutyStartingAfter(eq(12L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).isEmpty();
    }

    @Test
    @DisplayName("Scenario 13: A completed duty still affects the rest calculation where applicable")
    void completedDutyAffectsRestCalculation() {
        Driver driver = createDriver(13L, LicenceClass.HMV,
                LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), LocalDate.of(2026, 12, 31), true);
        Vehicle bus = createVehicle(13L, VehicleType.BUS, LicenceClass.HMV, true);

        // Completed duty ended 4 hours ago
        LocalDateTime start = LocalDateTime.of(2026, 10, 15, 14, 0);
        LocalDateTime end = LocalDateTime.of(2026, 10, 15, 18, 0);

        Duty completedDuty = new Duty();
        completedDuty.setId(401L);
        completedDuty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        completedDuty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        completedDuty.setDutyStatus(DutyStatus.COMPLETED);

        when(dutyRepository.findOverlappingDutiesForDriver(eq(13L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findOverlappingDutiesForVehicle(eq(13L), eq(start), eq(end)))
                .thenReturn(Collections.emptyList());
        when(dutyRepository.findLatestDutyEndingBefore(eq(13L), eq(start)))
                .thenReturn(Optional.of(completedDuty));
        when(dutyRepository.findEarliestDutyStartingAfter(eq(13L), eq(end)))
                .thenReturn(Optional.empty());

        List<String> rejections = eligibilityService.checkEligibility(driver, bus, start, end, VehicleType.BUS, null);

        assertThat(rejections).anyMatch(r -> r.contains("Insufficient rest") && r.contains("4h 0m"));
    }
}

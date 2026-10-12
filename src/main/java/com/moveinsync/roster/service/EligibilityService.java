package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.DutyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Centralized eligibility checker for driver-vehicle-duty combinations.
 * 
 * Validates all mandatory assignment rules:
 * 1. Driver is active and available
 * 2. Licence class satisfies vehicle-type policy
 * 3. Driving licence valid through duty end date
 * 4. Medical clearance valid through duty end date
 * 5. Training valid through duty end date
 * 6. No conflicting assignments (half-open interval overlap)
 * 7. Minimum 8-hour rest between consecutive duties
 * 8. Vehicle is active and suitable
 * 9. No vehicle overlap
 */
@Service
public class EligibilityService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityService.class);

    private final DutyRepository dutyRepository;
    private final VehicleLicencePolicy licencePolicy;
    private final Clock clock;
    private final int requiredRestHours;

    public EligibilityService(DutyRepository dutyRepository,
                              VehicleLicencePolicy licencePolicy,
                              Clock clock,
                              @Value("${app.roster.rest-hours:8}") int requiredRestHours) {
        this.dutyRepository = dutyRepository;
        this.licencePolicy = licencePolicy;
        this.clock = clock;
        this.requiredRestHours = requiredRestHours;
    }

    /**
     * Check all eligibility rules for a driver-vehicle-duty combination.
     * Returns a list of rejection reasons. Empty list means eligible.
     */
    public List<String> checkEligibility(Driver driver, Vehicle vehicle, 
                                          LocalDateTime dutyStart, LocalDateTime dutyEnd,
                                          VehicleType requiredVehicleType,
                                          Long excludeDutyId) {
        List<String> reasons = new ArrayList<>();

        // 1. Driver active
        if (!driver.isActive()) {
            reasons.add("Driver " + driver.getId() + " is not active");
            return reasons; // No point checking further
        }

        // 2. Licence class check
        if (!licencePolicy.isLicenceSufficient(driver.getLicenceClass(), requiredVehicleType)) {
            reasons.add("Driver " + driver.getId() + " has " + driver.getLicenceClass() 
                    + " licence, but " + requiredVehicleType + " requires " 
                    + licencePolicy.getRequiredLicenceClass(requiredVehicleType));
        }

        // 3. Licence validity - check against duty end date
        // An expiry date is valid through that calendar date
        LocalDate dutyEndDate = dutyEnd.toLocalDate();
        if (driver.getLicenceExpiryDate().isBefore(dutyEndDate)) {
            reasons.add("Driver " + driver.getId() + " licence expires on " 
                    + driver.getLicenceExpiryDate() + " before duty ends on " + dutyEndDate);
        }

        // 4. Medical validity
        if (driver.getMedicalValidityDate().isBefore(dutyEndDate)) {
            reasons.add("Driver " + driver.getId() + " medical clearance expires on " 
                    + driver.getMedicalValidityDate() + " before duty ends on " + dutyEndDate);
        }

        // 5. Training validity
        if (driver.getTrainingValidityDate().isBefore(dutyEndDate)) {
            reasons.add("Driver " + driver.getId() + " training expires on " 
                    + driver.getTrainingValidityDate() + " before duty ends on " + dutyEndDate);
        }

        // 6. Driver overlap check (half-open interval: [start, end))
        // newStart < existingEnd && existingStart < newEnd
        List<Duty> overlapping = dutyRepository.findOverlappingDutiesForDriver(
                driver.getId(), dutyStart, dutyEnd);
        // Exclude the current duty if we're checking for reassignment
        if (excludeDutyId != null) {
            overlapping = overlapping.stream()
                    .filter(d -> !d.getId().equals(excludeDutyId))
                    .toList();
        }
        if (!overlapping.isEmpty()) {
            reasons.add("Driver " + driver.getId() + " has " + overlapping.size() 
                    + " overlapping duty(ies)");
        }

        // 7. Rest-hour check (both directions)
        if (reasons.isEmpty() || !reasons.stream().anyMatch(r -> r.contains("overlapping"))) {
            checkRestHours(driver.getId(), dutyStart, dutyEnd, excludeDutyId, reasons);
        }

        // 8. Vehicle checks
        if (vehicle != null) {
            if (!vehicle.isActive()) {
                reasons.add("Vehicle " + vehicle.getId() + " is not active");
            }
            if (vehicle.getVehicleType() != requiredVehicleType) {
                reasons.add("Vehicle " + vehicle.getId() + " is type " + vehicle.getVehicleType() 
                        + " but duty requires " + requiredVehicleType);
            }
            // Check driver licence against vehicle's required licence class
            if (!licencePolicy.isLicenceSufficient(driver.getLicenceClass(), vehicle.getRequiredLicenceClass())) {
                reasons.add("Driver " + driver.getId() + " licence " + driver.getLicenceClass()
                        + " insufficient for vehicle " + vehicle.getId() 
                        + " requiring " + vehicle.getRequiredLicenceClass());
            }

            // 9. Vehicle overlap check
            List<Duty> vehicleOverlap = dutyRepository.findOverlappingDutiesForVehicle(
                    vehicle.getId(), dutyStart, dutyEnd);
            if (excludeDutyId != null) {
                vehicleOverlap = vehicleOverlap.stream()
                        .filter(d -> !d.getId().equals(excludeDutyId))
                        .toList();
            }
            if (!vehicleOverlap.isEmpty()) {
                reasons.add("Vehicle " + vehicle.getId() + " has " + vehicleOverlap.size() 
                        + " overlapping duty(ies)");
            }
        }

        return reasons;
    }

    /**
     * Check rest-hour requirement in both directions:
     * - Gap after the driver's previous duty ending before this duty starts
     * - Gap before the driver's next duty starting after this duty ends
     * 
     * A driver must have at least 8 full hours of rest between consecutive duties.
     * Uses Duration calculations, not string comparisons.
     */
    private void checkRestHours(Long driverId, LocalDateTime dutyStart, LocalDateTime dutyEnd,
                                 Long excludeDutyId, List<String> reasons) {
        Duration requiredRest = Duration.ofHours(requiredRestHours);

        // Check previous duty (gap before this duty)
        Optional<Duty> previousDuty = dutyRepository.findLatestDutyEndingBefore(driverId, dutyStart);
        if (previousDuty.isPresent()) {
            Duty prev = previousDuty.get();
            if (excludeDutyId == null || !prev.getId().equals(excludeDutyId)) {
                Duration gap = Duration.between(prev.getEndTime(), dutyStart);
                if (gap.compareTo(requiredRest) < 0) {
                    reasons.add("Insufficient rest: only " + formatDuration(gap) 
                            + " between previous duty (ends " + prev.getEndTime() 
                            + ") and new duty (starts " + dutyStart + "). Required: " 
                            + requiredRestHours + " hours");
                }
            }
        }

        // Check next duty (gap after this duty)
        Optional<Duty> nextDuty = dutyRepository.findEarliestDutyStartingAfter(driverId, dutyEnd);
        if (nextDuty.isPresent()) {
            Duty next = nextDuty.get();
            if (excludeDutyId == null || !next.getId().equals(excludeDutyId)) {
                Duration gap = Duration.between(dutyEnd, next.getStartTime());
                if (gap.compareTo(requiredRest) < 0) {
                    reasons.add("Insufficient rest: only " + formatDuration(gap)
                            + " between new duty (ends " + dutyEnd 
                            + ") and next duty (starts " + next.getStartTime() + "). Required: " 
                            + requiredRestHours + " hours");
                }
            }
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return hours + "h " + minutes + "m";
    }
}

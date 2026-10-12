package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.dto.request.ReassignmentRequest;
import com.moveinsync.roster.dto.response.*;
import com.moveinsync.roster.exception.BusinessRuleException;
import com.moveinsync.roster.exception.ResourceNotFoundException;
import com.moveinsync.roster.model.entity.*;
import com.moveinsync.roster.model.enums.*;
import com.moveinsync.roster.repository.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RosterService {

    private static final Logger log = LoggerFactory.getLogger(RosterService.class);

    private final DutyRepository dutyRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final AssignmentAuditRepository auditRepository;
    private final EligibilityService eligibilityService;
    private final VehicleLicencePolicy licencePolicy;
    private final Clock clock;
    private final int regularWorkHoursPerDay;

    // Micrometer metrics
    private final Counter rosterGenerationCounter;
    private final Counter successfulAssignmentCounter;
    private final Counter unfilledDutyCounter;
    private final Counter restRuleRejectionCounter;
    private final Counter driverOverlapRejectionCounter;
    private final Counter vehicleConflictRejectionCounter;
    private final Counter licenceRejectionCounter;
    private final Counter reassignmentAttemptCounter;
    private final Counter reassignmentSuccessCounter;
    private final Timer rosterGenerationTimer;

    public RosterService(DutyRepository dutyRepository,
                         DriverRepository driverRepository,
                         VehicleRepository vehicleRepository,
                         AssignmentAuditRepository auditRepository,
                         EligibilityService eligibilityService,
                         VehicleLicencePolicy licencePolicy,
                         Clock clock,
                         @Value("${app.roster.regular-work-hours-per-day:8}") int regularWorkHoursPerDay,
                         MeterRegistry meterRegistry) {
        this.dutyRepository = dutyRepository;
        this.driverRepository = driverRepository;
        this.vehicleRepository = vehicleRepository;
        this.auditRepository = auditRepository;
        this.eligibilityService = eligibilityService;
        this.licencePolicy = licencePolicy;
        this.clock = clock;
        this.regularWorkHoursPerDay = regularWorkHoursPerDay;

        this.rosterGenerationCounter = Counter.builder("roster.generation.attempts").register(meterRegistry);
        this.successfulAssignmentCounter = Counter.builder("roster.assignments.successful").register(meterRegistry);
        this.unfilledDutyCounter = Counter.builder("roster.duties.unfilled").register(meterRegistry);
        this.restRuleRejectionCounter = Counter.builder("roster.rejections.rest_rule").register(meterRegistry);
        this.driverOverlapRejectionCounter = Counter.builder("roster.rejections.driver_overlap").register(meterRegistry);
        this.vehicleConflictRejectionCounter = Counter.builder("roster.rejections.vehicle_conflict").register(meterRegistry);
        this.licenceRejectionCounter = Counter.builder("roster.rejections.licence").register(meterRegistry);
        this.reassignmentAttemptCounter = Counter.builder("roster.reassignment.attempts").register(meterRegistry);
        this.reassignmentSuccessCounter = Counter.builder("roster.reassignment.successful").register(meterRegistry);
        this.rosterGenerationTimer = Timer.builder("roster.generation.duration").register(meterRegistry);
    }

    /**
     * Generate roster: constrained, scarcity-first greedy assignment.
     *
     * Algorithm:
     * 1. Collect all unassigned ACTIVE duties in range
     * 2. For each duty, compute feasible (driver, vehicle) pairs
     * 3. Sort duties by number of feasible combinations (ascending) — scarce first
     *    Break ties by start time (ascending), then duty ID (ascending) for determinism
     * 4. For each duty (in scarcity order):
     *    a. Re-evaluate feasible combinations (schedule changed by earlier assignments)
     *    b. Filter out any combo violating hard rules
     *    c. Rank remaining combos by: lower incremental overtime > better utilization > workload balance > stable ID tiebreaker
     *    d. Assign the best combo, or mark duty as unfilled with reason
     * 5. Return results
     *
     * Complexity: O(D * (V * Dr * Q)) where D=duties, V=vehicles, Dr=drivers, Q=query cost for overlap/rest checks.
     * The scarcity sort is O(D log D). Each duty evaluates at most V*Dr combinations, each requiring DB queries.
     */
    @Transactional
    public RosterGenerationResponse generateRoster(LocalDateTime startTime, LocalDateTime endTime) {
        rosterGenerationCounter.increment();
        long startMs = System.currentTimeMillis();

        // Validate
        if (!endTime.isAfter(startTime)) {
            throw new BusinessRuleException("End time must be after start time");
        }

        String actingUser = getActingUser();
        log.info("Generating roster for period {} to {} by user {}", startTime, endTime, actingUser);

        // 1. Get unassigned active duties
        List<Duty> duties = dutyRepository.findUnassignedActiveDuties(startTime, endTime);
        if (duties.isEmpty()) {
            log.info("No unassigned duties found in range");
            return buildResponse(List.of(), List.of(), startMs);
        }

        // 2. Get available resources
        List<Driver> activeDrivers = driverRepository.findByActiveTrue();
        List<Vehicle> activeVehicles = vehicleRepository.findByActiveTrue();

        // 3. Compute initial feasibility counts and sort by scarcity
        Map<Long, Integer> feasibilityCounts = new HashMap<>();
        for (Duty duty : duties) {
            int count = countFeasibleCombinations(duty, activeDrivers, activeVehicles);
            feasibilityCounts.put(duty.getId(), count);
        }

        duties.sort(Comparator
                .comparingInt((Duty d) -> feasibilityCounts.getOrDefault(d.getId(), 0))
                .thenComparing(Duty::getStartTime)
                .thenComparing(Duty::getId));

        // 4. Greedy assignment
        List<Duty> assigned = new ArrayList<>();
        List<Duty> unfilled = new ArrayList<>();
        // Track how much work each driver has been assigned (in minutes) per calendar day
        Map<Long, Map<LocalDate, Long>> driverDailyMinutes = new HashMap<>();

        for (Duty duty : duties) {
            boolean wasAssigned = tryAssignDuty(duty, activeDrivers, activeVehicles, 
                    driverDailyMinutes, actingUser);
            if (wasAssigned) {
                assigned.add(duty);
                successfulAssignmentCounter.increment();
            } else {
                unfilled.add(duty);
                unfilledDutyCounter.increment();
            }
        }

        log.info("Roster generation complete: {} assigned, {} unfilled out of {} total", 
                assigned.size(), unfilled.size(), duties.size());

        return buildResponse(assigned, unfilled, startMs);
    }

    /**
     * Try to assign a duty using the best feasible driver-vehicle combination.
     * Returns true if assigned, false if unfilled.
     */
    private boolean tryAssignDuty(Duty duty, List<Driver> drivers, List<Vehicle> vehicles,
                                   Map<Long, Map<LocalDate, Long>> driverDailyMinutes,
                                   String actingUser) {
        VehicleType requiredType = duty.getRequiredVehicleType();
        
        // Filter vehicles by type
        List<Vehicle> suitableVehicles = vehicles.stream()
                .filter(v -> v.isActive() && v.getVehicleType() == requiredType)
                .toList();

        if (suitableVehicles.isEmpty()) {
            duty.setUnfilledReason("No active vehicles of type " + requiredType + " available");
            dutyRepository.save(duty);
            return false;
        }

        // Track all rejection reasons for reporting
        Map<String, Integer> rejectionCounts = new HashMap<>();
        List<CandidateScore> candidates = new ArrayList<>();

        for (Vehicle vehicle : suitableVehicles) {
            for (Driver driver : drivers) {
                List<String> rejections = eligibilityService.checkEligibility(
                        driver, vehicle, duty.getStartTime(), duty.getEndTime(),
                        requiredType, null);

                if (rejections.isEmpty()) {
                    // Calculate score
                    double overtimeScore = calculateOvertimeScore(driver, duty, driverDailyMinutes);
                    int existingDutyCount = getExistingDutyCount(driver.getId());
                    candidates.add(new CandidateScore(driver, vehicle, overtimeScore, existingDutyCount));
                } else {
                    // Track rejection reasons for metrics
                    for (String reason : rejections) {
                        String category = categorizeRejection(reason);
                        rejectionCounts.merge(category, 1, Integer::sum);
                    }
                }
            }
        }

        // Update metrics
        rejectionCounts.forEach((category, count) -> {
            switch (category) {
                case "REST_RULE" -> restRuleRejectionCounter.increment(count);
                case "DRIVER_OVERLAP" -> driverOverlapRejectionCounter.increment(count);
                case "VEHICLE_CONFLICT" -> vehicleConflictRejectionCounter.increment(count);
                case "LICENCE" -> licenceRejectionCounter.increment(count);
            }
        });

        if (candidates.isEmpty()) {
            String reason = buildUnfilledReason(rejectionCounts);
            duty.setUnfilledReason(reason);
            dutyRepository.save(duty);
            return false;
        }

        // Rank: lower overtime > better utilization (prefer already-scheduled drivers) > workload balance (fewer duties) > stable ID tiebreaker
        candidates.sort(Comparator
                .comparingDouble(CandidateScore::overtimeScore)
                .thenComparing(Comparator.comparingInt(CandidateScore::existingDutyCount).reversed())
                .thenComparing(c -> c.driver().getId())
                .thenComparing(c -> c.vehicle().getId()));

        CandidateScore best = candidates.get(0);
        
        // Assign
        duty.setAssignedDriver(best.driver());
        duty.setAssignedVehicle(best.vehicle());
        duty.setAssignmentState(AssignmentState.ASSIGNED);
        duty.setUnfilledReason(null);
        dutyRepository.save(duty);

        // Update daily minutes tracker
        updateDailyMinutes(driverDailyMinutes, best.driver().getId(), duty.getStartTime(), duty.getEndTime());

        // Create audit record
        createAuditRecord(duty, null, null, best.driver().getId(), best.vehicle().getId(),
                AuditAction.ROSTER_GENERATION, "Roster generation assignment", actingUser);

        return true;
    }

    /**
     * Reassign a duty when a driver drops out.
     */
    @Transactional
    public ReassignmentResponse reassignDuty(ReassignmentRequest request) {
        reassignmentAttemptCounter.increment();
        String actingUser = getActingUser();

        // 1. Load and validate duty
        Duty duty = dutyRepository.findByIdForUpdate(request.getDutyId())
                .orElseThrow(() -> new ResourceNotFoundException("Duty", request.getDutyId()));

        if (duty.getDutyStatus() != DutyStatus.ACTIVE) {
            throw new BusinessRuleException("Cannot reassign a " + duty.getDutyStatus() + " duty");
        }

        Long previousDriverId = duty.getAssignedDriver() != null ? duty.getAssignedDriver().getId() : null;
        Long previousVehicleId = duty.getAssignedVehicle() != null ? duty.getAssignedVehicle().getId() : null;

        // 2. Unassign current driver
        duty.setAssignedDriver(null);
        duty.setAssignedVehicle(null);
        duty.setAssignmentState(AssignmentState.UNASSIGNED);

        // 3. Find replacement (exclude unavailable driver)
        List<Driver> candidates = driverRepository.findByActiveTrue().stream()
                .filter(d -> !d.getId().equals(request.getUnavailableDriverId()))
                .toList();

        List<Vehicle> vehicles = vehicleRepository.findByActiveTrueAndVehicleType(duty.getRequiredVehicleType());

        boolean reassigned = false;
        Long newDriverId = null;
        Long newVehicleId = null;

        for (Vehicle vehicle : vehicles) {
            for (Driver driver : candidates) {
                List<String> rejections = eligibilityService.checkEligibility(
                        driver, vehicle, duty.getStartTime(), duty.getEndTime(),
                        duty.getRequiredVehicleType(), duty.getId());

                if (rejections.isEmpty()) {
                    duty.setAssignedDriver(driver);
                    duty.setAssignedVehicle(vehicle);
                    duty.setAssignmentState(AssignmentState.ASSIGNED);
                    duty.setUnfilledReason(null);
                    newDriverId = driver.getId();
                    newVehicleId = vehicle.getId();
                    reassigned = true;
                    break;
                }
            }
            if (reassigned) break;
        }

        if (!reassigned) {
            duty.setUnfilledReason("Reassignment failed: no eligible driver-vehicle combination found. Reason: " + request.getReason());
        }

        dutyRepository.save(duty);

        // 4. Create audit record
        createAuditRecord(duty, previousDriverId, previousVehicleId, newDriverId, newVehicleId,
                AuditAction.REASSIGNMENT, request.getReason(), actingUser);

        if (reassigned) {
            reassignmentSuccessCounter.increment();
        }

        // 5. Build response
        ReassignmentResponse response = new ReassignmentResponse();
        response.setDutyId(duty.getId());
        response.setReassigned(reassigned);
        response.setPreviousDriverId(previousDriverId);
        response.setNewDriverId(newDriverId);
        response.setPreviousVehicleId(previousVehicleId);
        response.setNewVehicleId(newVehicleId);
        response.setStatus(reassigned ? "REASSIGNED" : "UNASSIGNED");
        response.setMessage(reassigned ? "Successfully reassigned to driver " + newDriverId 
                : "No eligible replacement found. Duty remains unassigned.");
        return response;
    }

    // Helper methods below:

    private int countFeasibleCombinations(Duty duty, List<Driver> drivers, List<Vehicle> vehicles) {
        VehicleType type = duty.getRequiredVehicleType();
        int count = 0;
        List<Vehicle> suitable = vehicles.stream()
                .filter(v -> v.isActive() && v.getVehicleType() == type).toList();
        for (Vehicle v : suitable) {
            for (Driver d : drivers) {
                if (eligibilityService.checkEligibility(d, v, duty.getStartTime(), duty.getEndTime(), type, null).isEmpty()) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Calculate overtime score: sum of minutes exceeding regularWorkHoursPerDay across each calendar day.
     * Lower is better. Split duty at midnight boundaries.
     */
    private double calculateOvertimeScore(Driver driver, Duty duty,
                                           Map<Long, Map<LocalDate, Long>> dailyMinutes) {
        Map<LocalDate, Long> driverMinutes = dailyMinutes.getOrDefault(driver.getId(), new HashMap<>());
        
        // Calculate additional minutes per day from this duty (split at midnight)
        Map<LocalDate, Long> additionalMinutes = splitDutyByDay(duty.getStartTime(), duty.getEndTime());
        
        double overtime = 0;
        long regularMinutes = (long) regularWorkHoursPerDay * 60;
        
        for (Map.Entry<LocalDate, Long> entry : additionalMinutes.entrySet()) {
            long existing = driverMinutes.getOrDefault(entry.getKey(), 0L);
            long total = existing + entry.getValue();
            if (total > regularMinutes) {
                overtime += (total - regularMinutes);
            }
        }
        
        return overtime;
    }

    /**
     * Split a duty's duration across calendar days at midnight boundaries.
     * Returns map of date -> minutes worked on that date.
     */
    private Map<LocalDate, Long> splitDutyByDay(LocalDateTime start, LocalDateTime end) {
        Map<LocalDate, Long> result = new LinkedHashMap<>();
        LocalDateTime current = start;
        
        while (current.isBefore(end)) {
            LocalDateTime dayEnd = current.toLocalDate().plusDays(1).atStartOfDay();
            if (dayEnd.isAfter(end)) {
                dayEnd = end;
            }
            long minutes = Duration.between(current, dayEnd).toMinutes();
            result.merge(current.toLocalDate(), minutes, Long::sum);
            current = dayEnd;
        }
        
        return result;
    }

    private void updateDailyMinutes(Map<Long, Map<LocalDate, Long>> dailyMinutes, Long driverId,
                                     LocalDateTime start, LocalDateTime end) {
        Map<LocalDate, Long> driverMinutes = dailyMinutes.computeIfAbsent(driverId, k -> new HashMap<>());
        Map<LocalDate, Long> additional = splitDutyByDay(start, end);
        additional.forEach((date, mins) -> driverMinutes.merge(date, mins, Long::sum));
    }

    private int getExistingDutyCount(Long driverId) {
        return dutyRepository.findNonCancelledDutiesByDriverOrdered(driverId).size();
    }

    private String categorizeRejection(String reason) {
        if (reason.contains("rest") || reason.contains("Rest")) return "REST_RULE";
        if (reason.contains("overlapping") && reason.contains("Driver")) return "DRIVER_OVERLAP";
        if (reason.contains("overlapping") && reason.contains("Vehicle")) return "VEHICLE_CONFLICT";
        if (reason.contains("licence") || reason.contains("Licence")) return "LICENCE";
        if (reason.contains("medical")) return "MEDICAL_EXPIRED";
        if (reason.contains("training")) return "TRAINING_EXPIRED";
        if (reason.contains("active")) return "INACTIVE";
        return "OTHER";
    }

    private String buildUnfilledReason(Map<String, Integer> rejectionCounts) {
        if (rejectionCounts.isEmpty()) {
            return "No eligible driver-vehicle combinations available";
        }
        StringBuilder sb = new StringBuilder("No eligible assignment: ");
        rejectionCounts.forEach((reason, count) -> 
                sb.append(reason).append("=").append(count).append(", "));
        sb.setLength(sb.length() - 2);
        return sb.toString();
    }

    private void createAuditRecord(Duty duty, Long prevDriverId, Long prevVehicleId,
                                    Long newDriverId, Long newVehicleId,
                                    AuditAction action, String reason, String actingUser) {
        AssignmentAudit audit = new AssignmentAudit();
        audit.setDuty(duty);
        audit.setPreviousDriverId(prevDriverId);
        audit.setPreviousVehicleId(prevVehicleId);
        audit.setNewDriverId(newDriverId);
        audit.setNewVehicleId(newVehicleId);
        audit.setAction(action);
        audit.setReason(reason);
        audit.setActingUser(actingUser);
        auditRepository.save(audit);
    }

    private RosterGenerationResponse buildResponse(List<Duty> assigned, List<Duty> unfilled, long startMs) {
        RosterGenerationResponse response = new RosterGenerationResponse();
        response.setTotalDuties(assigned.size() + unfilled.size());
        response.setAssignedDuties(assigned.size());
        response.setUnfilledDuties(unfilled.size());
        response.setAssignments(assigned.stream().map(this::toDutyResponse).toList());
        response.setUnfilled(unfilled.stream().map(this::toDutyResponse).toList());
        response.setGenerationTimeMs(String.valueOf(System.currentTimeMillis() - startMs));
        return response;
    }

    private DutyResponse toDutyResponse(Duty duty) {
        DutyResponse r = new DutyResponse();
        r.setId(duty.getId());
        r.setRequiredVehicleType(duty.getRequiredVehicleType().name());
        r.setStartTime(duty.getStartTime());
        r.setEndTime(duty.getEndTime());
        if (duty.getAssignedDriver() != null) {
            r.setAssignedDriverId(duty.getAssignedDriver().getId());
            r.setAssignedDriverName(duty.getAssignedDriver().getName());
        }
        if (duty.getAssignedVehicle() != null) {
            r.setAssignedVehicleId(duty.getAssignedVehicle().getId());
            r.setAssignedVehicleRegistration(duty.getAssignedVehicle().getRegistrationNumber());
        }
        r.setDutyStatus(duty.getDutyStatus().name());
        r.setAssignmentState(duty.getAssignmentState().name());
        r.setUnfilledReason(duty.getUnfilledReason());
        r.setCreatedAt(duty.getCreatedAt());
        r.setUpdatedAt(duty.getUpdatedAt());
        return r;
    }

    private String getActingUser() {
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            return "system";
        }
    }

    // Record for ranking candidates
    private record CandidateScore(Driver driver, Vehicle vehicle, double overtimeScore, int existingDutyCount) {}
}

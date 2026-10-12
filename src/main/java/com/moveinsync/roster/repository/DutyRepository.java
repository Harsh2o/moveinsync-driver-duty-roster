package com.moveinsync.roster.repository;

import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DutyRepository extends JpaRepository<Duty, Long> {

    /**
     * Find duties that overlap with a given time range for a specific driver.
     * Uses half-open interval: [start, end) — newStart < existingEnd AND existingStart < newEnd.
     * Excludes cancelled duties.
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedDriver.id = :driverId " +
           "AND d.dutyStatus <> 'CANCELLED' " +
           "AND d.startTime < :endTime AND d.endTime > :startTime")
    List<Duty> findOverlappingDutiesForDriver(
            @Param("driverId") Long driverId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find duties that overlap with a given time range for a specific vehicle.
     * Excludes cancelled duties.
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedVehicle.id = :vehicleId " +
           "AND d.dutyStatus <> 'CANCELLED' " +
           "AND d.startTime < :endTime AND d.endTime > :startTime")
    List<Duty> findOverlappingDutiesForVehicle(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all non-cancelled duties assigned to a driver, ordered by start time.
     * Used for rest-hour calculations.
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedDriver.id = :driverId " +
           "AND d.dutyStatus <> 'CANCELLED' " +
           "ORDER BY d.startTime ASC")
    List<Duty> findNonCancelledDutiesByDriverOrdered(@Param("driverId") Long driverId);

    /**
     * Find the duty ending most recently before a given time for a driver.
     * Excludes cancelled duties.
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedDriver.id = :driverId " +
           "AND d.dutyStatus <> 'CANCELLED' " +
           "AND d.endTime <= :beforeTime " +
           "ORDER BY d.endTime DESC LIMIT 1")
    Optional<Duty> findLatestDutyEndingBefore(
            @Param("driverId") Long driverId,
            @Param("beforeTime") LocalDateTime beforeTime);

    /**
     * Find the duty starting earliest after a given time for a driver.
     * Excludes cancelled duties.
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedDriver.id = :driverId " +
           "AND d.dutyStatus <> 'CANCELLED' " +
           "AND d.startTime >= :afterTime " +
           "ORDER BY d.startTime ASC LIMIT 1")
    Optional<Duty> findEarliestDutyStartingAfter(
            @Param("driverId") Long driverId,
            @Param("afterTime") LocalDateTime afterTime);

    /**
     * Find duties in a date range with optional filters.
     */
    @Query("SELECT d FROM Duty d WHERE d.startTime >= :startTime AND d.endTime <= :endTime")
    Page<Duty> findByDateRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable);

    @Query("SELECT d FROM Duty d WHERE d.startTime >= :startTime AND d.endTime <= :endTime " +
           "AND d.assignmentState = :state")
    Page<Duty> findByDateRangeAndAssignmentState(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("state") AssignmentState state,
            Pageable pageable);

    @Query("SELECT d FROM Duty d WHERE d.startTime >= :startTime AND d.endTime <= :endTime " +
           "AND d.dutyStatus = :status")
    Page<Duty> findByDateRangeAndDutyStatus(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") DutyStatus status,
            Pageable pageable);

    /**
     * Find unassigned active duties in a date range.
     */
    @Query("SELECT d FROM Duty d WHERE d.dutyStatus = 'ACTIVE' " +
           "AND d.assignmentState = 'UNASSIGNED' " +
           "AND d.startTime >= :startTime AND d.endTime <= :endTime " +
           "ORDER BY d.startTime ASC")
    List<Duty> findUnassignedActiveDuties(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find all active duties in a date range for roster generation.
     */
    @Query("SELECT d FROM Duty d WHERE d.dutyStatus = 'ACTIVE' " +
           "AND d.startTime >= :startTime AND d.startTime < :endTime " +
           "ORDER BY d.startTime ASC")
    List<Duty> findActiveDutiesInRange(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Count duties by assignment state in a date range.
     */
    @Query("SELECT COUNT(d) FROM Duty d WHERE d.dutyStatus = 'ACTIVE' " +
           "AND d.assignmentState = :state " +
           "AND d.startTime >= :startTime AND d.endTime <= :endTime")
    long countByAssignmentStateInRange(
            @Param("state") AssignmentState state,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find duties assigned to a driver in a date range (for utilization calculation).
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedDriver.id = :driverId " +
           "AND d.dutyStatus IN ('ACTIVE', 'COMPLETED') " +
           "AND d.assignmentState = 'ASSIGNED' " +
           "AND d.startTime >= :startTime AND d.endTime <= :endTime")
    List<Duty> findAssignedDutiesForDriverInRange(
            @Param("driverId") Long driverId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Find duties assigned to a vehicle in a date range (for utilization calculation).
     */
    @Query("SELECT d FROM Duty d WHERE d.assignedVehicle.id = :vehicleId " +
           "AND d.dutyStatus IN ('ACTIVE', 'COMPLETED') " +
           "AND d.assignmentState = 'ASSIGNED' " +
           "AND d.startTime >= :startTime AND d.endTime <= :endTime")
    List<Duty> findAssignedDutiesForVehicleInRange(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    /**
     * Lock a duty for update to prevent concurrent assignment.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Duty d WHERE d.id = :id")
    Optional<Duty> findByIdForUpdate(@Param("id") Long id);

    Page<Duty> findByDutyStatus(DutyStatus status, Pageable pageable);

    Page<Duty> findByAssignmentState(AssignmentState state, Pageable pageable);
}

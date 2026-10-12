package com.moveinsync.roster.model.entity;

import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.VehicleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "duties")
public class Duty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "required_vehicle_type", nullable = false, length = 10)
    private VehicleType requiredVehicleType;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_driver_id")
    private Driver assignedDriver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_vehicle_id")
    private Vehicle assignedVehicle;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "duty_status", nullable = false, length = 20)
    private DutyStatus dutyStatus = DutyStatus.ACTIVE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_state", nullable = false, length = 20)
    private AssignmentState assignmentState = AssignmentState.UNASSIGNED;

    @Column(name = "unfilled_reason", columnDefinition = "TEXT")
    private String unfilledReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    public Duty() {}

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public VehicleType getRequiredVehicleType() {
        return requiredVehicleType;
    }

    public void setRequiredVehicleType(VehicleType requiredVehicleType) {
        this.requiredVehicleType = requiredVehicleType;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Driver getAssignedDriver() {
        return assignedDriver;
    }

    public void setAssignedDriver(Driver assignedDriver) {
        this.assignedDriver = assignedDriver;
    }

    public Vehicle getAssignedVehicle() {
        return assignedVehicle;
    }

    public void setAssignedVehicle(Vehicle assignedVehicle) {
        this.assignedVehicle = assignedVehicle;
    }

    public DutyStatus getDutyStatus() {
        return dutyStatus;
    }

    public void setDutyStatus(DutyStatus dutyStatus) {
        this.dutyStatus = dutyStatus;
    }

    public AssignmentState getAssignmentState() {
        return assignmentState;
    }

    public void setAssignmentState(AssignmentState assignmentState) {
        this.assignmentState = assignmentState;
    }

    public String getUnfilledReason() {
        return unfilledReason;
    }

    public void setUnfilledReason(String unfilledReason) {
        this.unfilledReason = unfilledReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Duty other = (Duty) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

package com.moveinsync.roster.model.entity;

import com.moveinsync.roster.model.enums.AuditAction;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "assignment_audits")
public class AssignmentAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duty_id", nullable = false)
    private Duty duty;

    @Column(name = "previous_driver_id")
    private Long previousDriverId;

    @Column(name = "previous_vehicle_id")
    private Long previousVehicleId;

    @Column(name = "new_driver_id")
    private Long newDriverId;

    @Column(name = "new_vehicle_id")
    private Long newVehicleId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private AuditAction action;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @NotBlank
    @Column(name = "acting_user", nullable = false, length = 50)
    private String actingUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AssignmentAudit() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Duty getDuty() {
        return duty;
    }

    public void setDuty(Duty duty) {
        this.duty = duty;
    }

    public Long getPreviousDriverId() {
        return previousDriverId;
    }

    public void setPreviousDriverId(Long previousDriverId) {
        this.previousDriverId = previousDriverId;
    }

    public Long getPreviousVehicleId() {
        return previousVehicleId;
    }

    public void setPreviousVehicleId(Long previousVehicleId) {
        this.previousVehicleId = previousVehicleId;
    }

    public Long getNewDriverId() {
        return newDriverId;
    }

    public void setNewDriverId(Long newDriverId) {
        this.newDriverId = newDriverId;
    }

    public Long getNewVehicleId() {
        return newVehicleId;
    }

    public void setNewVehicleId(Long newVehicleId) {
        this.newVehicleId = newVehicleId;
    }

    public AuditAction getAction() {
        return action;
    }

    public void setAction(AuditAction action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getActingUser() {
        return actingUser;
    }

    public void setActingUser(String actingUser) {
        this.actingUser = actingUser;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AssignmentAudit other = (AssignmentAudit) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

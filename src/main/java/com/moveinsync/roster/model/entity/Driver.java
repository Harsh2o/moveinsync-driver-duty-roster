package com.moveinsync.roster.model.entity;

import com.moveinsync.roster.model.enums.LicenceClass;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotBlank
    @Size(max = 20)
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "licence_class", nullable = false, length = 10)
    private LicenceClass licenceClass;

    @NotNull
    @Column(name = "licence_expiry_date", nullable = false)
    private LocalDate licenceExpiryDate;

    @NotNull
    @Column(name = "medical_validity_date", nullable = false)
    private LocalDate medicalValidityDate;

    @NotNull
    @Column(name = "training_validity_date", nullable = false)
    private LocalDate trainingValidityDate;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version")
    private Integer version;

    public Driver() {}

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public LicenceClass getLicenceClass() {
        return licenceClass;
    }

    public void setLicenceClass(LicenceClass licenceClass) {
        this.licenceClass = licenceClass;
    }

    public LocalDate getLicenceExpiryDate() {
        return licenceExpiryDate;
    }

    public void setLicenceExpiryDate(LocalDate licenceExpiryDate) {
        this.licenceExpiryDate = licenceExpiryDate;
    }

    public LocalDate getMedicalValidityDate() {
        return medicalValidityDate;
    }

    public void setMedicalValidityDate(LocalDate medicalValidityDate) {
        this.medicalValidityDate = medicalValidityDate;
    }

    public LocalDate getTrainingValidityDate() {
        return trainingValidityDate;
    }

    public void setTrainingValidityDate(LocalDate trainingValidityDate) {
        this.trainingValidityDate = trainingValidityDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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
        Driver other = (Driver) o;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

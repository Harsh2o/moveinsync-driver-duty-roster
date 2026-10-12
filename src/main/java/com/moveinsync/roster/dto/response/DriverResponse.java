package com.moveinsync.roster.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DriverResponse {
    private Long id;
    private String name;
    private String licenceClass;
    private LocalDate licenceExpiryDate;
    private LocalDate medicalValidityDate;
    private LocalDate trainingValidityDate;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public DriverResponse() {
    }

    public DriverResponse(Long id, String name, String licenceClass, LocalDate licenceExpiryDate, LocalDate medicalValidityDate, LocalDate trainingValidityDate, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.licenceClass = licenceClass;
        this.licenceExpiryDate = licenceExpiryDate;
        this.medicalValidityDate = medicalValidityDate;
        this.trainingValidityDate = trainingValidityDate;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getLicenceClass() {
        return licenceClass;
    }

    public void setLicenceClass(String licenceClass) {
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
}

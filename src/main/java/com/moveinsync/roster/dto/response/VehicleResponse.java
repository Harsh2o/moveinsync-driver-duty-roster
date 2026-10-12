package com.moveinsync.roster.dto.response;

import java.time.LocalDateTime;

public class VehicleResponse {
    private Long id;
    private String registrationNumber;
    private String vehicleType;
    private String requiredLicenceClass;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public VehicleResponse() {
    }

    public VehicleResponse(Long id, String registrationNumber, String vehicleType, String requiredLicenceClass, boolean active, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
        this.requiredLicenceClass = requiredLicenceClass;
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

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getRequiredLicenceClass() {
        return requiredLicenceClass;
    }

    public void setRequiredLicenceClass(String requiredLicenceClass) {
        this.requiredLicenceClass = requiredLicenceClass;
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

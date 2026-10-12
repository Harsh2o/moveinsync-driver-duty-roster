package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VehicleRequest {
    @NotBlank(message = "Registration number is required")
    @Size(max = 20)
    private String registrationNumber;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    public VehicleRequest() {
    }

    public VehicleRequest(String registrationNumber, String vehicleType) {
        this.registrationNumber = registrationNumber;
        this.vehicleType = vehicleType;
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
}

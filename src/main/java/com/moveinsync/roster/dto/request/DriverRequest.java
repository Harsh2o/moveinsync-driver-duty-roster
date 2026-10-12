package com.moveinsync.roster.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class DriverRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20)
    private String phoneNumber;

    @NotBlank(message = "Licence class is required")
    private String licenceClass;

    @NotNull(message = "Licence expiry date is required")
    private LocalDate licenceExpiryDate;

    @NotNull(message = "Medical validity date is required")
    private LocalDate medicalValidityDate;

    @NotNull(message = "Training validity date is required")
    private LocalDate trainingValidityDate;

    public DriverRequest() {
    }

    public DriverRequest(String name, String phoneNumber, String licenceClass, LocalDate licenceExpiryDate, LocalDate medicalValidityDate, LocalDate trainingValidityDate) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.licenceClass = licenceClass;
        this.licenceExpiryDate = licenceExpiryDate;
        this.medicalValidityDate = medicalValidityDate;
        this.trainingValidityDate = trainingValidityDate;
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
}

package com.moveinsync.roster.dto.response;

import java.util.List;

public class UtilizationResponse {
    private List<DriverUtilization> driverUtilizations;
    private List<VehicleUtilization> vehicleUtilizations;

    public UtilizationResponse() {
    }

    public UtilizationResponse(List<DriverUtilization> driverUtilizations, List<VehicleUtilization> vehicleUtilizations) {
        this.driverUtilizations = driverUtilizations;
        this.vehicleUtilizations = vehicleUtilizations;
    }

    public List<DriverUtilization> getDriverUtilizations() {
        return driverUtilizations;
    }

    public void setDriverUtilizations(List<DriverUtilization> driverUtilizations) {
        this.driverUtilizations = driverUtilizations;
    }

    public List<VehicleUtilization> getVehicleUtilizations() {
        return vehicleUtilizations;
    }

    public void setVehicleUtilizations(List<VehicleUtilization> vehicleUtilizations) {
        this.vehicleUtilizations = vehicleUtilizations;
    }

    public static class DriverUtilization {
        private Long driverId;
        private String driverName;
        private String licenceClass;
        private int assignedDuties;
        private double totalScheduledHours;
        private double utilizationPercentage;
        private double overtimeHours;
        private boolean available;

        public DriverUtilization() {
        }

        public DriverUtilization(Long driverId, String driverName, String licenceClass, int assignedDuties, double totalScheduledHours, double utilizationPercentage, double overtimeHours, boolean available) {
            this.driverId = driverId;
            this.driverName = driverName;
            this.licenceClass = licenceClass;
            this.assignedDuties = assignedDuties;
            this.totalScheduledHours = totalScheduledHours;
            this.utilizationPercentage = utilizationPercentage;
            this.overtimeHours = overtimeHours;
            this.available = available;
        }

        public Long getDriverId() {
            return driverId;
        }

        public void setDriverId(Long driverId) {
            this.driverId = driverId;
        }

        public String getDriverName() {
            return driverName;
        }

        public void setDriverName(String driverName) {
            this.driverName = driverName;
        }

        public String getLicenceClass() {
            return licenceClass;
        }

        public void setLicenceClass(String licenceClass) {
            this.licenceClass = licenceClass;
        }

        public int getAssignedDuties() {
            return assignedDuties;
        }

        public void setAssignedDuties(int assignedDuties) {
            this.assignedDuties = assignedDuties;
        }

        public double getTotalScheduledHours() {
            return totalScheduledHours;
        }

        public void setTotalScheduledHours(double totalScheduledHours) {
            this.totalScheduledHours = totalScheduledHours;
        }

        public double getUtilizationPercentage() {
            return utilizationPercentage;
        }

        public void setUtilizationPercentage(double utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
        }

        public double getOvertimeHours() {
            return overtimeHours;
        }

        public void setOvertimeHours(double overtimeHours) {
            this.overtimeHours = overtimeHours;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }

    public static class VehicleUtilization {
        private Long vehicleId;
        private String registrationNumber;
        private String vehicleType;
        private int assignedDuties;
        private double totalScheduledHours;
        private double utilizationPercentage;
        private boolean available;

        public VehicleUtilization() {
        }

        public VehicleUtilization(Long vehicleId, String registrationNumber, String vehicleType, int assignedDuties, double totalScheduledHours, double utilizationPercentage, boolean available) {
            this.vehicleId = vehicleId;
            this.registrationNumber = registrationNumber;
            this.vehicleType = vehicleType;
            this.assignedDuties = assignedDuties;
            this.totalScheduledHours = totalScheduledHours;
            this.utilizationPercentage = utilizationPercentage;
            this.available = available;
        }

        public Long getVehicleId() {
            return vehicleId;
        }

        public void setVehicleId(Long vehicleId) {
            this.vehicleId = vehicleId;
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

        public int getAssignedDuties() {
            return assignedDuties;
        }

        public void setAssignedDuties(int assignedDuties) {
            this.assignedDuties = assignedDuties;
        }

        public double getTotalScheduledHours() {
            return totalScheduledHours;
        }

        public void setTotalScheduledHours(double totalScheduledHours) {
            this.totalScheduledHours = totalScheduledHours;
        }

        public double getUtilizationPercentage() {
            return utilizationPercentage;
        }

        public void setUtilizationPercentage(double utilizationPercentage) {
            this.utilizationPercentage = utilizationPercentage;
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }
    }
}

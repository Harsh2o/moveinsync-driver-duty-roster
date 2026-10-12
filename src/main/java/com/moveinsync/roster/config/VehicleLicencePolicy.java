package com.moveinsync.roster.config;

import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * Centralized vehicle-type-to-licence-class policy.
 * 
 * Defines the minimum licence class required to operate each vehicle type.
 * BUS requires HMV (mandatory rule per case study).
 * CAB and SHUTTLE default to LMV (reasonable assumption, easily configurable here).
 * 
 * An HMV licence holder can drive any vehicle that requires LMV or HMV.
 * An LMV licence holder can only drive vehicles requiring LMV.
 */
@Component
public class VehicleLicencePolicy {

    private final Map<VehicleType, LicenceClass> requiredLicenceMap;

    public VehicleLicencePolicy() {
        this.requiredLicenceMap = new EnumMap<>(VehicleType.class);
        requiredLicenceMap.put(VehicleType.BUS, LicenceClass.HMV);
        requiredLicenceMap.put(VehicleType.CAB, LicenceClass.LMV);
        requiredLicenceMap.put(VehicleType.SHUTTLE, LicenceClass.LMV);
    }

    /**
     * Returns the minimum required licence class for a given vehicle type.
     */
    public LicenceClass getRequiredLicenceClass(VehicleType vehicleType) {
        LicenceClass required = requiredLicenceMap.get(vehicleType);
        if (required == null) {
            throw new IllegalArgumentException("No licence policy defined for vehicle type: " + vehicleType);
        }
        return required;
    }

    /**
     * Checks if a driver's licence class satisfies the requirement for a vehicle type.
     * HMV satisfies both HMV and LMV requirements.
     * LMV satisfies only LMV requirements.
     */
    public boolean isLicenceSufficient(LicenceClass driverLicence, VehicleType vehicleType) {
        LicenceClass required = getRequiredLicenceClass(vehicleType);
        return isLicenceSufficient(driverLicence, required);
    }

    /**
     * Checks if a driver's licence class satisfies a required licence class.
     */
    public boolean isLicenceSufficient(LicenceClass driverLicence, LicenceClass requiredLicence) {
        if (requiredLicence == LicenceClass.HMV) {
            return driverLicence == LicenceClass.HMV;
        }
        // LMV requirement: both LMV and HMV satisfy it
        return true;
    }
}

package com.moveinsync.roster.service;

import com.moveinsync.roster.config.VehicleLicencePolicy;
import com.moveinsync.roster.dto.request.VehicleRequest;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.dto.response.VehicleResponse;
import com.moveinsync.roster.exception.BusinessRuleException;
import com.moveinsync.roster.exception.DuplicateResourceException;
import com.moveinsync.roster.exception.ResourceNotFoundException;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VehicleService {

    private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

    private final VehicleRepository vehicleRepository;
    private final VehicleLicencePolicy vehicleLicencePolicy;

    public VehicleService(VehicleRepository vehicleRepository, VehicleLicencePolicy vehicleLicencePolicy) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleLicencePolicy = vehicleLicencePolicy;
    }

    @Transactional
    @CacheEvict(value = "vehicles", allEntries = true)
    public VehicleResponse createVehicle(VehicleRequest request) {
        if (vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Vehicle with registration number already exists: " + request.getRegistrationNumber());
        }

        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(request.getVehicleType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessRuleException("Invalid vehicle type: " + request.getVehicleType());
        }

        LicenceClass requiredLicenceClass = vehicleLicencePolicy.getRequiredLicenceClass(vehicleType);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setVehicleType(vehicleType);
        vehicle.setRequiredLicenceClass(requiredLicenceClass);
        vehicle.setActive(true);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Created vehicle: id={}, registrationNumber={}", saved.getId(), saved.getRegistrationNumber());
        return toResponse(saved);
    }

    @Cacheable(value = "vehicles", key = "#id")
    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        return toResponse(vehicle);
    }

    @Transactional(readOnly = true)
    public PageResponse<VehicleResponse> listVehicles(int page, int size, Boolean active) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Vehicle> vehiclePage;
        if (active != null) {
            vehiclePage = vehicleRepository.findByActive(active, pageRequest);
        } else {
            vehiclePage = vehicleRepository.findAll(pageRequest);
        }

        List<VehicleResponse> content = vehiclePage.getContent().stream()
                .map(this::toResponse).toList();
        return new PageResponse<>(content, vehiclePage.getNumber(), vehiclePage.getSize(),
                vehiclePage.getTotalElements(), vehiclePage.getTotalPages(), vehiclePage.isLast());
    }

    @Transactional
    @CacheEvict(value = "vehicles", key = "#id")
    public VehicleResponse updateVehicle(Long id, VehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));

        if (!vehicle.getRegistrationNumber().equalsIgnoreCase(request.getRegistrationNumber())
                && vehicleRepository.existsByRegistrationNumber(request.getRegistrationNumber())) {
            throw new DuplicateResourceException("Vehicle with registration number already exists: " + request.getRegistrationNumber());
        }

        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(request.getVehicleType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessRuleException("Invalid vehicle type: " + request.getVehicleType());
        }

        LicenceClass requiredLicenceClass = vehicleLicencePolicy.getRequiredLicenceClass(vehicleType);

        vehicle.setRegistrationNumber(request.getRegistrationNumber());
        vehicle.setVehicleType(vehicleType);
        vehicle.setRequiredLicenceClass(requiredLicenceClass);

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Updated vehicle: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "vehicles", key = "#id")
    public VehicleResponse toggleVehicleActive(Long id, boolean active) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
        vehicle.setActive(active);
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Vehicle id={} active status changed to {}", id, active);
        return toResponse(saved);
    }

    private VehicleResponse toResponse(Vehicle vehicle) {
        VehicleResponse response = new VehicleResponse();
        response.setId(vehicle.getId());
        response.setRegistrationNumber(vehicle.getRegistrationNumber());
        response.setVehicleType(vehicle.getVehicleType().name());
        response.setRequiredLicenceClass(vehicle.getRequiredLicenceClass().name());
        response.setActive(vehicle.isActive());
        response.setCreatedAt(vehicle.getCreatedAt());
        response.setUpdatedAt(vehicle.getUpdatedAt());
        return response;
    }
}

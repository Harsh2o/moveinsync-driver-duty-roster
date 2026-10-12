package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.request.DriverRequest;
import com.moveinsync.roster.dto.response.DriverResponse;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.exception.ResourceNotFoundException;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.repository.DriverRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverService.class);
    private final DriverRepository driverRepository;

    public DriverService(DriverRepository driverRepository) {
        this.driverRepository = driverRepository;
    }

    @Transactional
    @CacheEvict(value = "drivers", allEntries = true)
    public DriverResponse createDriver(DriverRequest request) {
        Driver driver = new Driver();
        driver.setName(request.getName());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setLicenceClass(LicenceClass.valueOf(request.getLicenceClass().toUpperCase()));
        driver.setLicenceExpiryDate(request.getLicenceExpiryDate());
        driver.setMedicalValidityDate(request.getMedicalValidityDate());
        driver.setTrainingValidityDate(request.getTrainingValidityDate());
        driver.setActive(true);
        
        Driver saved = driverRepository.save(driver);
        log.info("Created driver: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Cacheable(value = "drivers", key = "#id")
    @Transactional(readOnly = true)
    public DriverResponse getDriver(Long id) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        return toResponse(driver);
    }

    @Transactional(readOnly = true)
    public PageResponse<DriverResponse> listDrivers(int page, int size, Boolean active) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("id").ascending());
        Page<Driver> driverPage;
        if (active != null) {
            driverPage = driverRepository.findByActive(active, pageRequest);
        } else {
            driverPage = driverRepository.findAll(pageRequest);
        }
        
        java.util.List<DriverResponse> content = driverPage.getContent().stream()
                .map(this::toResponse).toList();
        return new PageResponse<>(content, driverPage.getNumber(), driverPage.getSize(),
                driverPage.getTotalElements(), driverPage.getTotalPages(), driverPage.isLast());
    }

    @Transactional
    @CacheEvict(value = "drivers", key = "#id")
    public DriverResponse updateDriver(Long id, DriverRequest request) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        driver.setName(request.getName());
        driver.setPhoneNumber(request.getPhoneNumber());
        driver.setLicenceClass(LicenceClass.valueOf(request.getLicenceClass().toUpperCase()));
        driver.setLicenceExpiryDate(request.getLicenceExpiryDate());
        driver.setMedicalValidityDate(request.getMedicalValidityDate());
        driver.setTrainingValidityDate(request.getTrainingValidityDate());
        
        Driver saved = driverRepository.save(driver);
        log.info("Updated driver: id={}", saved.getId());
        return toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "drivers", key = "#id")
    public DriverResponse toggleDriverActive(Long id, boolean active) {
        Driver driver = driverRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", id));
        driver.setActive(active);
        Driver saved = driverRepository.save(driver);
        log.info("Driver id={} active status changed to {}", id, active);
        return toResponse(saved);
    }

    private DriverResponse toResponse(Driver driver) {
        DriverResponse response = new DriverResponse();
        response.setId(driver.getId());
        response.setName(driver.getName());
        // phoneNumber intentionally omitted from response for privacy
        response.setLicenceClass(driver.getLicenceClass().name());
        response.setLicenceExpiryDate(driver.getLicenceExpiryDate());
        response.setMedicalValidityDate(driver.getMedicalValidityDate());
        response.setTrainingValidityDate(driver.getTrainingValidityDate());
        response.setActive(driver.isActive());
        response.setCreatedAt(driver.getCreatedAt());
        response.setUpdatedAt(driver.getUpdatedAt());
        return response;
    }
}

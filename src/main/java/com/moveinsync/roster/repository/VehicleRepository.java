package com.moveinsync.roster.repository;

import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.VehicleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Page<Vehicle> findByActive(boolean active, Pageable pageable);

    List<Vehicle> findByActiveTrue();

    List<Vehicle> findByActiveTrueAndVehicleType(VehicleType vehicleType);

    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumber(String registrationNumber);
}

package com.moveinsync.roster.repository;

import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.enums.LicenceClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Page<Driver> findByActive(boolean active, Pageable pageable);

    List<Driver> findByActiveTrue();

    List<Driver> findByActiveTrueAndLicenceClass(LicenceClass licenceClass);

    @Query("SELECT d FROM Driver d WHERE d.active = true AND d.licenceClass IN :classes")
    List<Driver> findActiveDriversByLicenceClasses(@Param("classes") List<LicenceClass> classes);
}

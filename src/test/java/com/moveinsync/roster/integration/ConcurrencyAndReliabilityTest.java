package com.moveinsync.roster.integration;

import com.moveinsync.roster.dto.request.DriverRequest;
import com.moveinsync.roster.dto.request.DutyRequest;
import com.moveinsync.roster.dto.request.VehicleRequest;
import com.moveinsync.roster.dto.response.DriverResponse;
import com.moveinsync.roster.dto.response.DutyResponse;
import com.moveinsync.roster.dto.response.RosterGenerationResponse;
import com.moveinsync.roster.dto.response.VehicleResponse;
import com.moveinsync.roster.service.DriverService;
import com.moveinsync.roster.service.DutyService;
import com.moveinsync.roster.service.RosterService;
import com.moveinsync.roster.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyAndReliabilityTest {

    @Autowired
    private DriverService driverService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private DutyService dutyService;

    @Autowired
    private RosterService rosterService;

    @Test
    @DisplayName("Scenario 22: Concurrent assignment attempts cannot create an invalid roster or double-booking")
    void testConcurrentRosterGenerationPreventsDoubleBooking() throws Exception {
        // Create 1 HMV Driver and 1 BUS
        DriverRequest dReq = new DriverRequest();
        dReq.setName("Concurrency Driver");
        dReq.setPhoneNumber("9876543210");
        dReq.setLicenceClass("HMV");
        dReq.setLicenceExpiryDate(LocalDate.of(2027, 12, 31));
        dReq.setMedicalValidityDate(LocalDate.of(2027, 12, 31));
        dReq.setTrainingValidityDate(LocalDate.of(2027, 12, 31));
        DriverResponse driver = driverService.createDriver(dReq);

        VehicleRequest vReq = new VehicleRequest();
        vReq.setRegistrationNumber("CBUS-" + (System.currentTimeMillis() % 1000000));
        vReq.setVehicleType("BUS");
        VehicleResponse vehicle = vehicleService.createVehicle(vReq);

        // Create 2 overlapping BUS duties at the EXACT same time
        LocalDateTime start = LocalDateTime.of(2026, 11, 1, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 1, 12, 0);

        DutyRequest duty1Req = new DutyRequest();
        duty1Req.setRequiredVehicleType("BUS");
        duty1Req.setStartTime(start);
        duty1Req.setEndTime(end);
        DutyResponse duty1 = dutyService.createDuty(duty1Req);

        DutyRequest duty2Req = new DutyRequest();
        duty2Req.setRequiredVehicleType("BUS");
        duty2Req.setStartTime(start);
        duty2Req.setEndTime(end);
        DutyResponse duty2 = dutyService.createDuty(duty2Req);

        // Run roster generation concurrently across 2 threads
        int threads = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threads);

        AtomicInteger totalAssigned = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    RosterGenerationResponse res = rosterService.generateRoster(start, end);
                    totalAssigned.addAndGet(res.getAssignedDuties());
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // trigger both threads simultaneously
        finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly 1 duty must be assigned, and 1 MUST remain unfilled! (Since there is only 1 bus and 1 driver)
        DutyResponse d1Updated = dutyService.getDuty(duty1.getId());
        DutyResponse d2Updated = dutyService.getDuty(duty2.getId());

        int assignedCount = 0;
        if ("ASSIGNED".equals(d1Updated.getAssignmentState())) assignedCount++;
        if ("ASSIGNED".equals(d2Updated.getAssignmentState())) assignedCount++;

        assertThat(assignedCount).isEqualTo(1);
    }
}

package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.response.DutyResponse;
import com.moveinsync.roster.exception.InvalidStateTransitionException;
import com.moveinsync.roster.model.entity.AssignmentAudit;
import com.moveinsync.roster.model.entity.Driver;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.entity.Vehicle;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.AuditAction;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.LicenceClass;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.AssignmentAuditRepository;
import com.moveinsync.roster.repository.DutyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DutyStateTransitionTest {

    @Mock
    private DutyRepository dutyRepository;
    @Mock
    private AssignmentAuditRepository auditRepository;

    private DutyService dutyService;

    @BeforeEach
    void setUp() {
        dutyService = new DutyService(dutyRepository, auditRepository);
    }

    @Test
    @DisplayName("Valid transition: ACTIVE to COMPLETED")
    void testActiveToCompletedAllowed() {
        Duty duty = new Duty();
        duty.setId(10L);
        duty.setRequiredVehicleType(VehicleType.BUS);
        duty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        duty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        duty.setDutyStatus(DutyStatus.ACTIVE);
        duty.setAssignmentState(AssignmentState.ASSIGNED);

        when(dutyRepository.findById(10L)).thenReturn(Optional.of(duty));
        when(dutyRepository.save(any(Duty.class))).thenAnswer(i -> i.getArgument(0));

        DutyResponse response = dutyService.updateDutyStatus(10L, "COMPLETED");

        assertThat(response.getDutyStatus()).isEqualTo("COMPLETED");
        verify(dutyRepository).save(duty);
    }

    @Test
    @DisplayName("Valid transition: ACTIVE to CANCELLED unassigns driver/vehicle and audits")
    void testActiveToCancelledUnassignsAndAudits() {
        Driver driver = new Driver();
        driver.setId(1L);
        driver.setName("Assigned Driver");

        Vehicle vehicle = new Vehicle();
        vehicle.setId(2L);
        vehicle.setRegistrationNumber("KA01AB1234");

        Duty duty = new Duty();
        duty.setId(11L);
        duty.setRequiredVehicleType(VehicleType.BUS);
        duty.setStartTime(LocalDateTime.of(2026, 10, 15, 6, 0));
        duty.setEndTime(LocalDateTime.of(2026, 10, 15, 10, 0));
        duty.setDutyStatus(DutyStatus.ACTIVE);
        duty.setAssignmentState(AssignmentState.ASSIGNED);
        duty.setAssignedDriver(driver);
        duty.setAssignedVehicle(vehicle);

        when(dutyRepository.findById(11L)).thenReturn(Optional.of(duty));
        when(dutyRepository.save(any(Duty.class))).thenAnswer(i -> i.getArgument(0));

        DutyResponse response = dutyService.updateDutyStatus(11L, "CANCELLED");

        assertThat(response.getDutyStatus()).isEqualTo("CANCELLED");
        assertThat(duty.getAssignedDriver()).isNull();
        assertThat(duty.getAssignedVehicle()).isNull();
        assertThat(duty.getAssignmentState()).isEqualTo(AssignmentState.UNASSIGNED);

        ArgumentCaptor<AssignmentAudit> captor = ArgumentCaptor.forClass(AssignmentAudit.class);
        verify(auditRepository).save(captor.capture());
        AssignmentAudit audit = captor.getValue();
        assertThat(audit.getAction()).isEqualTo(AuditAction.CANCELLATION);
        assertThat(audit.getPreviousDriverId()).isEqualTo(1L);
        assertThat(audit.getPreviousVehicleId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("Invalid transition: CANCELLED to ACTIVE is rejected")
    void testCancelledToActiveRejected() {
        Duty duty = new Duty();
        duty.setId(12L);
        duty.setDutyStatus(DutyStatus.CANCELLED);

        when(dutyRepository.findById(12L)).thenReturn(Optional.of(duty));

        assertThatThrownBy(() -> dutyService.updateDutyStatus(12L, "ACTIVE"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Only ACTIVE -> COMPLETED and ACTIVE -> CANCELLED are allowed");
    }

    @Test
    @DisplayName("Invalid transition: COMPLETED to CANCELLED is rejected")
    void testCompletedToCancelledRejected() {
        Duty duty = new Duty();
        duty.setId(13L);
        duty.setDutyStatus(DutyStatus.COMPLETED);

        when(dutyRepository.findById(13L)).thenReturn(Optional.of(duty));

        assertThatThrownBy(() -> dutyService.updateDutyStatus(13L, "CANCELLED"))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("Only ACTIVE -> COMPLETED and ACTIVE -> CANCELLED are allowed");
    }
}

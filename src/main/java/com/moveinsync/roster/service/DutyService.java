package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.request.DutyRequest;
import com.moveinsync.roster.dto.request.DutyStatusUpdateRequest;
import com.moveinsync.roster.dto.response.DutyResponse;
import com.moveinsync.roster.dto.response.PageResponse;
import com.moveinsync.roster.exception.BusinessRuleException;
import com.moveinsync.roster.exception.InvalidStateTransitionException;
import com.moveinsync.roster.exception.ResourceNotFoundException;
import com.moveinsync.roster.model.entity.AssignmentAudit;
import com.moveinsync.roster.model.entity.Duty;
import com.moveinsync.roster.model.enums.AssignmentState;
import com.moveinsync.roster.model.enums.AuditAction;
import com.moveinsync.roster.model.enums.DutyStatus;
import com.moveinsync.roster.model.enums.VehicleType;
import com.moveinsync.roster.repository.AssignmentAuditRepository;
import com.moveinsync.roster.repository.DutyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DutyService {

    private static final Logger log = LoggerFactory.getLogger(DutyService.class);

    private final DutyRepository dutyRepository;
    private final AssignmentAuditRepository auditRepository;

    public DutyService(DutyRepository dutyRepository, AssignmentAuditRepository auditRepository) {
        this.dutyRepository = dutyRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public DutyResponse createDuty(DutyRequest request) {
        if (request.getStartTime() == null || request.getEndTime() == null) {
            throw new BusinessRuleException("Duty start time and end time are required");
        }
        if (!request.getStartTime().isBefore(request.getEndTime())) {
            throw new BusinessRuleException("Duty start time must be before end time");
        }

        VehicleType vehicleType;
        try {
            vehicleType = VehicleType.valueOf(request.getRequiredVehicleType().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessRuleException("Invalid vehicle type: " + request.getRequiredVehicleType());
        }

        Duty duty = new Duty();
        duty.setRequiredVehicleType(vehicleType);
        duty.setStartTime(request.getStartTime());
        duty.setEndTime(request.getEndTime());
        duty.setDutyStatus(DutyStatus.ACTIVE);
        duty.setAssignmentState(AssignmentState.UNASSIGNED);
        duty.setAssignedDriver(null);
        duty.setAssignedVehicle(null);

        Duty saved = dutyRepository.save(duty);
        log.info("Created duty: id={}, vehicleType={}, start={}, end={}",
                saved.getId(), saved.getRequiredVehicleType(), saved.getStartTime(), saved.getEndTime());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public DutyResponse getDuty(Long id) {
        Duty duty = dutyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Duty", id));
        return toResponse(duty);
    }

    @Transactional(readOnly = true)
    public PageResponse<DutyResponse> listDuties(int page, int size,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 AssignmentState assignmentState) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startTime").ascending());
        Page<Duty> dutyPage;

        if (startTime != null && endTime != null) {
            if (assignmentState != null) {
                dutyPage = dutyRepository.findByDateRangeAndAssignmentState(startTime, endTime, assignmentState, pageRequest);
            } else {
                dutyPage = dutyRepository.findByDateRange(startTime, endTime, pageRequest);
            }
        } else if (assignmentState != null) {
            dutyPage = dutyRepository.findByAssignmentState(assignmentState, pageRequest);
        } else {
            dutyPage = dutyRepository.findAll(pageRequest);
        }

        List<DutyResponse> content = dutyPage.getContent().stream()
                .map(this::toResponse).toList();
        return new PageResponse<>(content, dutyPage.getNumber(), dutyPage.getSize(),
                dutyPage.getTotalElements(), dutyPage.getTotalPages(), dutyPage.isLast());
    }

    @Transactional(readOnly = true)
    public PageResponse<DutyResponse> listDuties(int page, int size,
                                                 LocalDateTime startTime,
                                                 LocalDateTime endTime,
                                                 String assignmentState) {
        AssignmentState state = null;
        if (assignmentState != null && !assignmentState.isBlank()) {
            try {
                state = AssignmentState.valueOf(assignmentState.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new BusinessRuleException("Invalid assignment state: " + assignmentState);
            }
        }
        return listDuties(page, size, startTime, endTime, state);
    }

    @Transactional
    public DutyResponse updateDutyStatus(Long id, DutyStatusUpdateRequest request) {
        return updateDutyStatus(id, request.getStatus());
    }

    @Transactional
    public DutyResponse updateDutyStatus(Long id, String status) {
        Duty duty = dutyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Duty", id));

        DutyStatus targetStatus;
        try {
            targetStatus = DutyStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidStateTransitionException("Invalid duty status: " + status);
        }

        DutyStatus currentStatus = duty.getDutyStatus();
        if (currentStatus != DutyStatus.ACTIVE || (targetStatus != DutyStatus.COMPLETED && targetStatus != DutyStatus.CANCELLED)) {
            throw new InvalidStateTransitionException(
                    "Invalid duty status transition from " + currentStatus + " to " + targetStatus
                            + ". Only ACTIVE -> COMPLETED and ACTIVE -> CANCELLED are allowed.");
        }

        if (targetStatus == DutyStatus.CANCELLED) {
            boolean wasAssigned = duty.getAssignmentState() == AssignmentState.ASSIGNED
                    || duty.getAssignedDriver() != null
                    || duty.getAssignedVehicle() != null;

            if (wasAssigned) {
                Long prevDriverId = duty.getAssignedDriver() != null ? duty.getAssignedDriver().getId() : null;
                Long prevVehicleId = duty.getAssignedVehicle() != null ? duty.getAssignedVehicle().getId() : null;

                duty.setAssignedDriver(null);
                duty.setAssignedVehicle(null);
                duty.setAssignmentState(AssignmentState.UNASSIGNED);

                AssignmentAudit audit = new AssignmentAudit();
                audit.setDuty(duty);
                audit.setPreviousDriverId(prevDriverId);
                audit.setPreviousVehicleId(prevVehicleId);
                audit.setNewDriverId(null);
                audit.setNewVehicleId(null);
                audit.setAction(AuditAction.CANCELLATION);
                audit.setReason("Duty cancelled");
                audit.setActingUser(getActingUser());
                auditRepository.save(audit);
            }
        }

        duty.setDutyStatus(targetStatus);
        Duty saved = dutyRepository.save(duty);
        log.info("Duty id={} status updated from {} to {}", id, currentStatus, targetStatus);
        return toResponse(saved);
    }

    private String getActingUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getName() != null && !auth.getName().isBlank()) {
                return auth.getName();
            }
        } catch (Exception ignored) {
        }
        return "system";
    }

    private DutyResponse toResponse(Duty duty) {
        DutyResponse response = new DutyResponse();
        response.setId(duty.getId());
        response.setRequiredVehicleType(duty.getRequiredVehicleType() != null ? duty.getRequiredVehicleType().name() : null);
        response.setStartTime(duty.getStartTime());
        response.setEndTime(duty.getEndTime());
        if (duty.getAssignedDriver() != null) {
            response.setAssignedDriverId(duty.getAssignedDriver().getId());
            response.setAssignedDriverName(duty.getAssignedDriver().getName());
        }
        if (duty.getAssignedVehicle() != null) {
            response.setAssignedVehicleId(duty.getAssignedVehicle().getId());
            response.setAssignedVehicleRegistration(duty.getAssignedVehicle().getRegistrationNumber());
        }
        response.setDutyStatus(duty.getDutyStatus() != null ? duty.getDutyStatus().name() : null);
        response.setAssignmentState(duty.getAssignmentState() != null ? duty.getAssignmentState().name() : null);
        response.setUnfilledReason(duty.getUnfilledReason());
        response.setCreatedAt(duty.getCreatedAt());
        response.setUpdatedAt(duty.getUpdatedAt());
        return response;
    }
}

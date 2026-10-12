package com.moveinsync.roster.repository;

import com.moveinsync.roster.model.entity.AssignmentAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentAuditRepository extends JpaRepository<AssignmentAudit, Long> {

    List<AssignmentAudit> findByDutyIdOrderByCreatedAtDesc(Long dutyId);

    Page<AssignmentAudit> findByDutyId(Long dutyId, Pageable pageable);
}

package org.lab1.repository;

import org.lab1.model.entity.JoinRequest;
import org.lab1.model.enums.JoinRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JoinRequestRepository extends JpaRepository<JoinRequest, Long> {

    Page<JoinRequest> findAllByProjectId(Long projectId, Pageable pageable);

    Page<JoinRequest> findAllByApplicantId(Long applicantId, Pageable pageable);

    boolean existsByProjectIdAndApplicantIdAndStatus(Long projectId, Long applicantId, JoinRequestStatus status);
}

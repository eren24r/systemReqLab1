package org.lab1.repository;

import org.lab1.model.entity.TeamMembership;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeamMembershipRepository extends JpaRepository<TeamMembership, Long> {

    @EntityGraph(attributePaths = "member")
    Slice<TeamMembership> findAllByProjectId(Long projectId, Pageable pageable);

    Optional<TeamMembership> findByProjectIdAndMemberId(Long projectId, Long memberId);

    boolean existsByProjectIdAndMemberId(Long projectId, Long memberId);
}

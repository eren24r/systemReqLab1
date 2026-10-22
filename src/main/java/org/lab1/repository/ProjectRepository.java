package org.lab1.repository;

import org.lab1.model.entity.Project;
import org.lab1.model.enums.ProjectStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @EntityGraph(attributePaths = "owner")
    Slice<Project> findAllBy(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Slice<Project> findAllByStatus(ProjectStatus status, Pageable pageable);
}

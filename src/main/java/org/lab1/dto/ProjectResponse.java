package org.lab1.dto;

import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ProjectResponse(
        Long id,
        String title,
        String description,
        ProjectStage stage,
        ProjectStatus status,
        BigDecimal fundingGoal,
        BigDecimal raisedAmount,
        Long ownerId,
        String ownerName,
        Instant createdAt,
        List<SkillResponse> requiredSkills
) {
}

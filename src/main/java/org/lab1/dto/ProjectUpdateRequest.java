package org.lab1.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;

import java.math.BigDecimal;
import java.util.Set;

public record ProjectUpdateRequest(
        @NotBlank @Size(min = 3, max = 150) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotNull ProjectStage stage,
        @NotNull ProjectStatus status,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal fundingGoal,
        Set<Long> requiredSkillIds
) {
}

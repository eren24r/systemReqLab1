package org.lab1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lab1.model.enums.SkillCategory;

public record SkillRequest(
        @NotBlank @Size(min = 2, max = 64) String name,
        @NotNull SkillCategory category
) {
}

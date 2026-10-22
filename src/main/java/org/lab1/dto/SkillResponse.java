package org.lab1.dto;

import org.lab1.model.enums.SkillCategory;

public record SkillResponse(
        Long id,
        String name,
        SkillCategory category
) {
}

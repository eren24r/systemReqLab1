package org.lab1.dto;

import org.lab1.model.enums.UserRole;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        UserRole role,
        String bio,
        Instant createdAt,
        List<SkillResponse> skills
) {
}

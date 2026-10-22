package org.lab1.dto;

import org.lab1.model.enums.TeamRole;

import java.math.BigDecimal;
import java.time.Instant;

public record TeamMemberResponse(
        Long id,
        Long projectId,
        Long userId,
        String fullName,
        TeamRole role,
        BigDecimal equityShare,
        Instant joinedAt
) {
}

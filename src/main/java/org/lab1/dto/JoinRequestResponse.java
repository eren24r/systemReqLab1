package org.lab1.dto;

import org.lab1.model.enums.JoinRequestStatus;
import org.lab1.model.enums.TeamRole;

import java.time.Instant;

public record JoinRequestResponse(
        Long id,
        Long projectId,
        String projectTitle,
        Long applicantId,
        String applicantName,
        TeamRole desiredRole,
        String message,
        JoinRequestStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}

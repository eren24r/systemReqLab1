package org.lab1.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lab1.model.enums.TeamRole;

public record JoinRequestCreateRequest(
        @NotNull Long projectId,
        @NotNull Long applicantId,
        @NotNull TeamRole desiredRole,
        @Size(max = 2000) String message
) {
}

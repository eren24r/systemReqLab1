package org.lab1.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.lab1.model.enums.TeamRole;

import java.math.BigDecimal;

public record TeamMemberUpdateRequest(
        @NotNull TeamRole role,
        @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal equityShare
) {
}

package org.lab1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.lab1.model.enums.UserRole;

public record UserUpdateRequest(
        @NotBlank @Size(min = 2, max = 100) String fullName,
        @NotNull UserRole role,
        @Size(max = 2000) String bio
) {
}

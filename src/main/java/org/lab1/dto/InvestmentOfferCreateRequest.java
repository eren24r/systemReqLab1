package org.lab1.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record InvestmentOfferCreateRequest(
        @NotNull Long projectId,
        @NotNull Long investorId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @DecimalMin("0.01") @DecimalMax("100.00") BigDecimal equityPercent,
        @Size(max = 2000) String message
) {
}

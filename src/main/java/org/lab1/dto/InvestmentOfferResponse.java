package org.lab1.dto;

import org.lab1.model.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record InvestmentOfferResponse(
        Long id,
        Long projectId,
        String projectTitle,
        Long investorId,
        String investorName,
        BigDecimal amount,
        BigDecimal equityPercent,
        String message,
        OfferStatus status,
        Instant createdAt,
        Instant resolvedAt
) {
}

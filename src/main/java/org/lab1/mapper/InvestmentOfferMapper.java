package org.lab1.mapper;

import org.lab1.dto.InvestmentOfferResponse;
import org.lab1.model.entity.InvestmentOffer;
import org.springframework.stereotype.Component;

@Component
public class InvestmentOfferMapper {

    public InvestmentOfferResponse toResponse(InvestmentOffer offer) {
        return new InvestmentOfferResponse(
                offer.getId(),
                offer.getProject().getId(),
                offer.getProject().getTitle(),
                offer.getInvestor().getId(),
                offer.getInvestor().getFullName(),
                offer.getAmount(),
                offer.getEquityPercent(),
                offer.getMessage(),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getResolvedAt()
        );
    }
}

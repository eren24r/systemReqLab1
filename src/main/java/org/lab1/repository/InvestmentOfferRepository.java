package org.lab1.repository;

import org.lab1.model.entity.InvestmentOffer;
import org.lab1.model.enums.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentOfferRepository extends JpaRepository<InvestmentOffer, Long> {

    Page<InvestmentOffer> findAllByProjectId(Long projectId, Pageable pageable);

    Page<InvestmentOffer> findAllByInvestorId(Long investorId, Pageable pageable);

    List<InvestmentOffer> findAllByProjectIdAndStatus(Long projectId, OfferStatus status);

    boolean existsByProjectIdAndInvestorIdAndStatus(Long projectId, Long investorId, OfferStatus status);
}

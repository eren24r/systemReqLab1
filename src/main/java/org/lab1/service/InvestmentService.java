package org.lab1.service;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.InvestmentOfferCreateRequest;
import org.lab1.dto.InvestmentOfferResponse;
import org.lab1.exception.BusinessRuleException;
import org.lab1.exception.ConflictException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.InvestmentOfferMapper;
import org.lab1.model.entity.InvestmentOffer;
import org.lab1.model.entity.Project;
import org.lab1.model.entity.User;
import org.lab1.model.enums.OfferStatus;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.UserRole;
import org.lab1.repository.InvestmentOfferRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InvestmentService {

    private final InvestmentOfferRepository offerRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final InvestmentOfferMapper offerMapper;

    @Transactional
    public InvestmentOfferResponse createOffer(InvestmentOfferCreateRequest request) {
        Project project = projectService.getEntity(request.projectId());
        User investor = userService.getEntity(request.investorId());
        if (investor.getRole() != UserRole.INVESTOR) {
            throw new BusinessRuleException("Only users with the INVESTOR role can make investment offers");
        }
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new BusinessRuleException("Investment offers are accepted only for published projects");
        }
        if (offerRepository.existsByProjectIdAndInvestorIdAndStatus(project.getId(), investor.getId(), OfferStatus.PENDING)) {
            throw new ConflictException("Investor %d already has a pending offer for project %d".formatted(investor.getId(), project.getId()));
        }
        InvestmentOffer offer = new InvestmentOffer();
        offer.setProject(project);
        offer.setInvestor(investor);
        offer.setAmount(request.amount());
        offer.setEquityPercent(request.equityPercent());
        offer.setMessage(request.message());
        return offerMapper.toResponse(offerRepository.save(offer));
    }

    @Transactional(readOnly = true)
    public InvestmentOfferResponse getById(Long id) {
        return offerMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<InvestmentOfferResponse> findAll(Long projectId, Long investorId, Pageable pageable) {
        Page<InvestmentOffer> page;
        if (projectId != null) {
            page = offerRepository.findAllByProjectId(projectId, pageable);
        } else if (investorId != null) {
            page = offerRepository.findAllByInvestorId(investorId, pageable);
        } else {
            page = offerRepository.findAll(pageable);
        }
        return page.map(offerMapper::toResponse);
    }

    @Transactional
    public InvestmentOfferResponse accept(Long id) {
        InvestmentOffer offer = getEntity(id);
        ensurePending(offer);
        Project project = offer.getProject();
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new BusinessRuleException("Offers can be accepted only for published projects");
        }
        Instant now = Instant.now();
        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setResolvedAt(now);
        project.setRaisedAmount(project.getRaisedAmount().add(offer.getAmount()));
        if (project.getRaisedAmount().compareTo(project.getFundingGoal()) >= 0) {
            project.setStatus(ProjectStatus.FUNDED);
            offerRepository.findAllByProjectIdAndStatus(project.getId(), OfferStatus.PENDING).forEach(pending -> {
                pending.setStatus(OfferStatus.REJECTED);
                pending.setResolvedAt(now);
            });
        }
        return offerMapper.toResponse(offer);
    }

    @Transactional
    public InvestmentOfferResponse reject(Long id) {
        return resolve(id, OfferStatus.REJECTED);
    }

    @Transactional
    public InvestmentOfferResponse withdraw(Long id) {
        return resolve(id, OfferStatus.WITHDRAWN);
    }

    private InvestmentOfferResponse resolve(Long id, OfferStatus status) {
        InvestmentOffer offer = getEntity(id);
        ensurePending(offer);
        offer.setStatus(status);
        offer.setResolvedAt(Instant.now());
        return offerMapper.toResponse(offer);
    }

    private InvestmentOffer getEntity(Long id) {
        return offerRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Investment offer", id));
    }

    private void ensurePending(InvestmentOffer offer) {
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new ConflictException("Investment offer %d is already %s".formatted(offer.getId(), offer.getStatus()));
        }
    }
}

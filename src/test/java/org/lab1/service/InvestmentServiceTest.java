package org.lab1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvestmentServiceTest {

    @Mock
    private InvestmentOfferRepository offerRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private final InvestmentOfferMapper offerMapper = new InvestmentOfferMapper();

    private InvestmentService investmentService;

    private Project project;
    private User investor;

    @BeforeEach
    void setUp() {
        investmentService = new InvestmentService(offerRepository, projectService, userService, offerMapper);
        User owner = user(1L, UserRole.STUDENT);
        investor = user(2L, UserRole.INVESTOR);
        project = new Project();
        project.setId(10L);
        project.setTitle("Project");
        project.setOwner(owner);
        project.setStatus(ProjectStatus.PUBLISHED);
        project.setFundingGoal(new BigDecimal("1000.00"));
    }

    @Test
    void createOffer_byNonInvestor_throwsBusinessRule() {
        User student = user(3L, UserRole.STUDENT);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(3L)).thenReturn(student);

        assertThatThrownBy(() -> investmentService.createOffer(request(10L, 3L)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("INVESTOR");
        verify(offerRepository, never()).save(any());
    }

    @Test
    void createOffer_forDraftProject_throwsBusinessRule() {
        project.setStatus(ProjectStatus.DRAFT);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(2L)).thenReturn(investor);

        assertThatThrownBy(() -> investmentService.createOffer(request(10L, 2L)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void createOffer_withPendingDuplicate_throwsConflict() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(2L)).thenReturn(investor);
        when(offerRepository.existsByProjectIdAndInvestorIdAndStatus(10L, 2L, OfferStatus.PENDING)).thenReturn(true);

        assertThatThrownBy(() -> investmentService.createOffer(request(10L, 2L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOffer_savesPendingOffer() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(2L)).thenReturn(investor);
        when(offerRepository.existsByProjectIdAndInvestorIdAndStatus(10L, 2L, OfferStatus.PENDING)).thenReturn(false);
        when(offerRepository.save(any(InvestmentOffer.class))).thenAnswer(invocation -> {
            InvestmentOffer saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        InvestmentOfferResponse response = investmentService.createOffer(request(10L, 2L));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.status()).isEqualTo(OfferStatus.PENDING);
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        assertThat(response.investorName()).isEqualTo("User 2");
    }

    @Test
    void accept_partialAmount_keepsProjectPublished() {
        InvestmentOffer offer = offer(100L, "400.00");
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        InvestmentOfferResponse response = investmentService.accept(100L);

        assertThat(response.status()).isEqualTo(OfferStatus.ACCEPTED);
        assertThat(offer.getResolvedAt()).isNotNull();
        assertThat(project.getRaisedAmount()).isEqualByComparingTo("400.00");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PUBLISHED);
        verify(offerRepository, never()).findAllByProjectIdAndStatus(any(), any());
    }

    @Test
    void accept_reachingGoal_fundsProjectAndRejectsPendingOffers() {
        project.setRaisedAmount(new BigDecimal("700.00"));
        InvestmentOffer offer = offer(100L, "300.00");
        InvestmentOffer pending = offer(101L, "50.00");
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));
        when(offerRepository.findAllByProjectIdAndStatus(10L, OfferStatus.PENDING)).thenReturn(List.of(pending));

        investmentService.accept(100L);

        assertThat(project.getRaisedAmount()).isEqualByComparingTo("1000.00");
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.FUNDED);
        assertThat(pending.getStatus()).isEqualTo(OfferStatus.REJECTED);
        assertThat(pending.getResolvedAt()).isNotNull();
    }

    @Test
    void accept_alreadyResolved_throwsConflict() {
        InvestmentOffer offer = offer(100L, "100.00");
        offer.setStatus(OfferStatus.WITHDRAWN);
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> investmentService.accept(100L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("WITHDRAWN");
    }

    @Test
    void accept_forUnpublishedProject_throwsBusinessRule() {
        project.setStatus(ProjectStatus.ARCHIVED);
        InvestmentOffer offer = offer(100L, "100.00");
        when(offerRepository.findById(100L)).thenReturn(Optional.of(offer));

        assertThatThrownBy(() -> investmentService.accept(100L))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(offer.getStatus()).isEqualTo(OfferStatus.PENDING);
    }

    @Test
    void rejectAndWithdraw_setStatuses() {
        InvestmentOffer first = offer(100L, "100.00");
        InvestmentOffer second = offer(101L, "100.00");
        when(offerRepository.findById(100L)).thenReturn(Optional.of(first));
        when(offerRepository.findById(101L)).thenReturn(Optional.of(second));

        assertThat(investmentService.reject(100L).status()).isEqualTo(OfferStatus.REJECTED);
        assertThat(investmentService.withdraw(101L).status()).isEqualTo(OfferStatus.WITHDRAWN);
        assertThat(project.getRaisedAmount()).isEqualByComparingTo("0");
    }

    @Test
    void getById_unknown_throwsNotFound() {
        when(offerRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> investmentService.getById(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Investment offer with id 5 was not found");
    }

    private InvestmentOffer offer(Long id, String amount) {
        InvestmentOffer offer = new InvestmentOffer();
        offer.setId(id);
        offer.setProject(project);
        offer.setInvestor(investor);
        offer.setAmount(new BigDecimal(amount));
        offer.setEquityPercent(new BigDecimal("5.00"));
        return offer;
    }

    private static InvestmentOfferCreateRequest request(Long projectId, Long investorId) {
        return new InvestmentOfferCreateRequest(projectId, investorId, new BigDecimal("250.00"), new BigDecimal("5.00"), "hi");
    }

    private static User user(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setEmail("user" + id + "@example.com");
        user.setFullName("User " + id);
        user.setRole(role);
        return user;
    }
}

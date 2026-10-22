package org.lab1.controller;

import org.junit.jupiter.api.Test;
import org.lab1.AbstractIntegrationTest;
import org.lab1.dto.InvestmentOfferCreateRequest;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.UserRole;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InvestmentFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createOffer_businessRules_areEnforced() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long draftProject = createProject(ownerId, Set.of());
        long publishedProject = createPublishedProject(ownerId);
        long studentId = createUser(UserRole.STUDENT);
        long investorId = createUser(UserRole.INVESTOR);

        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(publishedProject, studentId, BigDecimal.TEN, BigDecimal.ONE, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("INVESTOR")));

        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(draftProject, investorId, BigDecimal.TEN, BigDecimal.ONE, null))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(publishedProject, investorId, BigDecimal.ZERO, new BigDecimal("101"), null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.amount").exists())
                .andExpect(jsonPath("$.details.equityPercent").exists());

        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(9999L, investorId, BigDecimal.TEN, BigDecimal.ONE, null))))
                .andExpect(status().isNotFound());

        createOffer(publishedProject, investorId, "100.00");
        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(publishedProject, investorId, BigDecimal.TEN, BigDecimal.ONE, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("pending offer")));
    }

    @Test
    void acceptOffer_increasesRaisedAmountAndFundsProjectWhenGoalReached() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long investor1 = createUser(UserRole.INVESTOR);
        long investor2 = createUser(UserRole.INVESTOR);
        long investor3 = createUser(UserRole.INVESTOR);
        long offer1 = createOffer(projectId, investor1, "400.00");
        long offer2 = createOffer(projectId, investor2, "600.00");
        long offer3 = createOffer(projectId, investor3, "50.00");

        mockMvc.perform(post("/api/investment-offers/{id}/accept", offer1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(jsonPath("$.raisedAmount").value(400.0))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/investment-offers/{id}/accept", offer1))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/investment-offers/{id}/accept", offer2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(jsonPath("$.raisedAmount").value(1000.0))
                .andExpect(jsonPath("$.status").value("FUNDED"));

        mockMvc.perform(get("/api/investment-offers/{id}", offer3))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/investment-offers").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new InvestmentOfferCreateRequest(projectId, investor3, BigDecimal.TEN, BigDecimal.ONE, null))))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void rejectAndWithdraw_changeStatusOnlyOnce() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long investor1 = createUser(UserRole.INVESTOR);
        long investor2 = createUser(UserRole.INVESTOR);
        long rejected = createOffer(projectId, investor1, "100.00");
        long withdrawn = createOffer(projectId, investor2, "100.00");

        mockMvc.perform(post("/api/investment-offers/{id}/reject", rejected))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post("/api/investment-offers/{id}/withdraw", withdrawn))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WITHDRAWN"));

        mockMvc.perform(post("/api/investment-offers/{id}/withdraw", rejected))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/investment-offers/{id}/reject", 31337))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{id}", projectId))
                .andExpect(jsonPath("$.raisedAmount").value(0));
    }

    @Test
    void updateProject_cannotLowerGoalBelowRaisedAmount() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long investorId = createUser(UserRole.INVESTOR);
        long offerId = createOffer(projectId, investorId, "500.00");
        mockMvc.perform(post("/api/investment-offers/{id}/accept", offerId)).andExpect(status().isOk());

        mockMvc.perform(put("/api/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new ProjectUpdateRequest("Title", "Description",
                                ProjectStage.MVP, ProjectStatus.PUBLISHED, new BigDecimal("300.00"), null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("raised amount")));
    }

    @Test
    void findOffers_supportsFiltersAndTotalCountHeader() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectA = createPublishedProject(ownerId);
        long projectB = createPublishedProject(ownerId);
        long investor1 = createUser(UserRole.INVESTOR);
        long investor2 = createUser(UserRole.INVESTOR);
        createOffer(projectA, investor1, "10.00");
        createOffer(projectA, investor2, "20.00");
        createOffer(projectB, investor1, "30.00");

        mockMvc.perform(get("/api/investment-offers").param("projectId", String.valueOf(projectA)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].projectId").value(projectA));

        mockMvc.perform(get("/api/investment-offers").param("investorId", String.valueOf(investor1)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$[0].investorId").value(investor1));

        mockMvc.perform(get("/api/investment-offers").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(header().string("X-Total-Pages", "3"))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/investment-offers").param("size", "51"))
                .andExpect(status().isBadRequest());
    }
}

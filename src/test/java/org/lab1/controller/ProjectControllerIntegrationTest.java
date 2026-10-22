package org.lab1.controller;

import org.junit.jupiter.api.Test;
import org.lab1.AbstractIntegrationTest;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.UserRole;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createProject_createsFounderMembershipAndRequiredSkills() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long skillA = createSkill("Skill A " + System.nanoTime());
        long skillB = createSkill("Skill B " + System.nanoTime());
        ProjectCreateRequest request = new ProjectCreateRequest(
                "  Smart Campus  ", "IoT platform", ProjectStage.PROTOTYPE, new BigDecimal("5000.00"), ownerId, Set.of(skillA, skillB));

        String response = mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Smart Campus"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.stage").value("PROTOTYPE"))
                .andExpect(jsonPath("$.raisedAmount").value(0))
                .andExpect(jsonPath("$.ownerId").value(ownerId))
                .andExpect(jsonPath("$.ownerName").value("Test User"))
                .andExpect(jsonPath("$.requiredSkills", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        long projectId = objectMapper.readTree(response).get("id").asLong();

        mockMvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].userId").value(ownerId))
                .andExpect(jsonPath("$.content[0].role").value("FOUNDER"))
                .andExpect(jsonPath("$.content[0].equityShare").value(100.0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void createProject_withInvestorOwner_returnsUnprocessableEntity() throws Exception {
        long investorId = createUser(UserRole.INVESTOR);
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Title", "Description", ProjectStage.IDEA, DEFAULT_GOAL, investorId, null);

        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Investors cannot own projects")));
    }

    @Test
    void createProject_withUnknownOwnerOrSkill_returnsNotFound() throws Exception {
        ProjectCreateRequest unknownOwner = new ProjectCreateRequest(
                "Title", "Description", ProjectStage.IDEA, DEFAULT_GOAL, 12345L, null);
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(json(unknownOwner)))
                .andExpect(status().isNotFound());

        long ownerId = createUser(UserRole.STUDENT);
        ProjectCreateRequest unknownSkill = new ProjectCreateRequest(
                "Title", "Description", ProjectStage.IDEA, DEFAULT_GOAL, ownerId, Set.of(98765L));
        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(json(unknownSkill)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Skill with id 98765 was not found"));
    }

    @Test
    void createProject_invalidBody_returnsBadRequest() throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "ab", "", null, new BigDecimal("-1"), null, null);

        mockMvc.perform(post("/api/projects").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.title").exists())
                .andExpect(jsonPath("$.details.description").exists())
                .andExpect(jsonPath("$.details.stage").exists())
                .andExpect(jsonPath("$.details.fundingGoal").exists())
                .andExpect(jsonPath("$.details.ownerId").exists());
    }

    @Test
    void feed_usesInfiniteScrollWithoutTotalCount() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        for (int i = 0; i < 3; i++) {
            createProject(ownerId, Set.of());
        }

        mockMvc.perform(get("/api/projects").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("X-Total-Count"))
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true));

        mockMvc.perform(get("/api/projects").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.hasNext").value(false));

        mockMvc.perform(get("/api/projects").param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void feed_filtersByStatus() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        createProject(ownerId, Set.of());
        long publishedId = createPublishedProject(ownerId);

        mockMvc.perform(get("/api/projects").param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(publishedId));

        mockMvc.perform(get("/api/projects").param("status", "WRONG"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProject_changesStatusAndRejectsManualFunded() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createProject(ownerId, Set.of());
        ProjectUpdateRequest publish = new ProjectUpdateRequest(
                "Published title", "New description", ProjectStage.MVP, ProjectStatus.PUBLISHED, new BigDecimal("2000.00"), null);

        mockMvc.perform(put("/api/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON).content(json(publish)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Published title"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.stage").value("MVP"))
                .andExpect(jsonPath("$.fundingGoal").value(2000.0));

        ProjectUpdateRequest funded = new ProjectUpdateRequest(
                "Published title", "New description", ProjectStage.MVP, ProjectStatus.FUNDED, new BigDecimal("2000.00"), null);
        mockMvc.perform(put("/api/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON).content(json(funded)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(put("/api/projects/{id}", 4242).contentType(MediaType.APPLICATION_JSON).content(json(publish)))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteProject_removesProjectAndTeam() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long investorId = createUser(UserRole.INVESTOR);
        createOffer(projectId, investorId, "100.00");

        mockMvc.perform(delete("/api/projects/{id}", projectId)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/projects/{id}", projectId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/projects/{id}/members", projectId)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/investment-offers").param("projectId", String.valueOf(projectId)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "0"));
    }
}

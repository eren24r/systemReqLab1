package org.lab1.controller;

import org.junit.jupiter.api.Test;
import org.lab1.AbstractIntegrationTest;
import org.lab1.dto.JoinRequestCreateRequest;
import org.lab1.dto.TeamMemberUpdateRequest;
import org.lab1.model.enums.TeamRole;
import org.lab1.model.enums.UserRole;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TeamFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void joinRequest_onDraftProject_returnsUnprocessableEntity() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createProject(ownerId, Set.of());
        long applicantId = createUser(UserRole.STUDENT);

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, applicantId, TeamRole.DEVELOPER, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("published")));
    }

    @Test
    void joinRequest_businessRules_areEnforced() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long investorId = createUser(UserRole.INVESTOR);
        long applicantId = createUser(UserRole.STUDENT);

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, investorId, TeamRole.DEVELOPER, null))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, applicantId, TeamRole.FOUNDER, null))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, ownerId, TeamRole.DEVELOPER, null))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, 777L, TeamRole.DEVELOPER, null))))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(null, null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptJoinRequest_addsMemberAndClosesRequest() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long applicantId = createUser(UserRole.STUDENT);
        long requestId = createJoinRequest(projectId, applicantId);

        mockMvc.perform(get("/api/join-requests/{id}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.applicantId").value(applicantId))
                .andExpect(jsonPath("$.projectId").value(projectId));

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, applicantId, TeamRole.DESIGNER, null))))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/join-requests/{id}/accept", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());

        mockMvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[1].userId").value(applicantId))
                .andExpect(jsonPath("$.content[1].role").value("DEVELOPER"));

        mockMvc.perform(post("/api/join-requests/{id}/accept", requestId))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/join-requests/{id}/reject", requestId))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/join-requests").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new JoinRequestCreateRequest(projectId, applicantId, TeamRole.DESIGNER, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("already a member")));
    }

    @Test
    void rejectJoinRequest_keepsTeamUnchanged() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long applicantId = createUser(UserRole.STUDENT);
        long requestId = createJoinRequest(projectId, applicantId);

        mockMvc.perform(post("/api/join-requests/{id}/reject", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(jsonPath("$.content", hasSize(1)));

        mockMvc.perform(post("/api/join-requests/{id}/accept", 4040))
                .andExpect(status().isNotFound());
    }

    @Test
    void findJoinRequests_supportsFiltersAndTotalCountHeader() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectA = createPublishedProject(ownerId);
        long projectB = createPublishedProject(ownerId);
        long applicant1 = createUser(UserRole.STUDENT);
        long applicant2 = createUser(UserRole.STUDENT);
        createJoinRequest(projectA, applicant1);
        createJoinRequest(projectA, applicant2);
        createJoinRequest(projectB, applicant1);

        mockMvc.perform(get("/api/join-requests").param("projectId", String.valueOf(projectA)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/join-requests").param("applicantId", String.valueOf(applicant1)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "2"));

        mockMvc.perform(get("/api/join-requests").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/join-requests").param("size", "51"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void teamMembers_canBeUpdatedAndRemovedWithOwnerProtection() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        long projectId = createPublishedProject(ownerId);
        long memberId = createUser(UserRole.STUDENT);
        long requestId = createJoinRequest(projectId, memberId);
        mockMvc.perform(post("/api/join-requests/{id}/accept", requestId)).andExpect(status().isOk());

        mockMvc.perform(patch("/api/projects/{id}/members/{userId}", projectId, memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TeamMemberUpdateRequest(TeamRole.DESIGNER, new BigDecimal("15.50")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DESIGNER"))
                .andExpect(jsonPath("$.equityShare").value(15.5));

        mockMvc.perform(patch("/api/projects/{id}/members/{userId}", projectId, memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TeamMemberUpdateRequest(TeamRole.FOUNDER, BigDecimal.TEN))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/projects/{id}/members/{userId}", projectId, ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TeamMemberUpdateRequest(TeamRole.DEVELOPER, BigDecimal.TEN))))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch("/api/projects/{id}/members/{userId}", projectId, memberId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(new TeamMemberUpdateRequest(TeamRole.DESIGNER, new BigDecimal("150")))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, ownerId))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, memberId))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/projects/{id}/members/{userId}", projectId, memberId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{id}/members", projectId))
                .andExpect(jsonPath("$.content", hasSize(1)));
    }
}

package org.lab1.controller;

import org.junit.jupiter.api.Test;
import org.lab1.AbstractIntegrationTest;
import org.lab1.dto.UserCreateRequest;
import org.lab1.dto.UserUpdateRequest;
import org.lab1.model.enums.UserRole;
import org.springframework.http.MediaType;

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

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createUser_returnsCreatedWithNormalizedEmail() throws Exception {
        UserCreateRequest request = new UserCreateRequest("Alice@Example.com", "Alice Smith", UserRole.STUDENT, "Java dev");

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.fullName").value("Alice Smith"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.skills").isEmpty());
    }

    @Test
    void createUser_duplicateEmail_returnsConflict() throws Exception {
        createUser("dup@example.com", UserRole.STUDENT);

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UserCreateRequest("DUP@example.com", "Other", UserRole.INVESTOR, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    void createUser_invalidBody_returnsBadRequestWithFieldDetails() throws Exception {
        String body = "{\"email\":\"not-an-email\",\"fullName\":\"\",\"role\":null}";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details.email").exists())
                .andExpect(jsonPath("$.details.fullName").exists())
                .andExpect(jsonPath("$.details.role").exists());
    }

    @Test
    void createUser_malformedJson_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Malformed request body")));
    }

    @Test
    void createUser_unknownEnumValue_returnsBadRequest() throws Exception {
        String body = "{\"email\":\"a@b.com\",\"fullName\":\"Name\",\"role\":\"BOSS\"}";

        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUser_unknownId_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 9999 was not found"))
                .andExpect(jsonPath("$.path").value("/api/users/9999"));
    }

    @Test
    void getUser_invalidIdType_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("invalid value")));
    }

    @Test
    void unknownEndpoint_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_returnsPageWithTotalCountHeader() throws Exception {
        for (int i = 0; i < 3; i++) {
            createUser(UserRole.STUDENT);
        }

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Total-Count", "3"))
                .andExpect(header().string("X-Total-Pages", "2"))
                .andExpect(jsonPath("$", hasSize(2)));

        mockMvc.perform(get("/api/users").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void findAll_pageSizeAboveLimit_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/users").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.size").exists());

        mockMvc.perform(get("/api/users").param("size", "0"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/users").param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateUser_changesFields() throws Exception {
        long id = createUser(UserRole.STUDENT);

        mockMvc.perform(put("/api/users/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new UserUpdateRequest("New Name", UserRole.ADMIN, "bio"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("New Name"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.bio").value("bio"));
    }

    @Test
    void deleteUser_thenGetReturnsNotFound() throws Exception {
        long id = createUser(UserRole.STUDENT);

        mockMvc.perform(delete("/api/users/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/users/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void deleteUser_owningProject_returnsConflict() throws Exception {
        long ownerId = createUser(UserRole.STUDENT);
        createProject(ownerId, Set.of());

        mockMvc.perform(delete("/api/users/{id}", ownerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("data integrity")));
    }

    @Test
    void addAndRemoveSkill_managesManyToManyRelation() throws Exception {
        long userId = createUser(UserRole.STUDENT);
        long skillId = createSkill("Kotlin " + userId);

        mockMvc.perform(post("/api/users/{id}/skills/{skillId}", userId, skillId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills", hasSize(1)))
                .andExpect(jsonPath("$.skills[0].id").value(skillId));

        mockMvc.perform(post("/api/users/{id}/skills/{skillId}", userId, skillId))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/users/{id}/skills/{skillId}", userId, 9999))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/users/{id}/skills/{skillId}", userId, skillId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skills").isEmpty());

        mockMvc.perform(delete("/api/users/{id}/skills/{skillId}", userId, skillId))
                .andExpect(status().isNotFound());
    }
}

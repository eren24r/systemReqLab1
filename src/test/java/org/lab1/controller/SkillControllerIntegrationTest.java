package org.lab1.controller;

import org.junit.jupiter.api.Test;
import org.lab1.AbstractIntegrationTest;
import org.lab1.dto.SkillRequest;
import org.lab1.model.enums.SkillCategory;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SkillControllerIntegrationTest extends AbstractIntegrationTest {

    @Test
    void createSkill_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillRequest("  Rust  ", SkillCategory.DEVELOPMENT))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Rust"))
                .andExpect(jsonPath("$.category").value("DEVELOPMENT"));
    }

    @Test
    void createSkill_duplicateNameIgnoringCase_returnsConflict() throws Exception {
        mockMvc.perform(post("/api/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillRequest("java", SkillCategory.DEVELOPMENT))))
                .andExpect(status().isConflict());
    }

    @Test
    void createSkill_invalidBody_returnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/skills").contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillRequest("x", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.category").exists());
    }

    @Test
    void getSkill_returnsSeededSkill() throws Exception {
        long id = createSkill("Go " + System.nanoTime());

        mockMvc.perform(get("/api/skills/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));

        mockMvc.perform(get("/api/skills/{id}", 99999))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAll_returnsSortedPageWithTotalCountHeader() throws Exception {
        mockMvc.perform(get("/api/skills").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Total-Count"))
                .andExpect(header().exists("X-Total-Pages"))
                .andExpect(jsonPath("$.length()", lessThanOrEqualTo(5)))
                .andExpect(jsonPath("$[0].name").value("Business Analysis"));

        mockMvc.perform(get("/api/skills").param("size", "100"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateSkill_changesFieldsAndRejectsDuplicates() throws Exception {
        long id = createSkill("Scala " + System.nanoTime());

        mockMvc.perform(put("/api/skills/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillRequest("Scala 3", SkillCategory.OTHER))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Scala 3"))
                .andExpect(jsonPath("$.category").value("OTHER"));

        mockMvc.perform(put("/api/skills/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(json(new SkillRequest("JAVA", SkillCategory.DEVELOPMENT))))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSkill_removesIt() throws Exception {
        long id = createSkill("Temp " + System.nanoTime());

        mockMvc.perform(delete("/api/skills/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/skills/{id}", id)).andExpect(status().isNotFound());
    }
}

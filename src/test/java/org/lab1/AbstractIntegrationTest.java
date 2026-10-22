package org.lab1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.lab1.dto.InvestmentOfferCreateRequest;
import org.lab1.dto.JoinRequestCreateRequest;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.dto.SkillRequest;
import org.lab1.dto.UserCreateRequest;
import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.SkillCategory;
import org.lab1.model.enums.TeamRole;
import org.lab1.model.enums.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final BigDecimal DEFAULT_GOAL = new BigDecimal("1000.00");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    protected void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE investment_offers, join_requests, team_memberships, "
                + "project_required_skills, user_skills, projects, users RESTART IDENTITY CASCADE");
    }

    protected String json(Object value) throws JsonProcessingException {
        return objectMapper.writeValueAsString(value);
    }

    protected long postAndGetId(String url, Object body) throws Exception {
        String response = mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asLong();
    }

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }

    protected long createUser(UserRole role) throws Exception {
        return createUser(uniqueEmail(), role);
    }

    protected long createUser(String email, UserRole role) throws Exception {
        return postAndGetId("/api/users", new UserCreateRequest(email, "Test User", role, null));
    }

    protected long createSkill(String name) throws Exception {
        return postAndGetId("/api/skills", new SkillRequest(name, SkillCategory.DEVELOPMENT));
    }

    protected long createProject(long ownerId, Set<Long> requiredSkillIds) throws Exception {
        ProjectCreateRequest request = new ProjectCreateRequest(
                "Project " + UUID.randomUUID(), "Project description", ProjectStage.IDEA, DEFAULT_GOAL, ownerId, requiredSkillIds);
        return postAndGetId("/api/projects", request);
    }

    protected void updateProjectStatus(long projectId, ProjectStatus status, BigDecimal fundingGoal) throws Exception {
        ProjectUpdateRequest request = new ProjectUpdateRequest(
                "Project " + projectId, "Project description", ProjectStage.PROTOTYPE, status, fundingGoal, Set.of());
        mockMvc.perform(put("/api/projects/{id}", projectId).contentType(MediaType.APPLICATION_JSON).content(json(request)))
                .andExpect(status().isOk());
    }

    protected long createPublishedProject(long ownerId) throws Exception {
        long projectId = createProject(ownerId, Set.of());
        updateProjectStatus(projectId, ProjectStatus.PUBLISHED, DEFAULT_GOAL);
        return projectId;
    }

    protected long createJoinRequest(long projectId, long applicantId) throws Exception {
        return postAndGetId("/api/join-requests", new JoinRequestCreateRequest(projectId, applicantId, TeamRole.DEVELOPER, "Let me in"));
    }

    protected long createOffer(long projectId, long investorId, String amount) throws Exception {
        InvestmentOfferCreateRequest request = new InvestmentOfferCreateRequest(
                projectId, investorId, new BigDecimal(amount), new BigDecimal("10.00"), "Interested");
        return postAndGetId("/api/investment-offers", request);
    }
}

package org.lab1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectResponse;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.dto.SliceResponse;
import org.lab1.exception.BusinessRuleException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.ProjectMapper;
import org.lab1.mapper.SkillMapper;
import org.lab1.model.entity.Project;
import org.lab1.model.entity.Skill;
import org.lab1.model.entity.User;
import org.lab1.model.enums.ProjectStage;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.SkillCategory;
import org.lab1.model.enums.TeamRole;
import org.lab1.model.enums.UserRole;
import org.lab1.repository.ProjectRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserService userService;

    @Mock
    private SkillService skillService;

    private ProjectService projectService;

    private User owner;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService(projectRepository, userService, skillService, new ProjectMapper(new SkillMapper()));
        owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");
        owner.setFullName("Owner");
        owner.setRole(UserRole.STUDENT);
    }

    @Test
    void create_addsFounderMembershipAndRequiredSkills() {
        Skill skill = new Skill();
        skill.setId(5L);
        skill.setName("Java");
        skill.setCategory(SkillCategory.DEVELOPMENT);
        when(userService.getEntity(1L)).thenReturn(owner);
        when(skillService.getEntities(Set.of(5L))).thenReturn(Set.of(skill));
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> {
            Project saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ProjectResponse response = projectService.create(new ProjectCreateRequest(
                " Title ", " Description ", ProjectStage.IDEA, new BigDecimal("100.00"), 1L, Set.of(5L)));

        ArgumentCaptor<Project> captor = ArgumentCaptor.forClass(Project.class);
        verify(projectRepository).save(captor.capture());
        Project saved = captor.getValue();
        assertThat(saved.getMemberships()).hasSize(1);
        assertThat(saved.getMemberships().get(0).getRole()).isEqualTo(TeamRole.FOUNDER);
        assertThat(saved.getMemberships().get(0).getMember()).isSameAs(owner);
        assertThat(saved.getMemberships().get(0).getProject()).isSameAs(saved);
        assertThat(saved.getMemberships().get(0).getEquityShare()).isEqualByComparingTo("100.00");
        assertThat(response.title()).isEqualTo("Title");
        assertThat(response.status()).isEqualTo(ProjectStatus.DRAFT);
        assertThat(response.requiredSkills()).extracting("name").containsExactly("Java");
        assertThat(response.ownerName()).isEqualTo("Owner");
    }

    @Test
    void create_byInvestor_throwsBusinessRule() {
        owner.setRole(UserRole.INVESTOR);
        when(userService.getEntity(1L)).thenReturn(owner);

        assertThatThrownBy(() -> projectService.create(new ProjectCreateRequest(
                "Title", "Description", ProjectStage.IDEA, BigDecimal.TEN, 1L, null)))
                .isInstanceOf(BusinessRuleException.class);
        verify(projectRepository, never()).save(any());
    }

    @Test
    void update_rejectsManualFundedAndGoalBelowRaised() {
        Project project = project(10L);
        project.setRaisedAmount(new BigDecimal("500.00"));
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        assertThatThrownBy(() -> projectService.update(10L, new ProjectUpdateRequest(
                "Title", "Description", ProjectStage.MVP, ProjectStatus.FUNDED, new BigDecimal("1000.00"), null)))
                .isInstanceOf(BusinessRuleException.class);

        assertThatThrownBy(() -> projectService.update(10L, new ProjectUpdateRequest(
                "Title", "Description", ProjectStage.MVP, ProjectStatus.PUBLISHED, new BigDecimal("400.00"), null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void update_appliesChanges() {
        Project project = project(10L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));
        when(skillService.getEntities(null)).thenReturn(Set.of());

        ProjectResponse response = projectService.update(10L, new ProjectUpdateRequest(
                "New title", "New description", ProjectStage.GROWTH, ProjectStatus.ARCHIVED, new BigDecimal("2000.00"), null));

        assertThat(response.title()).isEqualTo("New title");
        assertThat(response.stage()).isEqualTo(ProjectStage.GROWTH);
        assertThat(response.status()).isEqualTo(ProjectStatus.ARCHIVED);
        assertThat(response.fundingGoal()).isEqualByComparingTo("2000.00");
    }

    @Test
    void findAll_withAndWithoutStatus_returnsSlice() {
        Pageable pageable = PageRequest.of(0, 2);
        when(projectRepository.findAllBy(pageable)).thenReturn(new SliceImpl<>(List.of(project(1L), project(2L)), pageable, true));
        when(projectRepository.findAllByStatus(ProjectStatus.PUBLISHED, pageable))
                .thenReturn(new SliceImpl<>(List.of(project(3L)), pageable, false));

        SliceResponse<ProjectResponse> all = projectService.findAll(null, pageable);
        SliceResponse<ProjectResponse> published = projectService.findAll(ProjectStatus.PUBLISHED, pageable);

        assertThat(all.content()).hasSize(2);
        assertThat(all.hasNext()).isTrue();
        assertThat(all.page()).isZero();
        assertThat(all.size()).isEqualTo(2);
        assertThat(published.content()).extracting(ProjectResponse::id).containsExactly(3L);
        assertThat(published.hasNext()).isFalse();
    }

    @Test
    void getById_unknown_throwsNotFound() {
        when(projectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Project with id 99 was not found");
    }

    @Test
    void delete_removesEntity() {
        Project project = project(10L);
        when(projectRepository.findById(10L)).thenReturn(Optional.of(project));

        projectService.delete(10L);

        verify(projectRepository).delete(project);
    }

    private Project project(Long id) {
        Project project = new Project();
        project.setId(id);
        project.setTitle("Project " + id);
        project.setDescription("Description");
        project.setStage(ProjectStage.IDEA);
        project.setStatus(ProjectStatus.PUBLISHED);
        project.setFundingGoal(new BigDecimal("1000.00"));
        project.setOwner(owner);
        return project;
    }
}

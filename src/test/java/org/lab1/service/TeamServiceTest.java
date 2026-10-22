package org.lab1.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.lab1.dto.JoinRequestCreateRequest;
import org.lab1.dto.JoinRequestResponse;
import org.lab1.dto.TeamMemberUpdateRequest;
import org.lab1.exception.BusinessRuleException;
import org.lab1.exception.ConflictException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.JoinRequestMapper;
import org.lab1.mapper.TeamMemberMapper;
import org.lab1.model.entity.JoinRequest;
import org.lab1.model.entity.Project;
import org.lab1.model.entity.TeamMembership;
import org.lab1.model.entity.User;
import org.lab1.model.enums.JoinRequestStatus;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.TeamRole;
import org.lab1.model.enums.UserRole;
import org.lab1.repository.JoinRequestRepository;
import org.lab1.repository.TeamMembershipRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private JoinRequestRepository joinRequestRepository;

    @Mock
    private TeamMembershipRepository membershipRepository;

    @Mock
    private ProjectService projectService;

    @Mock
    private UserService userService;

    private TeamService teamService;

    private Project project;
    private User owner;
    private User applicant;

    @BeforeEach
    void setUp() {
        teamService = new TeamService(joinRequestRepository, membershipRepository, projectService, userService,
                new JoinRequestMapper(), new TeamMemberMapper());
        owner = user(1L, UserRole.STUDENT);
        applicant = user(2L, UserRole.STUDENT);
        project = new Project();
        project.setId(10L);
        project.setTitle("Project");
        project.setOwner(owner);
        project.setStatus(ProjectStatus.PUBLISHED);
    }

    @Test
    void createJoinRequest_savesPendingRequest() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(2L)).thenReturn(applicant);
        when(membershipRepository.existsByProjectIdAndMemberId(10L, 2L)).thenReturn(false);
        when(joinRequestRepository.existsByProjectIdAndApplicantIdAndStatus(10L, 2L, JoinRequestStatus.PENDING)).thenReturn(false);
        when(joinRequestRepository.save(any(JoinRequest.class))).thenAnswer(invocation -> {
            JoinRequest saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });

        JoinRequestResponse response = teamService.createJoinRequest(
                new JoinRequestCreateRequest(10L, 2L, TeamRole.DESIGNER, "hello"));

        assertThat(response.id()).isEqualTo(50L);
        assertThat(response.status()).isEqualTo(JoinRequestStatus.PENDING);
        assertThat(response.desiredRole()).isEqualTo(TeamRole.DESIGNER);
        assertThat(response.projectTitle()).isEqualTo("Project");
    }

    @Test
    void createJoinRequest_violations_throwExpectedExceptions() {
        when(projectService.getEntity(10L)).thenReturn(project);
        when(userService.getEntity(2L)).thenReturn(applicant);

        project.setStatus(ProjectStatus.DRAFT);
        assertThatThrownBy(() -> teamService.createJoinRequest(new JoinRequestCreateRequest(10L, 2L, TeamRole.DEVELOPER, null)))
                .isInstanceOf(BusinessRuleException.class);

        project.setStatus(ProjectStatus.PUBLISHED);
        assertThatThrownBy(() -> teamService.createJoinRequest(new JoinRequestCreateRequest(10L, 2L, TeamRole.FOUNDER, null)))
                .isInstanceOf(BusinessRuleException.class);

        when(membershipRepository.existsByProjectIdAndMemberId(10L, 2L)).thenReturn(true);
        assertThatThrownBy(() -> teamService.createJoinRequest(new JoinRequestCreateRequest(10L, 2L, TeamRole.DEVELOPER, null)))
                .isInstanceOf(ConflictException.class);

        verify(joinRequestRepository, never()).save(any());
    }

    @Test
    void acceptJoinRequest_createsMembershipAndMarksAccepted() {
        JoinRequest request = joinRequest(50L);
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));
        when(membershipRepository.existsByProjectIdAndMemberId(10L, 2L)).thenReturn(false);

        JoinRequestResponse response = teamService.acceptJoinRequest(50L);

        ArgumentCaptor<TeamMembership> captor = ArgumentCaptor.forClass(TeamMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getMember()).isSameAs(applicant);
        assertThat(captor.getValue().getRole()).isEqualTo(TeamRole.DEVELOPER);
        assertThat(response.status()).isEqualTo(JoinRequestStatus.ACCEPTED);
        assertThat(request.getResolvedAt()).isNotNull();
    }

    @Test
    void acceptJoinRequest_whenNotPending_throwsConflict() {
        JoinRequest request = joinRequest(50L);
        request.setStatus(JoinRequestStatus.REJECTED);
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> teamService.acceptJoinRequest(50L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("REJECTED");
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void acceptJoinRequest_onArchivedProject_throwsBusinessRule() {
        project.setStatus(ProjectStatus.ARCHIVED);
        JoinRequest request = joinRequest(50L);
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> teamService.acceptJoinRequest(50L))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(request.getStatus()).isEqualTo(JoinRequestStatus.PENDING);
    }

    @Test
    void rejectJoinRequest_marksRejected() {
        JoinRequest request = joinRequest(50L);
        when(joinRequestRepository.findById(50L)).thenReturn(Optional.of(request));

        assertThat(teamService.rejectJoinRequest(50L).status()).isEqualTo(JoinRequestStatus.REJECTED);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void getJoinRequest_unknown_throwsNotFound() {
        when(joinRequestRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.getJoinRequest(7L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateMember_enforcesFounderRules() {
        TeamMembership ownerMembership = membership(owner, TeamRole.FOUNDER);
        TeamMembership memberMembership = membership(applicant, TeamRole.DEVELOPER);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(membershipRepository.findByProjectIdAndMemberId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByProjectIdAndMemberId(10L, 2L)).thenReturn(Optional.of(memberMembership));

        assertThatThrownBy(() -> teamService.updateMember(10L, 1L, new TeamMemberUpdateRequest(TeamRole.DEVELOPER, BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> teamService.updateMember(10L, 2L, new TeamMemberUpdateRequest(TeamRole.FOUNDER, BigDecimal.TEN)))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(teamService.updateMember(10L, 2L, new TeamMemberUpdateRequest(TeamRole.ANALYST, new BigDecimal("12.50"))).role())
                .isEqualTo(TeamRole.ANALYST);
        assertThat(memberMembership.getEquityShare()).isEqualByComparingTo("12.50");
    }

    @Test
    void removeMember_protectsOwnerAndDeletesOthers() {
        TeamMembership ownerMembership = membership(owner, TeamRole.FOUNDER);
        TeamMembership memberMembership = membership(applicant, TeamRole.DEVELOPER);
        when(projectService.getEntity(10L)).thenReturn(project);
        when(membershipRepository.findByProjectIdAndMemberId(10L, 1L)).thenReturn(Optional.of(ownerMembership));
        when(membershipRepository.findByProjectIdAndMemberId(10L, 2L)).thenReturn(Optional.of(memberMembership));
        when(membershipRepository.findByProjectIdAndMemberId(10L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.removeMember(10L, 1L)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> teamService.removeMember(10L, 3L)).isInstanceOf(ResourceNotFoundException.class);

        teamService.removeMember(10L, 2L);

        verify(membershipRepository).delete(memberMembership);
        assertThat(project.getMemberships()).containsExactly(ownerMembership);
    }

    private JoinRequest joinRequest(Long id) {
        JoinRequest request = new JoinRequest();
        request.setId(id);
        request.setProject(project);
        request.setApplicant(applicant);
        request.setDesiredRole(TeamRole.DEVELOPER);
        return request;
    }

    private TeamMembership membership(User user, TeamRole role) {
        TeamMembership membership = new TeamMembership();
        membership.setId(user.getId() + 100);
        membership.setMember(user);
        membership.setRole(role);
        project.addMember(membership);
        return membership;
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

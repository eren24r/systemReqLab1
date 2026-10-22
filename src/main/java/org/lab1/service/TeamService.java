package org.lab1.service;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.JoinRequestCreateRequest;
import org.lab1.dto.JoinRequestResponse;
import org.lab1.dto.SliceResponse;
import org.lab1.dto.TeamMemberResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TeamService {

    private final JoinRequestRepository joinRequestRepository;
    private final TeamMembershipRepository membershipRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final JoinRequestMapper joinRequestMapper;
    private final TeamMemberMapper teamMemberMapper;

    @Transactional
    public JoinRequestResponse createJoinRequest(JoinRequestCreateRequest request) {
        Project project = projectService.getEntity(request.projectId());
        User applicant = userService.getEntity(request.applicantId());
        if (project.getStatus() != ProjectStatus.PUBLISHED) {
            throw new BusinessRuleException("Join requests are accepted only for published projects");
        }
        if (applicant.getRole() == UserRole.INVESTOR) {
            throw new BusinessRuleException("Investors cannot join project teams");
        }
        if (request.desiredRole() == TeamRole.FOUNDER) {
            throw new BusinessRuleException("Founder role cannot be requested");
        }
        if (membershipRepository.existsByProjectIdAndMemberId(project.getId(), applicant.getId())) {
            throw new ConflictException("User %d is already a member of project %d".formatted(applicant.getId(), project.getId()));
        }
        if (joinRequestRepository.existsByProjectIdAndApplicantIdAndStatus(project.getId(), applicant.getId(), JoinRequestStatus.PENDING)) {
            throw new ConflictException("User %d already has a pending request for project %d".formatted(applicant.getId(), project.getId()));
        }
        JoinRequest joinRequest = new JoinRequest();
        joinRequest.setProject(project);
        joinRequest.setApplicant(applicant);
        joinRequest.setDesiredRole(request.desiredRole());
        joinRequest.setMessage(request.message());
        return joinRequestMapper.toResponse(joinRequestRepository.save(joinRequest));
    }

    @Transactional(readOnly = true)
    public JoinRequestResponse getJoinRequest(Long id) {
        return joinRequestMapper.toResponse(getJoinRequestEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<JoinRequestResponse> findJoinRequests(Long projectId, Long applicantId, Pageable pageable) {
        Page<JoinRequest> page;
        if (projectId != null) {
            page = joinRequestRepository.findAllByProjectId(projectId, pageable);
        } else if (applicantId != null) {
            page = joinRequestRepository.findAllByApplicantId(applicantId, pageable);
        } else {
            page = joinRequestRepository.findAll(pageable);
        }
        return page.map(joinRequestMapper::toResponse);
    }

    @Transactional
    public JoinRequestResponse acceptJoinRequest(Long id) {
        JoinRequest joinRequest = getJoinRequestEntity(id);
        ensurePending(joinRequest);
        Project project = joinRequest.getProject();
        if (project.getStatus() == ProjectStatus.ARCHIVED) {
            throw new BusinessRuleException("Archived projects cannot accept new members");
        }
        if (membershipRepository.existsByProjectIdAndMemberId(project.getId(), joinRequest.getApplicant().getId())) {
            throw new ConflictException("Applicant is already a member of the project");
        }
        TeamMembership membership = new TeamMembership();
        membership.setProject(project);
        membership.setMember(joinRequest.getApplicant());
        membership.setRole(joinRequest.getDesiredRole());
        membershipRepository.save(membership);

        joinRequest.setStatus(JoinRequestStatus.ACCEPTED);
        joinRequest.setResolvedAt(Instant.now());
        return joinRequestMapper.toResponse(joinRequest);
    }

    @Transactional
    public JoinRequestResponse rejectJoinRequest(Long id) {
        JoinRequest joinRequest = getJoinRequestEntity(id);
        ensurePending(joinRequest);
        joinRequest.setStatus(JoinRequestStatus.REJECTED);
        joinRequest.setResolvedAt(Instant.now());
        return joinRequestMapper.toResponse(joinRequest);
    }

    @Transactional(readOnly = true)
    public SliceResponse<TeamMemberResponse> findMembers(Long projectId, Pageable pageable) {
        projectService.getEntity(projectId);
        return SliceResponse.from(membershipRepository.findAllByProjectId(projectId, pageable).map(teamMemberMapper::toResponse));
    }

    @Transactional
    public TeamMemberResponse updateMember(Long projectId, Long userId, TeamMemberUpdateRequest request) {
        TeamMembership membership = getMembership(projectId, userId);
        boolean isOwner = membership.getProject().getOwner().getId().equals(userId);
        if (isOwner && request.role() != TeamRole.FOUNDER) {
            throw new BusinessRuleException("Project owner must keep the FOUNDER role");
        }
        if (!isOwner && request.role() == TeamRole.FOUNDER) {
            throw new BusinessRuleException("Only the project owner can have the FOUNDER role");
        }
        membership.setRole(request.role());
        membership.setEquityShare(request.equityShare());
        return teamMemberMapper.toResponse(membership);
    }

    @Transactional
    public void removeMember(Long projectId, Long userId) {
        TeamMembership membership = getMembership(projectId, userId);
        if (membership.getProject().getOwner().getId().equals(userId)) {
            throw new BusinessRuleException("Project owner cannot be removed from the team");
        }
        membership.getProject().getMemberships().remove(membership);
        membershipRepository.delete(membership);
    }

    private TeamMembership getMembership(Long projectId, Long userId) {
        projectService.getEntity(projectId);
        return membershipRepository.findByProjectIdAndMemberId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member of project " + projectId, userId));
    }

    private JoinRequest getJoinRequestEntity(Long id) {
        return joinRequestRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Join request", id));
    }

    private void ensurePending(JoinRequest joinRequest) {
        if (joinRequest.getStatus() != JoinRequestStatus.PENDING) {
            throw new ConflictException("Join request %d is already %s".formatted(joinRequest.getId(), joinRequest.getStatus()));
        }
    }
}

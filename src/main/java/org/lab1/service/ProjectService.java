package org.lab1.service;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectResponse;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.dto.SliceResponse;
import org.lab1.exception.BusinessRuleException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.ProjectMapper;
import org.lab1.model.entity.Project;
import org.lab1.model.entity.TeamMembership;
import org.lab1.model.entity.User;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.model.enums.TeamRole;
import org.lab1.model.enums.UserRole;
import org.lab1.repository.ProjectRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final BigDecimal FOUNDER_EQUITY = new BigDecimal("100.00");

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final SkillService skillService;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse create(ProjectCreateRequest request) {
        User owner = userService.getEntity(request.ownerId());
        if (owner.getRole() == UserRole.INVESTOR) {
            throw new BusinessRuleException("Investors cannot own projects");
        }
        Project project = projectMapper.toEntity(request);
        project.setOwner(owner);
        project.setRequiredSkills(skillService.getEntities(request.requiredSkillIds()));

        TeamMembership founder = new TeamMembership();
        founder.setMember(owner);
        founder.setRole(TeamRole.FOUNDER);
        founder.setEquityShare(FOUNDER_EQUITY);
        project.addMember(founder);

        return projectMapper.toResponse(projectRepository.save(project));
    }

    @Transactional(readOnly = true)
    public ProjectResponse getById(Long id) {
        return projectMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public SliceResponse<ProjectResponse> findAll(ProjectStatus status, Pageable pageable) {
        Slice<Project> slice = status == null
                ? projectRepository.findAllBy(pageable)
                : projectRepository.findAllByStatus(status, pageable);
        return SliceResponse.from(slice.map(projectMapper::toResponse));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectUpdateRequest request) {
        Project project = getEntity(id);
        if (request.status() == ProjectStatus.FUNDED && project.getStatus() != ProjectStatus.FUNDED) {
            throw new BusinessRuleException("Status FUNDED is assigned automatically when the funding goal is reached");
        }
        if (request.fundingGoal().compareTo(project.getRaisedAmount()) < 0) {
            throw new BusinessRuleException("Funding goal cannot be lower than the already raised amount");
        }
        project.setTitle(request.title().trim());
        project.setDescription(request.description().trim());
        project.setStage(request.stage());
        project.setStatus(request.status());
        project.setFundingGoal(request.fundingGoal());
        project.setRequiredSkills(skillService.getEntities(request.requiredSkillIds()));
        return projectMapper.toResponse(project);
    }

    @Transactional
    public void delete(Long id) {
        Project project = getEntity(id);
        projectRepository.delete(project);
    }

    @Transactional(readOnly = true)
    public Project getEntity(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}

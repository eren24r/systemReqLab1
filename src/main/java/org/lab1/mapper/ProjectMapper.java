package org.lab1.mapper;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectResponse;
import org.lab1.model.entity.Project;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final SkillMapper skillMapper;

    public Project toEntity(ProjectCreateRequest request) {
        Project project = new Project();
        project.setTitle(request.title().trim());
        project.setDescription(request.description().trim());
        project.setStage(request.stage());
        project.setFundingGoal(request.fundingGoal());
        return project;
    }

    public ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getTitle(),
                project.getDescription(),
                project.getStage(),
                project.getStatus(),
                project.getFundingGoal(),
                project.getRaisedAmount(),
                project.getOwner().getId(),
                project.getOwner().getFullName(),
                project.getCreatedAt(),
                skillMapper.toResponses(project.getRequiredSkills())
        );
    }
}

package org.lab1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.lab1.dto.ProjectCreateRequest;
import org.lab1.dto.ProjectResponse;
import org.lab1.dto.ProjectUpdateRequest;
import org.lab1.dto.SliceResponse;
import org.lab1.dto.TeamMemberResponse;
import org.lab1.dto.TeamMemberUpdateRequest;
import org.lab1.model.enums.ProjectStatus;
import org.lab1.service.ProjectService;
import org.lab1.service.TeamService;
import org.lab1.web.Pagination;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Projects", description = "Startup projects and their teams")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final TeamService teamService;

    @Operation(summary = "Create a project; the owner automatically becomes the FOUNDER team member")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(@Valid @RequestBody ProjectCreateRequest request) {
        return projectService.create(request);
    }

    @Operation(summary = "Get a project by id")
    @GetMapping("/{id}")
    public ProjectResponse getById(@PathVariable Long id) {
        return projectService.getById(id);
    }

    @Operation(summary = "Project feed with infinite scroll: returns hasNext instead of the total count")
    @GetMapping
    public SliceResponse<ProjectResponse> findAll(
            @RequestParam(required = false) ProjectStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        return projectService.findAll(status, Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
    }

    @Operation(summary = "Update a project (status transitions DRAFT -> PUBLISHED -> ARCHIVED are done here)")
    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectUpdateRequest request) {
        return projectService.update(id, request);
    }

    @Operation(summary = "Delete a project with its team, requests and offers")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        projectService.delete(id);
    }

    @Operation(summary = "Team members of a project with infinite scroll")
    @GetMapping("/{id}/members")
    public SliceResponse<TeamMemberResponse> findMembers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        return teamService.findMembers(id, Pagination.of(page, size, Sort.by("joinedAt", "id")));
    }

    @Operation(summary = "Change role or equity share of a team member")
    @PatchMapping("/{id}/members/{userId}")
    public TeamMemberResponse updateMember(@PathVariable Long id, @PathVariable Long userId,
                                           @Valid @RequestBody TeamMemberUpdateRequest request) {
        return teamService.updateMember(id, userId, request);
    }

    @Operation(summary = "Remove a member from the team")
    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        teamService.removeMember(id, userId);
    }
}

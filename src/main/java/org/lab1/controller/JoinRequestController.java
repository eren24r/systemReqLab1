package org.lab1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.lab1.dto.JoinRequestCreateRequest;
import org.lab1.dto.JoinRequestResponse;
import org.lab1.service.TeamService;
import org.lab1.web.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Join requests", description = "Applications of students to join project teams")
@RestController
@RequestMapping("/api/join-requests")
@RequiredArgsConstructor
public class JoinRequestController {

    private final TeamService teamService;

    @Operation(summary = "Apply to join a project team")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public JoinRequestResponse create(@Valid @RequestBody JoinRequestCreateRequest request) {
        return teamService.createJoinRequest(request);
    }

    @Operation(summary = "Get a join request by id")
    @GetMapping("/{id}")
    public JoinRequestResponse getById(@PathVariable Long id) {
        return teamService.getJoinRequest(id);
    }

    @Operation(summary = "List join requests filtered by project or applicant; total count is returned in the X-Total-Count header")
    @GetMapping
    public ResponseEntity<List<JoinRequestResponse>> findAll(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long applicantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        Page<JoinRequestResponse> result = teamService.findJoinRequests(projectId, applicantId,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        return ResponseEntity.ok()
                .header(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .header(Pagination.TOTAL_PAGES_HEADER, String.valueOf(result.getTotalPages()))
                .body(result.getContent());
    }

    @Operation(summary = "Accept a join request: the applicant becomes a team member")
    @PostMapping("/{id}/accept")
    public JoinRequestResponse accept(@PathVariable Long id) {
        return teamService.acceptJoinRequest(id);
    }

    @Operation(summary = "Reject a join request")
    @PostMapping("/{id}/reject")
    public JoinRequestResponse reject(@PathVariable Long id) {
        return teamService.rejectJoinRequest(id);
    }
}

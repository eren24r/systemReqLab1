package org.lab1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.lab1.dto.SkillRequest;
import org.lab1.dto.SkillResponse;
import org.lab1.service.SkillService;
import org.lab1.web.Pagination;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Skills", description = "Skill catalogue used by user profiles and project requirements")
@RestController
@RequestMapping("/api/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Create a skill")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SkillResponse create(@Valid @RequestBody SkillRequest request) {
        return skillService.create(request);
    }

    @Operation(summary = "Get a skill by id")
    @GetMapping("/{id}")
    public SkillResponse getById(@PathVariable Long id) {
        return skillService.getById(id);
    }

    @Operation(summary = "List skills with offset pagination; total count is returned in the X-Total-Count header")
    @GetMapping
    public ResponseEntity<List<SkillResponse>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        Page<SkillResponse> result = skillService.findAll(Pagination.of(page, size, Sort.by("name")));
        return ResponseEntity.ok()
                .header(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .header(Pagination.TOTAL_PAGES_HEADER, String.valueOf(result.getTotalPages()))
                .body(result.getContent());
    }

    @Operation(summary = "Update a skill")
    @PutMapping("/{id}")
    public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest request) {
        return skillService.update(id, request);
    }

    @Operation(summary = "Delete a skill")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skillService.delete(id);
    }
}

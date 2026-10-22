package org.lab1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.lab1.dto.UserCreateRequest;
import org.lab1.dto.UserResponse;
import org.lab1.dto.UserUpdateRequest;
import org.lab1.service.UserService;
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

@Tag(name = "Users", description = "Students, investors and their skills")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Create a user")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody UserCreateRequest request) {
        return userService.create(request);
    }

    @Operation(summary = "Get a user by id")
    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @Operation(summary = "List users with offset pagination; total count is returned in the X-Total-Count header")
    @GetMapping
    public ResponseEntity<List<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Pagination.MAX_PAGE_SIZE) int size) {
        Page<UserResponse> result = userService.findAll(Pagination.of(page, size, Sort.by("id")));
        return ResponseEntity.ok()
                .header(Pagination.TOTAL_COUNT_HEADER, String.valueOf(result.getTotalElements()))
                .header(Pagination.TOTAL_PAGES_HEADER, String.valueOf(result.getTotalPages()))
                .body(result.getContent());
    }

    @Operation(summary = "Update a user")
    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(id, request);
    }

    @Operation(summary = "Delete a user")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userService.delete(id);
    }

    @Operation(summary = "Add a skill to a user")
    @PostMapping("/{id}/skills/{skillId}")
    public UserResponse addSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return userService.addSkill(id, skillId);
    }

    @Operation(summary = "Remove a skill from a user")
    @DeleteMapping("/{id}/skills/{skillId}")
    public UserResponse removeSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return userService.removeSkill(id, skillId);
    }
}

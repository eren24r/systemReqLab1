package org.lab1.mapper;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.UserCreateRequest;
import org.lab1.dto.UserResponse;
import org.lab1.dto.UserUpdateRequest;
import org.lab1.model.entity.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final SkillMapper skillMapper;

    public User toEntity(UserCreateRequest request) {
        User user = new User();
        user.setEmail(request.email().trim().toLowerCase());
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setBio(request.bio());
        return user;
    }

    public void apply(User user, UserUpdateRequest request) {
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setBio(request.bio());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole(),
                user.getBio(),
                user.getCreatedAt(),
                skillMapper.toResponses(user.getSkills())
        );
    }
}

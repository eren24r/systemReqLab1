package org.lab1.service;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.UserCreateRequest;
import org.lab1.dto.UserResponse;
import org.lab1.dto.UserUpdateRequest;
import org.lab1.exception.ConflictException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.UserMapper;
import org.lab1.model.entity.Skill;
import org.lab1.model.entity.User;
import org.lab1.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SkillService skillService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email().trim())) {
            throw new ConflictException("User with email '%s' already exists".formatted(request.email()));
        }
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return userMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(userMapper::toResponse);
    }

    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        User user = getEntity(id);
        userMapper.apply(user, request);
        return userMapper.toResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = getEntity(id);
        userRepository.delete(user);
        userRepository.flush();
    }

    @Transactional
    public UserResponse addSkill(Long userId, Long skillId) {
        User user = getEntity(userId);
        Skill skill = skillService.getEntity(skillId);
        if (!user.getSkills().add(skill)) {
            throw new ConflictException("User %d already has skill '%s'".formatted(userId, skill.getName()));
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse removeSkill(Long userId, Long skillId) {
        User user = getEntity(userId);
        Skill skill = skillService.getEntity(skillId);
        if (!user.getSkills().remove(skill)) {
            throw new ResourceNotFoundException("Skill of user " + userId, skillId);
        }
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}

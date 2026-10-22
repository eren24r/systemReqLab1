package org.lab1.service;

import lombok.RequiredArgsConstructor;
import org.lab1.dto.SkillRequest;
import org.lab1.dto.SkillResponse;
import org.lab1.exception.ConflictException;
import org.lab1.exception.ResourceNotFoundException;
import org.lab1.mapper.SkillMapper;
import org.lab1.model.entity.Skill;
import org.lab1.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SkillService {

    private final SkillRepository skillRepository;
    private final SkillMapper skillMapper;

    @Transactional
    public SkillResponse create(SkillRequest request) {
        if (skillRepository.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("Skill with name '%s' already exists".formatted(request.name()));
        }
        return skillMapper.toResponse(skillRepository.save(skillMapper.toEntity(request)));
    }

    @Transactional(readOnly = true)
    public SkillResponse getById(Long id) {
        return skillMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<SkillResponse> findAll(Pageable pageable) {
        return skillRepository.findAll(pageable).map(skillMapper::toResponse);
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = getEntity(id);
        if (skillRepository.existsByNameIgnoreCaseAndIdNot(request.name().trim(), id)) {
            throw new ConflictException("Skill with name '%s' already exists".formatted(request.name()));
        }
        skillMapper.apply(skill, request);
        return skillMapper.toResponse(skill);
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = getEntity(id);
        skillRepository.delete(skill);
        skillRepository.flush();
    }

    @Transactional(readOnly = true)
    public Skill getEntity(Long id) {
        return skillRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Skill", id));
    }

    @Transactional(readOnly = true)
    public Set<Skill> getEntities(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashSet<>();
        }
        List<Skill> skills = skillRepository.findAllById(ids);
        if (skills.size() != new HashSet<>(ids).size()) {
            Set<Long> found = new HashSet<>();
            skills.forEach(skill -> found.add(skill.getId()));
            Long missing = ids.stream().filter(id -> !found.contains(id)).findFirst().orElseThrow();
            throw new ResourceNotFoundException("Skill", missing);
        }
        return new HashSet<>(skills);
    }
}

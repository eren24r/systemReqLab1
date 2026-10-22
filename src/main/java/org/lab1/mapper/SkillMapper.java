package org.lab1.mapper;

import org.lab1.dto.SkillRequest;
import org.lab1.dto.SkillResponse;
import org.lab1.model.entity.Skill;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class SkillMapper {

    public Skill toEntity(SkillRequest request) {
        Skill skill = new Skill();
        apply(skill, request);
        return skill;
    }

    public void apply(Skill skill, SkillRequest request) {
        skill.setName(request.name().trim());
        skill.setCategory(request.category());
    }

    public SkillResponse toResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory());
    }

    public List<SkillResponse> toResponses(Collection<Skill> skills) {
        return skills.stream()
                .sorted(Comparator.comparing(Skill::getId))
                .map(this::toResponse)
                .toList();
    }
}

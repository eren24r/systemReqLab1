package org.lab1.mapper;

import org.lab1.dto.TeamMemberResponse;
import org.lab1.model.entity.TeamMembership;
import org.springframework.stereotype.Component;

@Component
public class TeamMemberMapper {

    public TeamMemberResponse toResponse(TeamMembership membership) {
        return new TeamMemberResponse(
                membership.getId(),
                membership.getProject().getId(),
                membership.getMember().getId(),
                membership.getMember().getFullName(),
                membership.getRole(),
                membership.getEquityShare(),
                membership.getJoinedAt()
        );
    }
}

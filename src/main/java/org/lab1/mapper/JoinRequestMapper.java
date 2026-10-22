package org.lab1.mapper;

import org.lab1.dto.JoinRequestResponse;
import org.lab1.model.entity.JoinRequest;
import org.springframework.stereotype.Component;

@Component
public class JoinRequestMapper {

    public JoinRequestResponse toResponse(JoinRequest request) {
        return new JoinRequestResponse(
                request.getId(),
                request.getProject().getId(),
                request.getProject().getTitle(),
                request.getApplicant().getId(),
                request.getApplicant().getFullName(),
                request.getDesiredRole(),
                request.getMessage(),
                request.getStatus(),
                request.getCreatedAt(),
                request.getResolvedAt()
        );
    }
}

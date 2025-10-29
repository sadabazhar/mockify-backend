package com.mockify.backend.dto.request.project;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateProjectRequest {
    private String name;
    private Long organizationId;
}

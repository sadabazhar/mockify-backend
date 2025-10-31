package com.mockify.backend.mapper;

import com.mockify.backend.model.Project;
import com.mockify.backend.dto.request.project.CreateProjectRequest;
import com.mockify.backend.dto.request.project.UpdateProjectRequest;
import com.mockify.backend.dto.response.project.ProjectResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // Entity -> Response
    @Mapping(source = "organization.id", target = "organizationId")
    ProjectResponse toResponse(Project project);
    List<ProjectResponse> toResponseList(List<Project> projects);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "mockSchemas", ignore = true)
    Project toEntity(CreateProjectRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "mockSchemas", ignore = true)
    void updateEntityFromRequest(UpdateProjectRequest request, @MappingTarget Project entity);
}

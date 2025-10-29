package com.mockify.backend.mapper;

import com.mockify.backend.model.Project;
import com.mockify.backend.dto.ProjectDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ProjectMapper {

    // Converts Project entity to ProjectDTO for API responses
    ProjectDTO toDto(Project project);

    // Converts List of Project entities to List of ProjectDTO
    List<ProjectDTO> toDtoList(List<Project> projects);

    // Converts ProjectDTO to Project entity
    Project toEntity(ProjectDTO dto);

    // Converts List of ProjectDTO to List of Project entities
    List<Project> toEntityList(List<ProjectDTO> dtos);

    /*
        Updates an existing Project entity with non-null values from DTO.
        Ignores null values, does not touch id, createdAt, or organization.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "organization", ignore = true)
    void updateProjectFromDto(ProjectDTO dto, @MappingTarget Project entity);
}

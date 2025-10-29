package com.mockify.backend.mapper;

import com.mockify.backend.model.Organization;
import com.mockify.backend.dto.OrganizationDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    // Converts Organization entity to OrganizationDTO for API responses
    OrganizationDTO toDto(Organization organization);

    // Converts List of Organization entities to List of OrganizationDTO
    List<OrganizationDTO> toDtoList(List<Organization> organizations);

    // Converts OrganizationDTO to Organization entity
    Organization toEntity(OrganizationDTO dto);

    // Converts List of OrganizationDTO to List of Organization entities
    List<Organization> toEntityList(List<OrganizationDTO> dtos);

    /*
        Updates an existing Organization entity with non-null values from DTO.
        Ignores null values so existing data is safe.
        Does not update id, createdAt, or owner to preserve integrity.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    void updateOrganizationFromDto(OrganizationDTO dto, @MappingTarget Organization entity);
}


package com.mockify.backend.mapper;

import com.mockify.backend.dto.request.organization.CreateOrganizationRequest;
import com.mockify.backend.dto.request.organization.UpdateOrganizationRequest;
import com.mockify.backend.dto.response.organization.OrganizationResponse;
import com.mockify.backend.model.Organization;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrganizationMapper {

    // Entity -> Response
    @Mapping(source = "owner.id", target = "ownerId")
    OrganizationResponse toResponse(Organization organization);
    List<OrganizationResponse> toResponseList(List<Organization> organizations);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "projects", ignore = true)
    Organization toEntity(CreateOrganizationRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "projects", ignore = true)
    void updateEntityFromRequest(UpdateOrganizationRequest request, @MappingTarget Organization entity);
}


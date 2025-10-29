package com.mockify.backend.mapper;

import com.mockify.backend.model.MockSchema;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MockSchemaMapper {

    // Entity → Response
    MockSchemaResponse toResponse(MockSchema schema);
    List<MockSchemaResponse> toResponseList(List<MockSchema> schemas);

    // Create Request → Entity
    MockSchema toEntity(CreateMockSchemaRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromRequest(UpdateMockSchemaRequest request, @MappingTarget MockSchema entity);
}

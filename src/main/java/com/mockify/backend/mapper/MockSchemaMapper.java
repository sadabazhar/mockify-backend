package com.mockify.backend.mapper;

import com.mockify.backend.model.MockSchema;
import com.mockify.backend.dto.MockSchemaDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MockSchemaMapper {

    // Converts MockSchema entity to MockSchemaDTO for API responses
    MockSchemaDTO toDto(MockSchema schema);

    // Converts List of MockSchema entities to List of MockSchemaDTO
    List<MockSchemaDTO> toDtoList(List<MockSchema> schemas);

    // Converts MockSchemaDTO to MockSchema entity
    MockSchema toEntity(MockSchemaDTO dto);

    // Converts List of MockSchemaDTO to List of MockSchema entities
    List<MockSchema> toEntityList(List<MockSchemaDTO> dtos);

    /*
        Updates an existing MockSchema entity with non-null values from DTO.
        Ignores null values, keeps id, createdAt, and project unchanged.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    void updateMockSchemaFromDto(MockSchemaDTO dto, @MappingTarget MockSchema entity);
}

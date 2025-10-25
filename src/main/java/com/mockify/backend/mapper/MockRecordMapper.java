package com.mockify.backend.mapper;

import com.mockify.backend.model.MockRecord;
import com.mockify.backend.dto.MockRecordDTO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MockRecordMapper {

    // Converts MockRecord entity to MockRecordDTO for API responses
    MockRecordDTO toDto(MockRecord record);

    // Converts List of MockRecord entities to List of MockRecordDTO
    List<MockRecordDTO> toDtoList(List<MockRecord> records);

    // Converts MockRecordDTO to MockRecord entity
    MockRecord toEntity(MockRecordDTO dto);

    // Converts List of MockRecordDTO to List of MockRecord entities
    List<MockRecord> toEntityList(List<MockRecordDTO> dtos);

    /*
        Updates an existing MockRecord entity with non-null values from DTO.
        Ignores null values, keeps id, createdAt, expiresAt, and mockSchema unchanged.
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "mockSchema", ignore = true)
    void updateMockRecordFromDto(MockRecordDTO dto, @MappingTarget MockRecord entity);
}

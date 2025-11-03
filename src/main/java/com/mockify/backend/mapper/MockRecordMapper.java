package com.mockify.backend.mapper;

import com.mockify.backend.model.MockRecord;
import com.mockify.backend.dto.request.record.CreateMockRecordRequest;
import com.mockify.backend.dto.request.record.UpdateMockRecordRequest;
import com.mockify.backend.dto.response.record.MockRecordResponse;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MockRecordMapper {

    // Entity -> Response
    @Mapping(source = "mockSchema.id", target = "schemaId")
    MockRecordResponse toResponse(MockRecord record);
    List<MockRecordResponse> toResponseList(List<MockRecord> records);

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "mockSchema", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    MockRecord toEntity(CreateMockRecordRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "mockSchema", ignore = true)
    void updateEntityFromRequest(UpdateMockRecordRequest request, @MappingTarget MockRecord entity);
}

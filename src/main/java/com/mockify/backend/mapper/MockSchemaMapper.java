package com.mockify.backend.mapper;

import com.mockify.backend.dto.response.schema.MockSchemaDetailResponse;
import com.mockify.backend.model.MockRecord;
import com.mockify.backend.model.MockSchema;
import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import org.mapstruct.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MockSchemaMapper {

    // Entity -> Response
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "recordCount", expression = "java(schema.getMockRecords() != null ? schema.getMockRecords().size() : 0)")
    @Mapping(target = "endpointUrl", expression = "java(\"/api/v1/mock/schemas/\" + schema.getSlug() + \"/records\")")
    MockSchemaResponse toResponse(MockSchema schema);

    List<MockSchemaResponse> toResponseList(List<MockSchema> schemas);

    @Mapping(target = "project", expression = "java(toProjectSummary(schema))")
    @Mapping(target = "stats", expression = "java(calculateSchemaStats(schema))")
    @Mapping(target = "recentRecords", expression = "java(getRecentRecords(schema))")
    MockSchemaDetailResponse toDetailResponse(MockSchema schema);

    // ===== Nested Mappings =====
    default MockSchemaDetailResponse.ProjectSummary toProjectSummary(MockSchema schema) {
        if (schema.getProject() == null) return null;

        MockSchemaDetailResponse.ProjectSummary summary = new MockSchemaDetailResponse.ProjectSummary();
        summary.setId(schema.getProject().getId());
        summary.setName(schema.getProject().getName());

        if (schema.getProject().getOrganization() != null) {
            summary.setOrganizationName(schema.getProject().getOrganization().getName());
        }

        return summary;
    }

    // Create Request -> Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "mockRecords", ignore = true)
    @Mapping(target = "schemaJson", expression = "java(request.getSchemaJson())")
    MockSchema toEntity(CreateMockSchemaRequest request);

    // Update Request -> Entity
    // Updates existing entity with only non-null fields
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "project", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "mockRecords", ignore = true)
    @Mapping(target = "schemaJson", expression = "java(request.getSchemaJson() != null ? request.getSchemaJson() : entity.getSchemaJson())")
    void updateEntityFromRequest(UpdateMockSchemaRequest request, @MappingTarget MockSchema entity);

    // ===== Helper Methods =====

    default MockSchemaDetailResponse.SchemaStats calculateSchemaStats(MockSchema schema) {
        MockSchemaDetailResponse.SchemaStats stats = new MockSchemaDetailResponse.SchemaStats();
        LocalDateTime now = LocalDateTime.now();

        if (schema.getMockRecords() == null || schema.getMockRecords().isEmpty()) {
            stats.setTotalRecords(0);
            stats.setActiveRecords(0);
            stats.setExpiredRecords(0);
            return stats;
        }

        List<MockRecord> records = new ArrayList<>(schema.getMockRecords());
        stats.setTotalRecords(records.size());

        long activeCount = records.stream()
                .filter(r -> r.getExpiresAt().isAfter(now))
                .count();

        stats.setActiveRecords((int) activeCount);
        stats.setExpiredRecords(records.size() - (int) activeCount);

        records.stream()
                .map(MockRecord::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .ifPresent(stats::setOldestRecord);

        records.stream()
                .map(MockRecord::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .ifPresent(stats::setNewestRecord);

        return stats;
    }

    default List<MockSchemaDetailResponse.MockRecordSummary> getRecentRecords(MockSchema schema) {
        if (schema.getMockRecords() == null || schema.getMockRecords().isEmpty()) {
            return new ArrayList<>();
        }

        LocalDateTime now = LocalDateTime.now();

        return schema.getMockRecords().stream()
                .sorted(Comparator.comparing(MockRecord::getCreatedAt).reversed())
                .limit(5)
                .map(record -> {
                    MockSchemaDetailResponse.MockRecordSummary summary =
                            new MockSchemaDetailResponse.MockRecordSummary();
                    summary.setId(record.getId());
                    summary.setData(record.getData() != null ? new HashMap<>(record.getData()) : new HashMap<>());
                    summary.setCreatedAt(record.getCreatedAt());
                    summary.setExpiresAt(record.getExpiresAt());
                    summary.setExpired(record.getExpiresAt().isBefore(now));
                    return summary;
                })
                .collect(Collectors.toList());
    }
}

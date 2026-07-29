package com.mockify.backend.dto.response.schema;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemaDiffResponse {
    private UUID schemaId;
    private Integer sourceVersion;
    private Integer targetVersion;
    private boolean hasChanges;
    private Object differences;
    private Integer addedFields;
    private Integer removedFields;
    private Integer modifiedFields;
}

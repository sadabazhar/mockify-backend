package com.mockify.backend.service;

import java.util.Map;

public interface MockValidatorService {
    // Validate schema blueprint correctness (types must be string keywords)
    void validateSchemaDefinition(Map<String, Object> schemaJson);

    // Validate record data matches schema structure
    void validateRecordAgainstSchema(Map<String, Object> schemaJson, Map<String, Object> recordJson);
}

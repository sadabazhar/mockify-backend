package com.mockify.backend.service.impl;

import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.MockValidatorService;
import org.springframework.stereotype.Service;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MockValidatorServiceImpl implements MockValidatorService {

    private static final Set<String> ALLOWED_TYPES =
            Set.of("string", "number", "boolean", "array", "object");

    // -------------------------
    // 1) Validate Schema Types
    // -------------------------
    @Override
    public void validateSchemaDefinition(Map<String, Object> schemaJson) {

        if (schemaJson == null || schemaJson.isEmpty()) {
            throw new BadRequestException("Schema JSON cannot be empty");
        }

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object typeValue = entry.getValue();

            if (field == null || field.trim().isEmpty()) {
                throw new BadRequestException("Schema field name cannot be empty");
            }

            if (!(typeValue instanceof String type)) {
                throw new BadRequestException(
                        "Invalid schema: Type of field '" + field + "' must be a string"
                );
            }

            String normalized = type.toLowerCase();

            if (!ALLOWED_TYPES.contains(normalized)) {
                throw new BadRequestException(
                        "Invalid type for field '" + field + "'. Allowed: " + ALLOWED_TYPES
                );
            }
        }
    }

    // -------------------------
    // 2) Validate Record JSON
    // -------------------------
    @Override
    public void validateRecordAgainstSchema(
            Map<String, Object> schemaJson,
            Map<String, Object> recordJson
    ) {

        if (recordJson == null) {
            throw new BadRequestException("Record data cannot be null");
        }

        // Check missing or mismatched fields
        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            String expectedType = entry.getValue().toString().toLowerCase();

            if (!recordJson.containsKey(field)) {
                throw new BadRequestException("Missing field '" + field + "' in record");
            }

            Object value = recordJson.get(field);

            switch (expectedType) {
                case "string" -> {
                    if (!(value instanceof String)) {
                        throw new BadRequestException("Field '" + field + "' must be a string");
                    }
                }
                case "number" -> {
                    if (!(value instanceof Number)) {
                        throw new BadRequestException("Field '" + field + "' must be a number");
                    }
                }
                case "boolean" -> {
                    if (!(value instanceof Boolean)) {
                        throw new BadRequestException("Field '" + field + "' must be a boolean");
                    }
                }
                case "array" -> {
                    if (!(value instanceof List<?> list)) {
                        throw new BadRequestException("Field '" + field + "' must be an array");
                    }
                }
                case "object" -> {
                    if (!(value instanceof Map<?, ?> map)) {
                        throw new BadRequestException("Field '" + field + "' must be an object");
                    }
                }
                default ->
                        throw new BadRequestException("Unsupported schema data type: " + expectedType);
            }
        }

        // Check for extra fields not in schema
        for (String field : recordJson.keySet()) {
            if (!schemaJson.containsKey(field)) {
                throw new BadRequestException("Field '" + field + "' is not allowed in this schema");
            }
        }
    }
}

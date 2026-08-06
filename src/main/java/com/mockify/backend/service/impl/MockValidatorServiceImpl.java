package com.mockify.backend.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.mockify.backend.exception.BadRequestException;
import com.mockify.backend.service.MockValidatorService;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;


@Service
public class MockValidatorServiceImpl implements MockValidatorService {


    /**
     * All supported data types that a schema field is allowed to use.
     * This keeps the system strict and prevents users from defining random or unsafe types.
     */

    private enum ALLOWED_TYPES {

        STRING("string"),
        NUMBER("number"),
        BOOLEAN("boolean"),
        ARRAY("array"),
        OBJECT("object"),
        EMAIL("email"),
        UUID("uuid"),
        DATETIME("datetime"),
        NULL("null"),
        JSON("json"),
        ENUM("enum");

        private final String value;

        ALLOWED_TYPES(String value) {
            this.value = value;
        }

        public static ALLOWED_TYPES from(String value) {
            for (ALLOWED_TYPES type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new BadRequestException("Invalid schema type: " + value);
        }
    }


    // validateSchemaDefinition

    @Override
    public void validateSchemaDefinition(Map<String, Object> schemaJson) {

        if (schemaJson == null || schemaJson.isEmpty()) {
            throw new BadRequestException("Schema JSON cannot be empty");
        }

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object definition = entry.getValue();

            if (field == null || field.trim().isEmpty()) {
                throw new BadRequestException("Schema field name cannot be empty");
            }

            ALLOWED_TYPES type;


            if (definition instanceof String strType) {
                type = parseType(field, strType);
            } else if (definition instanceof Map<?, ?> defMap) {

                Object typeObj = defMap.get("type");
                if (!(typeObj instanceof String)) {
                    throw new BadRequestException("Field '" + field + "' must define a type");
                }

                type = parseType(field, typeObj.toString());

                if (type == ALLOWED_TYPES.ENUM) {
                    Object values = defMap.get("values");
                    if (!(values instanceof List<?> list) || list.isEmpty()) {
                        throw new BadRequestException("Enum field '" + field + "' must define non-empty values");
                    }
                }
            } else {
                throw new BadRequestException("Invalid schema format for field '" + field + "'");
            }
        }
    }


    // Record Validation

    @Override
    public void validateRecordAgainstSchema(
            Map<String, Object> schemaJson,
            Map<String, Object> recordJson
    ) {

        if (recordJson == null) {
            throw new BadRequestException("Record data cannot be null");
        }

        // Validate each field defined in schema

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            Object schemaDef = entry.getValue();

            if (!recordJson.containsKey(field)) {
                throw new BadRequestException("Missing field '" + field + "' in record");
            }

            Object value = recordJson.get(field);

            ALLOWED_TYPES type;
            List<?> enumValues = null;

            if (schemaDef instanceof String strType) {
                type = parseType(field, strType);
            } else if (schemaDef instanceof Map<?, ?> defMap) {

                Object typeObj = defMap.get("type");

                if (typeObj == null) {
                    throw new BadRequestException(
                            "Field '" + field + "' schema must define a 'type' property"
                    );
                }

                if (!(typeObj instanceof String)) {
                    throw new BadRequestException(
                            "Field '" + field + "' schema 'type' must be a string"
                    );
                }

                type = parseType(field, (String) typeObj);


                if (type == ALLOWED_TYPES.ENUM) {
                    enumValues = (List<?>) defMap.get("values");
                }
            } else {
                throw new BadRequestException("Invalid schema definition for field '" + field + "'");
            }

            validateValueByType(field, type, value, enumValues);
        }

        // Extra field protection
        for (String field : recordJson.keySet()) {
            if (!schemaJson.containsKey(field)) {
                throw new BadRequestException("Field '" + field + "' is not allowed in this schema");
            }
        }
    }


    // validateValueByType

    private void validateValueByType(String field, ALLOWED_TYPES type, Object value, List<?> enumValues) {

        if (type == ALLOWED_TYPES.NULL) {
            if (value != null) {
                throw new BadRequestException("Field '" + field + "' must be null");
            }
            return;
        }

        if (value == null) {
            throw new BadRequestException("Field '" + field + "' cannot be null");
        }

        switch (type) {

            case STRING:
                require(value instanceof String, field, "string");
                break;

            case NUMBER:
                require(value instanceof Number, field, "number");
                break;

            case BOOLEAN:
                require(value instanceof Boolean, field, "boolean");
                break;

            case ARRAY:
                require(value instanceof List<?>, field, "array");
                break;

            case OBJECT:
            case JSON:
                require(value instanceof Map<?, ?>, field, "object");
                break;

            case EMAIL:
                require(value instanceof String, field, "email");
                if (!EmailValidator.getInstance().isValid(value.toString())) {
                    throw new BadRequestException("Invalid email format for field '" + field + "'");
                }
                break;

            case UUID:
                require(value instanceof String, field, "uuid");
                try {
                    UUID.fromString(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid UUID format for field '" + field + "'");
                }
                break;

            case DATETIME:
                require(value instanceof String, field, "datetime");
                try {
                    OffsetDateTime.parse(value.toString());
                } catch (Exception e) {
                    throw new BadRequestException("Invalid datetime format (ISO-8601) for field '" + field + "'");
                }
                break;

            case ENUM:
                if (enumValues == null || !enumValues.contains(value)) {
                    throw new BadRequestException(
                            "Invalid enum value for field '" + field + "'. Allowed: " + enumValues
                    );
                }
                break;
        }
    }

    private void require(boolean condition, String field, String type) {
        if (!condition) {
            throw new BadRequestException("Field '" + field + "' must be of type " + type);
        }
    }

    // Helper (internal only)

    private ALLOWED_TYPES parseType(String field, String typeStr) {
        try {
            return ALLOWED_TYPES.from(typeStr);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid type for field '" + field + "': " + typeStr);
        }
    }
}

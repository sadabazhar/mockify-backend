package com.mockify.backend.service.impl;

import com.github.javafaker.Faker;
import com.mockify.backend.service.MockAutoGenerateService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;

@Service
public class MockAutoGenerateServiceImpl implements MockAutoGenerateService {

    private final Faker faker = new Faker();
    private final Random random = new Random();

    // Field specific generators
    private final Map<String, Supplier<Object>> fieldGenerators = Map.of(
            "email", () -> faker.internet().emailAddress(),
            "username", () -> faker.name().username(),
            "id", () -> faker.number().numberBetween(1, 100000),
            "name", () -> faker.name().fullName()
    );

    // Type based fallback generators
    private final Map<String, Supplier<Object>> typeGenerators = Map.of(
            "string", () -> faker.lorem().word(),
            "number", () -> faker.number().numberBetween(1, 1000),
            "boolean", () -> faker.bool().bool(),
            "array", () -> List.of(faker.lorem().word(), faker.number().randomDigit()),
            "object", () -> Map.of("value", faker.lorem().word())
    );

    @Override
    public Map<String, Object> generateRecord(Map<String, Object> schemaJson) {

        Map<String, Object> record = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : schemaJson.entrySet()) {

            String field = entry.getKey();
            String type = entry.getValue().toString().toLowerCase();

            Object value =
                    Optional.ofNullable(fieldGenerators.get(field.toLowerCase()))
                            .orElse(typeGenerators.get(type))
                            .get();

            record.put(field, value);
        }

        return record;
    }
}
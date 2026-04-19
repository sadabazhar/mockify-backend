package com.mockify.backend.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mockify.backend.dto.request.apikey.CreateApiKeyRequest;
import com.mockify.backend.dto.response.apikey.ApiKeyResponse;
import com.mockify.backend.dto.response.apikey.CreateApiKeyResult;
import com.mockify.backend.model.ApiKeyPermission.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyDtoSerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testCreateApiKeyRequest_Serialization() throws Exception {
        CreateApiKeyRequest request = CreateApiKeyRequest.builder()
                .name("Test Key")
                .description("Test Description")
                .rateLimitPerMinute(1000)
                .permissions(List.of(
                        new CreateApiKeyRequest.PermissionRequest(
                                ApiPermission.READ,
                                ApiResourceType.RECORD,
                                null
                        )
                ))
                .build();

        String json = objectMapper.writeValueAsString(request);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"Test Key\""));
        assertTrue(json.contains("\"permission\":\"READ\""));
        assertTrue(json.contains("\"resourceType\":\"RECORD\""));
    }

    @Test
    void testCreateApiKeyRequest_Deserialization() throws Exception {
        String json = """
            {
                "name": "Test Key",
                "description": "Test Description",
                "rateLimitPerMinute": 1000,
                "permissions": [
                    {
                        "permission": "WRITE",
                        "resourceType": "SCHEMA",
                        "resourceId": null
                    }
                ]
            }
            """;

        CreateApiKeyRequest request = objectMapper.readValue(
                json,
                CreateApiKeyRequest.class
        );

        assertEquals("Test Key", request.getName());
        assertEquals(1000, request.getRateLimitPerMinute());
        assertEquals(1, request.getPermissions().size());
        assertEquals(ApiPermission.WRITE,
                request.getPermissions().get(0).getPermission());
    }

    @Test
    void testApiKeyResponse_Serialization() throws Exception {
        ApiKeyResponse response = ApiKeyResponse.builder()
                .id(UUID.randomUUID())
                .name("Test Key")
                .keyPrefix("mk_live_****")
                .organizationId(UUID.randomUUID())
                .organizationName("Test Org")
                .isActive(true)
                .rateLimitPerMinute(1000)
                .createdAt(LocalDateTime.now())
                .permissions(List.of())
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"Test Key\""));
        assertTrue(json.contains("\"keyPrefix\":\"mk_live_****\""));
        assertTrue(json.contains("\"active\":true"));
    }

    @Test
    void testCreateApiKeyResult_Serialization() throws Exception {
        ApiKeyResponse keyInfo = ApiKeyResponse.builder()
                .id(UUID.randomUUID())
                .name("Test Key")
                .keyPrefix("mk_live_****")
                .build();

        CreateApiKeyResult result = CreateApiKeyResult.builder()
                .apiKey("mk_live_abc123def456")
                .keyInfo(keyInfo)
                .build();

        String json = objectMapper.writeValueAsString(result);

        assertNotNull(json);
        assertTrue(json.contains("\"apiKey\":\"mk_live_abc123def456\""));
        assertTrue(json.contains("\"securityNotice\""));
        assertTrue(json.contains("Store this API key securely"));
    }
}
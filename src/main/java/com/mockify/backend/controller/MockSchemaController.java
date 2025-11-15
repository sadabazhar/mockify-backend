package com.mockify.backend.controller;

import com.mockify.backend.dto.request.schema.CreateMockSchemaRequest;
import com.mockify.backend.dto.request.schema.UpdateMockSchemaRequest;
import com.mockify.backend.dto.response.schema.MockSchemaResponse;
import com.mockify.backend.service.MockSchemaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class MockSchemaController {

    private final MockSchemaService mockSchemaService;

    @PostMapping("/schemas")
    public ResponseEntity<MockSchemaResponse> createSchema(
            @RequestBody CreateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        MockSchemaResponse response = mockSchemaService.createSchema(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/projects/{projectId}/schemas")
    public ResponseEntity<List<MockSchemaResponse>> getSchemasByProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        List<MockSchemaResponse> schemas = mockSchemaService.getSchemasByProjectId(userId, projectId);
        return ResponseEntity.ok(schemas);
    }

    @GetMapping("/schemas/{schemaId}")
    public ResponseEntity<MockSchemaResponse> getSchemaById(
            @PathVariable Long schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        MockSchemaResponse response = mockSchemaService.getSchemaById(userId, schemaId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/schemas/{schemaId}")
    public ResponseEntity<MockSchemaResponse> updateSchema(
            @PathVariable Long schemaId,
            @RequestBody UpdateMockSchemaRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        MockSchemaResponse response = mockSchemaService.updateSchema(userId, schemaId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/schemas/{schemaId}")
    public ResponseEntity<Void> deleteSchema(
            @PathVariable Long schemaId,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = Long.parseLong(userDetails.getUsername());
        mockSchemaService.deleteSchema(userId, schemaId);
        return ResponseEntity.noContent().build();
    }
}

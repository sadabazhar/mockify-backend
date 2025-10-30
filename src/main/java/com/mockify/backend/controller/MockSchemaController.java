package com.mockify.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MockSchemaController {
    @GetMapping("/projects/{projectId}/schemas")

    @GetMapping("/schemas/{schemaId}")

    @PostMapping("/schemas")

    @PutMapping("/schemas/{schemaId}")

    @DeleteMapping("/schemas/{schemaId}")
}
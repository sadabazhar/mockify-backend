package com.mockify.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MockRecordController {
    @PostMapping("/schemas/{schemaId}/records")

    @PostMapping("/schemas/{schemaId}/records/bulk")

    @PostMapping("/schemas/{schemaId}/records/query")

    @GetMapping("/records/{recordId}")

    @PutMapping("/records/{recordId}")

    @DeleteMapping("/records/{recordId}")

    @DeleteMapping("/schemas/{schemaId}/records/expired")
}

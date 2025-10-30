package com.mockify.backend.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProjectController {
    @GetMapping("/organizations/{orgId}/projects")

    @GetMapping("/projects/{projectId}")

    @PostMapping("/projects")

    @PutMapping("/projects/{projectId}")

    @DeleteMapping("/projects/{projectId}")
}

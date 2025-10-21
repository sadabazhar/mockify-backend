package com.mockify.backend.controller;

import com.mockify.backend.service.TestService;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/test/api")
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/db-check")
    public Map<String, Object> checkDatabase() {
        return testService.getAllSeedData();
    }
}

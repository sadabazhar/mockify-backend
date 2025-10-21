// TestService.java
package com.mockify.backend.service;

import com.mockify.backend.repository.*;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class TestService {
    private final UserRepository userRepo;
    private final OrganizationRepository orgRepo;
    private final ProjectRepository projectRepo;
    private final MockSchemaRepository schemaRepo;
    private final MockRecordRepository recordRepo;

    public TestService(
            UserRepository userRepo,
            OrganizationRepository orgRepo,
            ProjectRepository projectRepo,
            MockSchemaRepository schemaRepo,
            MockRecordRepository recordRepo) {
        this.userRepo = userRepo;
        this.orgRepo = orgRepo;
        this.projectRepo = projectRepo;
        this.schemaRepo = schemaRepo;
        this.recordRepo = recordRepo;
    }

    public Map<String, Object> getAllSeedData() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("users", userRepo.findAll());
        result.put("organizations", orgRepo.findAll());
        result.put("projects", projectRepo.findAll());
        result.put("schemas", schemaRepo.findAll());
        result.put("records", recordRepo.findAll());
        return result;
    }
}

package com.mockify.backend.service;

import java.util.Map;

public interface MockAutoGenerateService {
    Map<String, Object> generateRecord(Map<String, Object> schemaJson);
}

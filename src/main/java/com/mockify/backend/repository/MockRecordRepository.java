package com.mockify.backend.repository;

import com.mockify.backend.model.MockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MockRecordRepository extends JpaRepository<MockRecord, Long> {

    // Get all records under a schema
    List<MockRecord> findByMockSchema_Id(Long schemaId);

    // Find expired records before given time
    List<MockRecord> findByExpiresAtBefore(LocalDateTime now);

    // Delete all records under a schema
    void deleteByMockSchema_Id(Long schemaId);

    // Count all records
    long count();
}

package com.mockify.backend.repository;

import com.mockify.backend.model.MockRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MockRecordRepository extends JpaRepository<MockRecord, Long> {

    // Find records under a schema
    List<MockRecord> findByMockSchemaId(Long mockSchemaId);

    // Find active (non-expired) records
    List<MockRecord> findByMockSchemaIdAndExpiresAtAfter(Long mockSchemaId, LocalDateTime now);

    // Delete expired records
    void deleteByExpiresAtBefore(LocalDateTime now);

    // Delete records under a schema
    void deleteByMockSchemaId(Long mockSchemaId);

    // Count all records
    long count();
}
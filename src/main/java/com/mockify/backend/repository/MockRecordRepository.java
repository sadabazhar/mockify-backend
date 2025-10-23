package com.mockify.backend.repository;

import com.mockify.backend.model.MockRecord;

import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MockRecordRepository extends JpaRepository<MockRecord, Long> {
        // Find all records under a schema
    @Query("SELECT r FROM MockRecord r WHERE r.mockSchema.id = :mockSchemaId")
    List<MockRecord> findByMockSchemaId(@Param("mockSchemaId") Long mockSchemaId);

    // Find active (non-expired) records
    @Query("SELECT r FROM MockRecord r WHERE r.mockSchema.id = :mockSchemaId AND r.expiresAt > :now")
    List<MockRecord> findByMockSchemaIdAndExpiresAtAfter(
            @Param("mockSchemaId") Long mockSchemaId,
            @Param("now") LocalDateTime now
    );

    // Delete expired records
    @Transactional
    @Modifying
    @Query("DELETE FROM MockRecord r WHERE r.expiresAt < :now")
    void deleteByExpiresAtBefore(@Param("now") LocalDateTime now);

    // Delete all records under a schema
    @Transactional
    @Modifying
    @Query("DELETE FROM MockRecord r WHERE r.mockSchema.id = :mockSchemaId")
    void deleteByMockSchemaId(@Param("mockSchemaId") Long mockSchemaId);

    // Count all records
    @Query("SELECT COUNT(r) FROM MockRecord r")
    long countAllRecords();
}
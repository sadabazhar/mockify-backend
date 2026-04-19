package com.mockify.backend.repository;

import com.mockify.backend.model.MockRecord;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockRecordRepository extends JpaRepository<MockRecord, UUID> {

    // Get all records under a schema
    List<MockRecord> findByMockSchema_Id(UUID schemaId);

    // Find expired records before given time
    List<MockRecord> findByExpiresAtBefore(LocalDateTime now);

    // Delete all records under a schema
    void deleteByMockSchema_Id(UUID schemaId);

    // Count all records
    long count();

    // Delete expired mock datas
    @Modifying
    @Query("""
        DELETE FROM MockRecord r
        WHERE r.expiresAt < :now
    """)
    int deleteExpiredMockRecords(LocalDateTime now);


    // Eager-load full hierarchy for permission evaluation (avoids LazyInitializationException)
    @EntityGraph(attributePaths = {
            "mockSchema",
            "mockSchema.project",
            "mockSchema.project.organization",
            "mockSchema.project.organization.owner"
    })
    Optional<MockRecord> findWithContextById(@Param("id") UUID id);

}

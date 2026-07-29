package com.mockify.backend.repository;

import com.mockify.backend.model.MockSchemaVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MockSchemaVersionRepository extends JpaRepository<MockSchemaVersion, UUID> {

    // Find paginated history of versions for a specific schema
    Page<MockSchemaVersion> findByMockSchemaId(UUID mockSchemaId, Pageable pageable);

    // Find a specific version by schema ID and version number
    Optional<MockSchemaVersion> findByMockSchemaIdAndVersion(UUID mockSchemaId, Integer version);

    // Find the latest version of a schema
    Optional<MockSchemaVersion> findFirstByMockSchemaIdOrderByVersionDesc(UUID mockSchemaId);

    // Check if a specific version exists for a schema
    boolean existsByMockSchemaIdAndVersion(UUID mockSchemaId, Integer version);
}

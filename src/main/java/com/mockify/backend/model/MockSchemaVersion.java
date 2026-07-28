package com.mockify.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "mock_schema_versions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_schema_version", columnNames = {"mock_schema_id", "version"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockSchemaVersion {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "Mock schema is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mock_schema_id", nullable = false)
    private MockSchema mockSchema;

    @Min(value = 1, message = "Version must be at least 1")
    @Column(nullable = false)
    private Integer version;

    @NotNull(message = "Schema snapshot is required")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json_snapshot", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schemaJsonSnapshot;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "diff_json", columnDefinition = "jsonb")
    private List<Map<String, Object>> diffJson;

    @Column(name = "commit_message", length = 500)
    private String commitMessage;

    @Column(name = "changed_by_user_id")
    private UUID changedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
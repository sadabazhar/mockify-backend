package com.mockify.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "mock_schemas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MockSchema {
    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "Schema name is required")
    @Column(nullable = false)
    private String name;

    @NotBlank(message = "Slug is required")
    @Column(nullable = false, length = 255)
    private String slug;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull(message = "Schema JSON is required")
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "schema_json", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> schemaJson;

    @Min(value = 1, message = "Active version must be at least 1")
    @Column(name = "active_version", nullable = false)
    private Integer activeVersion;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "mockSchema", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MockRecord> mockRecords = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "mockSchema", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("version DESC")
    private List<MockSchemaVersion> versions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
        if (activeVersion == null) {
            activeVersion = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
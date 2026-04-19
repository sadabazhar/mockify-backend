package com.mockify.backend.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "api_key_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKeyPermission {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_key_id", nullable = false)
    private ApiKey apiKey;

    /**
     * Permission level: read, write, delete, admin
     * Hierarchical: read < write < delete < admin
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private ApiPermission permission;

    /**
     * Resource type: schema, record, project, organization
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "resource_type", nullable = false)
    private ApiResourceType resourceType;

    /**
     * Optional: specific resource ID
     * NULL = wildcard (all resources of this type)
     * Non-NULL = scoped to specific resource
     */
    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Permission levels (hierarchical)
     */
    public enum ApiPermission {
        READ,    // View resources
        WRITE,   // Create/update resources
        DELETE,  // Remove resources
        ADMIN;   // Full control (implies all above)

        /**
         * Check if this permission level includes another
         */
        public boolean includes(ApiPermission other) {
            return this.ordinal() >= other.ordinal();
        }
    }

    /**
     * Resource types that permissions apply to
     */
    public enum ApiResourceType {
        SCHEMA,
        RECORD,
        PROJECT,
        ORGANIZATION
    }
}
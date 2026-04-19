package com.mockify.backend.model;

import jakarta.persistence.*;
import lombok.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Visible prefix for identification (e.g., "mk_live_")
     * Displayed to users for key recognition
     */
    @Column(name = "key_prefix", nullable = false, length = 16)
    private String keyPrefix;

    /**
     * HMAC-SHA256 hash of the full API key
     * Never store the raw key after initial generation
     */
    @JsonIgnore
    @Column(name = "key_hash", nullable = false, length = 255)
    private String keyHash;

    // Ownership & Scope
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    /**
     * Optional project-level scoping
     * NULL = org-wide access
     * Non-NULL = restricted to specific project
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    // Key lifecycle
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Rate limiting
    @Builder.Default
    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 1000;

    // Permissions
    @JsonIgnore
    @OneToMany(mappedBy = "apiKey", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ApiKeyPermission> permissions = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if the API key is currently valid
     */
    public boolean isValid() {
        if (!isActive) {
            return false;
        }

        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }

        return true;
    }

    /**
     * Update last used timestamp
     */
    public void markAsUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }

    public void addPermission(ApiKeyPermission permission) {
        permissions.add(permission);
        permission.setApiKey(this);
    }
}
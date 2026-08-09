package com.mockify.backend.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Organization {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String slug;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * True for organizations created as part of a sandbox session.
     *
     * Sandbox orgs are owned by GUEST users and are automatically
     * deleted when expires_at passes. When a guest converts to a
     * real account, this flag is set to false and expires_at is
     * cleared — making the org indistinguishable from a real org.
     *
     * Invariant (enforced by DB CHECK constraint):
     *   if isSandbox = true then expiresAt must not be null.
     */
    @Column(name = "is_sandbox", nullable = false)
    private boolean isSandbox = false;

    /**
     * Sandbox expiry timestamp (null for real organizations).
     * The cleanup scheduler deletes orgs where:
     *   is_sandbox = true AND expires_at < NOW()
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @JsonIgnore
    @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL)
    private Set<Project> projects = new HashSet<>();

    // -------------------------------------------------------------------------
    // Lifecycle helpers
    // -------------------------------------------------------------------------

    /**
     * Returns true if this sandbox org has passed its expiry time.
     * Always false for non-sandbox orgs.
     */
    public boolean isExpired() {
        if (!isSandbox) {
            return false;
        }
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

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
}
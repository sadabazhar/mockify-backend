package com.mockify.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mockify.backend.common.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /**
     * Nullable for GUEST (sandbox) users.
     *
     * Uniqueness is enforced at the DB level via a partial index
     * (idx_users_email_unique_partial, WHERE email IS NOT NULL)
     * rather than a JPA unique constraint — because Hibernate's
     * @Column(unique=true) generates a blanket UNIQUE constraint
     * which would reject two simultaneous null emails.
     *
     * Do NOT add unique=true here.
     */
    @Column(nullable = true)
    private String email;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @JsonIgnore
    @Column(name = "password")
    private String password;

    // Default role is USER to prevent accidental admin creation
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "provider_name", nullable = false, length = 50)
    private String providerName = "local";

    @Column(name = "provider_id", length = 50)
    private String providerId;

    @Column(name = "username", unique = true, length = 150)
    private String username;

    @Column(name = "first_name", length = 150)
    private String firstName;

    @Column(name = "last_name", length = 150)
    private String lastName;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    /**
     * HMAC-SHA256 hash of the sandbox opaque token.
     * Stored as a Redis-fallback: if Redis is unavailable,
     * sandbox token validation falls back to this column.
     * Null for non-sandbox users.
     */
    @JsonIgnore
    @Column(name = "sandbox_token_hash", length = 255)
    private String sandboxTokenHash;

    @Column(name = "sandbox_created_at")
    private LocalDateTime sandboxCreatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL)
    private Set<Organization> organizations = new HashSet<>();

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // Role helpers
    // -------------------------------------------------------------------------

    public boolean isGuest() {
        return UserRole.GUEST == this.role;
    }

    public boolean isRealUser() {
        return UserRole.USER == this.role || UserRole.ADMIN == this.role;
    }
}
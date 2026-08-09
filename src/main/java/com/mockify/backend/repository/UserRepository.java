package com.mockify.backend.repository;

import com.mockify.backend.common.enums.UserRole;
import com.mockify.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Find user by email
    Optional<User> findByEmail(String email);

    // Check if email exists
    boolean existsByEmail(String email);

    // Delete user by email
    void deleteByEmail(String email);

    // Count all users
    long count();

    Optional<User> findByUsername(String username);

    Optional<User> findByProviderNameAndProviderId(String providerName, String providerId);

    boolean existsByUsername(String username);

    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    Page<User> findByRole(UserRole role, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCaseAndRole(String email, UserRole role, Pageable pageable);

    /**
     * Find a guest user by the hash of their sandbox token.
     * Used for Redis-fallback token validation.
     */
    Optional<User> findBySandboxTokenHash(String sandboxTokenHash);

    /**
     * Used by the cleanup scheduler to delete orphaned guest users
     * (guests whose sandbox org has already been deleted).
     */
    @Modifying
    @Query("""
        DELETE FROM User u
        WHERE u.role = com.mockify.backend.common.enums.UserRole.GUEST
          AND NOT EXISTS (
              SELECT 1 FROM Organization o WHERE o.owner.id = u.id
          )
    """)
    int deleteOrphanedGuestUsers();

    /**
     * Admin listing — exclude GUEST accounts from user management views.
     */
    Page<User> findByEmailContainingIgnoreCaseAndRoleNot(
            String email, UserRole role, Pageable pageable);

    Page<User> findByRoleNot(UserRole role, Pageable pageable);

    Page<User> findByEmailContainingIgnoreCaseAndRoleAndRoleNot(
            String email, UserRole filterRole, UserRole excludeRole, Pageable pageable);
}
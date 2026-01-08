package com.mockify.backend.repository;

import com.mockify.backend.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PasswordResetTokenRepository
        extends JpaRepository<PasswordResetToken, UUID> {

    List<PasswordResetToken> findByUsedFalseAndExpiresAtAfter(LocalDateTime now);

    // Delete expired reset tokens
    @Modifying
    @Query("""
        DELETE FROM PasswordResetToken t
        WHERE t.expiresAt < :now
           OR (t.used = true AND t.createdAt < :cutoff)
    """)
    int deleteExpiredTokens(LocalDateTime now, LocalDateTime cutoff);
}

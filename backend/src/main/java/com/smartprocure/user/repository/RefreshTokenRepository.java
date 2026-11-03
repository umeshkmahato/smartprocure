package com.smartprocure.user.repository;

import com.smartprocure.user.entity.RefreshToken;
import com.smartprocure.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /** Scheduled cleanup: remove tokens that expired before the given cutoff. */
    void deleteAllByExpiresAtBefore(LocalDateTime cutoff);

    /** Logout / forced-logout: revoke all active tokens for a user. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllActiveByUser(@Param("user") User user);
}

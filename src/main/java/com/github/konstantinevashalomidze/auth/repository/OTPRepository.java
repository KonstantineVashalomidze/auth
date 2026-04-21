package com.github.konstantinevashalomidze.auth.repository;

import com.github.konstantinevashalomidze.auth.model.entities.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {

    /**
     * Find the most recent unused, unexpired OTP for an email.
     * Ordered descending so the latest code wins when multiple were sent.
     */
    Optional<OTP> findFirstByEmailAndUsedAtIsNullAndExpiresAtAfterOrderByCreatedAtDesc(
            String email,
            Instant now
    );

    /** Purge consumed or expired OTPs — suitable for a scheduled cleanup job. */
    @Modifying
    @Query("DELETE FROM OTP o WHERE o.usedAt IS NOT NULL OR o.expiresAt < :cutoff")
    void deleteConsumedOrExpired(@Param("cutoff") Instant cutoff);
}

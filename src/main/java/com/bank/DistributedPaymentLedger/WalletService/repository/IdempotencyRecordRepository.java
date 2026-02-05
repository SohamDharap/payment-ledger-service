package com.bank.DistributedPaymentLedger.WalletService.repository;

import com.bank.DistributedPaymentLedger.WalletService.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for idempotency records.
 * Tracks processed payment requests to detect and safely handle duplicates.
 */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

    /**
     * Find an idempotency record by its unique idempotency key.
     * Used to detect duplicate/retry requests.
     */
    Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

    /**
     * Delete idempotency records older than a given date.
     * Called periodically to clean up old records (e.g., older than 24 hours).
     * This helps prevent database bloat while maintaining safety for active transactions.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM IdempotencyRecord ir WHERE ir.createdAt < :cutoffDate")
    int deleteOldRecords(@Param("cutoffDate") LocalDateTime cutoffDate);
}

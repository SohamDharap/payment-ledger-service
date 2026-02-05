package com.bank.DistributedPaymentLedger.WalletService.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Idempotency record tracks the result of a payment operation.
 * 
 * Ensures that:
 * 1. Duplicate requests (same idempotencyKey) return the same result
 * 2. The operation executes exactly once
 * 3. Concurrent duplicate requests safely resolve to the same ledger entry
 * 
 * This table is periodically cleaned (e.g., entries older than 24 hours).
 */
@Entity
@Table(
    name = "idempotency_records",
    indexes = {
        @Index(name = "idx_idempotency_key_unique", columnList = "idempotency_key", unique = true),
        @Index(name = "idx_created_at", columnList = "created_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdempotencyRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique idempotency key from the request.
     * Used to deduplicate identical requests.
     */
    @Column(nullable = false, unique = true, length = 255)
    private String idempotencyKey;

    /**
     * The ID of the ledger entry created for this idempotent operation.
     * If a duplicate request arrives, we return the same ledger entry.
     */
    @Column(name = "ledger_entry_id", nullable = false)
    private Long ledgerEntryId;

    /**
     * The status code of the response (200, 409, 400, etc.)
     */
    @Column(nullable = false)
    private Integer responseStatus;

    /**
     * The response body (JSON serialized). Useful for exact response replay.
     * Limited to reasonable size (VARCHAR or TEXT depending on DB).
     */
    @Column(columnDefinition = "TEXT")
    private String responseBody;

    /**
     * Timestamp when this idempotent operation was first processed.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;
}

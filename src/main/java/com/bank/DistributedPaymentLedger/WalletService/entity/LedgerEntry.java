package com.bank.DistributedPaymentLedger.WalletService.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Immutable ledger entry representing a single monetary operation.
 * 
 * Invariants:
 * - Once created, never updated or deleted (ledger immutability)
 * - Amount is always positive; type (CREDIT/DEBIT) determines operation
 * - Idempotency key ensures duplicate requests create only one entry
 * - All amounts use BigDecimal with scale 2 (cents)
 */
@Entity
@Table(
    name = "ledger_entries",
    indexes = {
        @Index(name = "idx_wallet_timestamp", columnList = "wallet_id,timestamp"),
        @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true)
    }
)
@Immutable
@Getter
@Setter
@ToString(exclude = "wallet")
@EqualsAndHashCode(exclude = "wallet")
@NoArgsConstructor
@AllArgsConstructor
public class LedgerEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false, updatable = false)
    private Wallet wallet;

    /**
     * Transaction type: CREDIT (money in) or DEBIT (money out)
     */
    @Column(nullable = false, updatable = false, length = 10)
    private String type; // "CREDIT" or "DEBIT"

    /**
     * Amount in cents (always positive; type determines direction).
     * Scale is always 2, rounding is HALF_UP.
     */
    @Column(nullable = false, updatable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /**
     * Unique idempotency key for this operation.
     * Ensures duplicate requests create only one ledger entry.
     * Example: UUID or "<userId>_<timestamp>_<requestHash>"
     */
    @Column(nullable = false, updatable = false, length = 255, unique = true)
    private String idempotencyKey;

    /**
     * Source/reason for the transaction (e.g., "DEPOSIT", "TRANSFER", "WITHDRAWAL", "ADMIN_CREDIT")
     */
    @Column(nullable = false, updatable = false, length = 50)
    private String source;

    /**
     * Optional reference ID linking related operations (e.g., both sides of a transfer).
     * DEBIT and CREDIT transactions of the same transfer share the same reference.
     */
    @Column(updatable = false, length = 255)
    private String reference;

    /**
     * Timestamp when this entry was created (in system time).
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime timestamp;

    /**
     * Read-only version for optimistic locking on the wallet.
     * This helps detect concurrent transaction patterns but does NOT mutate the entry.
     */
    @Version
    @Column(updatable = false)
    private Long version;

    public enum Type {
        CREDIT, DEBIT
    }
}

package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for a single ledger entry (read-only).
 * Used when returning paginated ledger history.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntryResponse {
    
    /**
     * Ledger entry ID (unique)
     */
    private Long id;

    /**
     * Transaction type: "CREDIT" (money in) or "DEBIT" (money out)
     */
    private String type;

    /**
     * Amount involved (always positive; type indicates direction)
     * Scale is always 2.
     */
    private BigDecimal amount;

    /**
     * Source/reason for this entry (e.g., "DEPOSIT", "TRANSFER", "WITHDRAWAL")
     */
    private String source;

    /**
     * Optional reference ID (e.g., transfer ID, order ID)
     */
    private String reference;

    /**
     * Timestamp when this entry was created (system time)
     */
    private LocalDateTime timestamp;

    /**
     * Idempotency key (for debugging; could be masked in some contexts)
     */
    private String idempotencyKey;
}

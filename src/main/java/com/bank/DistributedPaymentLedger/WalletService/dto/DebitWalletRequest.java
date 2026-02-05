package com.bank.DistributedPaymentLedger.WalletService.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request to debit (withdraw money from) a wallet.
 * 
 * Idempotency: The idempotencyKey ensures multiple identical requests result in one ledger entry.
 * Concurrency safe: Uses pessimistic locking to ensure balance is checked atomically.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebitWalletRequest {
    
    /**
     * Unique idempotency key for this debit operation.
     * Format: UUID or "<userId>_<timestamp>_<hash>".
     * Required to prevent duplicate debits.
     */
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 1, max = 255, message = "Idempotency key must be 1-255 characters")
    private String idempotencyKey;

    /**
     * Amount to debit (in cents, always positive).
     * Must use exactly 2 decimal places (e.g., 10.00, 99.99).
     * Will be checked against wallet balance atomically.
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 9, fraction = 2, message = "Amount must have at most 9 integer digits and 2 decimal places")
    private BigDecimal amount;

    /**
     * Optional reference ID linking to related operations.
     * Example: transferId, orderId, withdrawal request ID, etc.
     */
    @Size(max = 255, message = "Reference must be at most 255 characters")
    private String reference;
}

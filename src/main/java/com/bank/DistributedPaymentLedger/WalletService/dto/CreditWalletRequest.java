package com.bank.DistributedPaymentLedger.WalletService.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request to credit (add money to) a wallet.
 * 
 * Idempotency: The idempotencyKey ensures multiple identical requests result in one ledger entry.
 * Source identifies the reason (e.g., "DEPOSIT", "ADMIN_TRANSFER", "REFUND").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreditWalletRequest {
    
    /**
     * Unique idempotency key for this credit operation.
     * Format: UUID or "<userId>_<timestamp>_<hash>".
     * Required to prevent duplicate credits.
     */
    @NotBlank(message = "Idempotency key is required")
    @Size(min = 1, max = 255, message = "Idempotency key must be 1-255 characters")
    private String idempotencyKey;

    /**
     * Amount to credit (in cents, always positive).
     * Must use exactly 2 decimal places (e.g., 10.00, 99.99).
     */
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @DecimalMax(value = "999999999.99", message = "Amount exceeds maximum limit")
    @Digits(integer = 9, fraction = 2, message = "Amount must have at most 9 integer digits and 2 decimal places")
    private BigDecimal amount;

    /**
     * Source/reason for the credit (e.g., "DEPOSIT", "TRANSFER", "REFUND", "ADMIN").
     * Helps audit and trace money flow.
     */
    @NotBlank(message = "Source is required")
    @Size(min = 1, max = 50, message = "Source must be 1-50 characters")
    private String source;

    /**
     * Optional reference ID linking to related operations.
     * Example: transferId, orderId, refundId, etc.
     */
    @Size(max = 255, message = "Reference must be at most 255 characters")
    private String reference;
}

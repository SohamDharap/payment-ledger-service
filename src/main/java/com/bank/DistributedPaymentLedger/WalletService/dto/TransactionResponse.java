package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for credit/debit wallet operations.
 * Indicates success, idempotency conflicts, or validation errors.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionResponse {
    
    /**
     * Status of the operation.
     * "SUCCESS": Operation completed normally
     * "DUPLICATE": Idempotent request (same operation already processed)
     * "ERROR": Validation or business logic error
     */
    private String status;

    /**
     * Ledger entry ID created/returned by this operation
     */
    private Long ledgerEntryId;

    /**
     * The new wallet balance after this operation (if successful)
     * Computed from all ledger entries.
     */
    private BigDecimal newBalance;

    /**
     * The wallet ID affected by this operation
     */
    private Long walletId;

    /**
     * Transaction type (CREDIT or DEBIT)
     */
    private String type;

    /**
     * Amount involved in this operation (always positive)
     */
    private BigDecimal amount;

    /**
     * Timestamp of the ledger entry
     */
    private LocalDateTime timestamp;

    /**
     * Human-readable message (for errors or info)
     */
    private String message;

    /**
     * HTTP status code for this response
     */
    private Integer statusCode;
}

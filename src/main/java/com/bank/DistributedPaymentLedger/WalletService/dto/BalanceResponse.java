package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.*;

import java.math.BigDecimal;

/**
 * Response DTO for wallet balance query.
 * Balance is always computed from ledger entries, never stored/cached.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BalanceResponse {
    
    /**
     * Wallet ID
     */
    private Long walletId;

    /**
     * Current balance (SUM of CREDIT entries - SUM of DEBIT entries).
     * Always has exactly 2 decimal places (scale 2).
     */
    private BigDecimal balance;

    /**
     * Currency code (e.g., "USD")
     */
    private String currency;

    /**
     * Human-readable message
     */
    private String message;
}

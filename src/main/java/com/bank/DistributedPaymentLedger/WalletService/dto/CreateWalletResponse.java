package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.*;

/**
 * Response DTO for wallet creation.
 * Idempotent: Creating a wallet twice returns a 200 OK with the existing wallet ID.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletResponse {
    
    /**
     * Wallet ID (created or already existing)
     */
    private Long walletId;

    /**
     * User ID associated with this wallet
     */
    private Long userId;

    /**
     * Currency code (e.g., "USD")
     */
    private String currency;

    /**
     * Indicates if wallet was newly created (true) or already existed (false)
     */
    private Boolean isNewWallet;

    /**
     * Status message
     */
    private String message;
}

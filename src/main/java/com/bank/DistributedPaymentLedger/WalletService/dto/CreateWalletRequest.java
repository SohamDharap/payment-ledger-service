package com.bank.DistributedPaymentLedger.WalletService.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Request to create or activate a wallet for an authenticated user.
 * Idempotent: Creating a wallet for an already-active user is a no-op.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalletRequest {
    
    /**
     * Currency code (e.g., "USD", "EUR", "INR").
     * Defaults to "USD".
     */
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO code")
    private String currency;
}

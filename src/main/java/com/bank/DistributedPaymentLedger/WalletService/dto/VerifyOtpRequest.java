package com.bank.DistributedPaymentLedger.WalletService.dto;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    // Identifier used to look up the user
    private String email;
    private String mobileNumber;

    // The code the user provides
    private String otp;
}
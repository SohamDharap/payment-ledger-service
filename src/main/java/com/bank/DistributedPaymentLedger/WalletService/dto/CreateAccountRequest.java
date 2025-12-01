package com.bank.DistributedPaymentLedger.WalletService.dto;
import lombok.Data;

@Data
public class CreateAccountRequest {
    private String firstName;
    private String lastName;
    private String email;
    private String mobileNumber;
    private String role; // e.g., "USER"
}
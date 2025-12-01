package com.bank.DistributedPaymentLedger.WalletService.service;


import com.bank.DistributedPaymentLedger.WalletService.dto.AuthResponse;
import com.bank.DistributedPaymentLedger.WalletService.dto.CreateAccountRequest;
import com.bank.DistributedPaymentLedger.WalletService.entity.User;

public interface AuthService {

    /**
     * Creates a new User account, setting up a default role and encrypting the password.
     * @param request The DTO containing user data.
     * @return The saved User entity.
     */
    User createAccount(CreateAccountRequest request);

    /**
     * Generates and saves a time-bound OTP to the user's account.
     * @param identifier The user's email or mobile number.
     * @return The generated 6-digit OTP string (for debug/simulation).
     */
    String generateOtp(String identifier);

    /**
     * Verifies the provided OTP, clears the OTP field, and generates a JWT upon success.
     * @param identifier The user's email or mobile number.
     * @param otp The 6-digit code provided by the user.
     * @return AuthResponse containing the JWT token and a message.
     */
    AuthResponse verifyOtpAndLogin(String identifier, String otp);
}
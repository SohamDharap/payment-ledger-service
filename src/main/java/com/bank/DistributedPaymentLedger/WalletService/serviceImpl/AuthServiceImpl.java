package com.bank.DistributedPaymentLedger.WalletService.serviceImpl;


import com.bank.DistributedPaymentLedger.WalletService.dto.AuthResponse;
import com.bank.DistributedPaymentLedger.WalletService.dto.CreateAccountRequest;
import com.bank.DistributedPaymentLedger.WalletService.entity.User;
import com.bank.DistributedPaymentLedger.WalletService.repository.UserRepository;
import com.bank.DistributedPaymentLedger.WalletService.service.AuthService;
import com.bank.DistributedPaymentLedger.WalletService.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService { // Assuming you created an interface

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // --- Account Creation Logic ---
    @Transactional
    @Override
    public User createAccount(CreateAccountRequest request) {
        if (userRepository.existsByEmail(request.getEmail()) || userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new IllegalArgumentException("User already exists with this email or mobile number.");
        }

        User newUser = new User();
        newUser.setFirstName(request.getFirstName());
        newUser.setLastName(request.getLastName());
        newUser.setEmail(request.getEmail());
        newUser.setMobileNumber(request.getMobileNumber()); // Use the new field
        newUser.setUsername(request.getEmail());
        newUser.setPassword(passwordEncoder.encode("OTP_ONLY")); // Hash the placeholder password
        newUser.setRole(request.getRole() != null ? request.getRole() : "USER");

        return userRepository.save(newUser);
    }

    // --- OTP Generation Logic ---
    @Override
    public String generateOtp(String identifier) {
        // Search by either email OR mobile number
        User user = userRepository.findByEmailOrMobileNumber(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + identifier));

        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        return otp;
    }

    // --- OTP Verification Logic ---
    @Override
    public AuthResponse verifyOtpAndLogin(String identifier, String otp) {
        User user = userRepository.findByEmailOrMobileNumber(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));

        if (user.getOtp() == null || !user.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid or missing OTP.");
        }

        if (user.getOtpExpiry() != null && user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Request a new one.");
        }

        // Clear OTP and generate token
        user.setOtp(null);
        user.setOtpExpiry(null);
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user);

        // FIX: Return the structured AuthResponse object
        return new AuthResponse(jwtToken, "Login successful");
    }
}
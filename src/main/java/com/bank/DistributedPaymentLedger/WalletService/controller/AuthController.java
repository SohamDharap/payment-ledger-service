package com.bank.DistributedPaymentLedger.WalletService.controller;
import com.bank.DistributedPaymentLedger.WalletService.dto.AuthResponse;
import com.bank.DistributedPaymentLedger.WalletService.dto.CreateAccountRequest;
import com.bank.DistributedPaymentLedger.WalletService.dto.OtpRequest;
import com.bank.DistributedPaymentLedger.WalletService.dto.VerifyOtpRequest;
import com.bank.DistributedPaymentLedger.WalletService.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/createAccount")
    public ResponseEntity<AuthResponse> createAccount(@RequestBody CreateAccountRequest request) {
        try {
            authService.createAccount(request);
            return ResponseEntity.ok(new AuthResponse(null, "Account created. Proceed to /generateOtp."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/generateOtp")
    public ResponseEntity<AuthResponse> generateOtp(@RequestBody OtpRequest request) {
        try {
            String identifier = request.getEmail() != null ? request.getEmail() : request.getMobileNumber();
            String otp = authService.generateOtp(identifier);

            // NOTE: In production, the OTP would be sent via SMS/Email.
            return ResponseEntity.ok(new AuthResponse(null, "OTP generated for " + identifier + ". (Debug OTP: " + otp + ")"));
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new AuthResponse(null, e.getMessage()));
        }
    }

    @PostMapping("/verifyOtp")
    public ResponseEntity<AuthResponse> verifyOtp(@RequestBody VerifyOtpRequest request) {
        try {
            String identifier = request.getEmail() != null ? request.getEmail() : request.getMobileNumber();
            String jwtToken = String.valueOf(authService.verifyOtpAndLogin(identifier, request.getOtp()));

            // SUCCESS PATH (Returns 200 OK)
            return ResponseEntity.ok(new AuthResponse(jwtToken, "Login successful"));
        } catch (Exception e) {
            // ERROR PATH (Returns 401 Unauthorized)

            // CORRECTED LINE: Ensure the type matches the method signature
            AuthResponse errorBody = new AuthResponse(null, e.getMessage());
            return ResponseEntity.status(401).body(errorBody);
        }
    }
}
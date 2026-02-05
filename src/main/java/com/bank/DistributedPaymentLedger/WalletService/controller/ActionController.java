package com.bank.DistributedPaymentLedger.WalletService.controller;

import com.bank.DistributedPaymentLedger.WalletService.dto.*;
import com.bank.DistributedPaymentLedger.WalletService.service.LedgerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for ledger and payment operations.
 * All endpoints require authentication via JWT token.
 * 
 * API Patterns:
 * - POST /api/wallet/create       : Create wallet (idempotent)
 * - POST /api/wallet/credit       : Credit wallet (with idempotency key)
 * - POST /api/wallet/debit        : Debit wallet (with idempotency key)
 * - GET  /api/wallet/balance      : Get balance (derived from ledger)
 * - GET  /api/wallet/ledger       : Get paginated ledger entries
 */
@Slf4j
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class ActionController {

    private final LedgerService ledgerService;

    /**
     * Create wallet for authenticated user (idempotent).
     * 
     * Success: 200 OK (wallet created or already existed)
     * Error: 400 BAD_REQUEST (invalid input), 401 Unauthorized, 500 Internal error
     */
    @PostMapping("/create")
    public ResponseEntity<?> createWallet(@Valid @RequestBody CreateWalletRequest request) {
        try {
            Long userId = getCurrentUserId();
            log.info("Create wallet request from userId: {}", userId);

            CreateWalletResponse response = ledgerService.createWallet(userId, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error creating wallet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Credit wallet (add money).
     * Idempotent: Same idempotencyKey always produces same result.
     * 
     * Success: 200 OK
     * Duplicate: 409 CONFLICT (same idempotencyKey already processed)
     * Error: 400 BAD_REQUEST (invalid input), 401 Unauthorized, 500 Internal error
     */
    @PostMapping("/credit")
    public ResponseEntity<?> creditWallet(@Valid @RequestBody CreditWalletRequest request) {
        try {
            Long userId = getCurrentUserId();
            log.info("Credit wallet request from userId: {}, amount: {}", userId, request.getAmount());

            TransactionResponse response = ledgerService.creditWallet(userId, request);
            
            if ("DUPLICATE".equals(response.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error crediting wallet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Error crediting wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Debit wallet (withdraw money).
     * Idempotent + Concurrent-safe.
     * 
     * Success: 200 OK
     * Duplicate: 409 CONFLICT (same idempotencyKey already processed)
     * Insufficient balance: 400 BAD_REQUEST (with error message)
     * Error: 400 BAD_REQUEST (invalid input), 401 Unauthorized, 500 Internal error
     */
    @PostMapping("/debit")
    public ResponseEntity<?> debitWallet(@Valid @RequestBody DebitWalletRequest request) {
        try {
            Long userId = getCurrentUserId();
            log.info("Debit wallet request from userId: {}, amount: {}", userId, request.getAmount());

            TransactionResponse response = ledgerService.debitWallet(userId, request);
            
            if ("DUPLICATE".equals(response.getStatus())) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Validation or business error debiting wallet: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ErrorResponse(400, e.getMessage()));
        } catch (Exception e) {
            log.error("Error debiting wallet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get current wallet balance (computed from ledger entries).
     * 
     * Success: 200 OK with BalanceResponse
     * Error: 404 NOT_FOUND (wallet not found), 401 Unauthorized, 500 Internal error
     */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance() {
        try {
            Long userId = getCurrentUserId();
            log.info("Balance query from userId: {}", userId);

            BalanceResponse response = ledgerService.getBalance(userId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Wallet not found for balance query: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching balance", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal server error: " + e.getMessage()));
        }
    }

    /**
     * Get paginated ledger entries (transaction history).
     * Ordered by timestamp descending (most recent first).
     * Default page size: 20, max page: can be adjusted.
     * 
     * Query params:
     * - page=0     : Page number (zero-indexed)
     * - pageSize=20: Number of entries per page
     * 
     * Success: 200 OK with LedgerPageResponse
     * Error: 404 NOT_FOUND (wallet not found), 401 Unauthorized, 500 Internal error
     */
    @GetMapping("/ledger")
    public ResponseEntity<?> getLedgerEntries(
            @RequestParam(value = "page", defaultValue = "0") Integer page,
            @RequestParam(value = "pageSize", defaultValue = "20") Integer pageSize) {
        try {
            Long userId = getCurrentUserId();
            log.info("Ledger entries query from userId: {}, page: {}, pageSize: {}", userId, page, pageSize);

            // Validate pagination parameters
            if (page < 0) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(400, "Page number must be >= 0"));
            }
            if (pageSize < 1 || pageSize > 100) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse(400, "Page size must be between 1 and 100"));
            }

            Pageable pageable = PageRequest.of(page, pageSize);
            LedgerPageResponse response = ledgerService.getLedgerEntries(userId, pageable);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Wallet not found for ledger query: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(404, e.getMessage()));
        } catch (Exception e) {
            log.error("Error fetching ledger entries", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse(500, "Internal server error: " + e.getMessage()));
        }
    }

    // ============ Helper Methods ============

    /**
     * Extract user ID from JWT token (authentication context).
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof com.bank.DistributedPaymentLedger.WalletService.entity.User)) {
            throw new IllegalArgumentException("User not authenticated");
        }
        com.bank.DistributedPaymentLedger.WalletService.entity.User user = 
            (com.bank.DistributedPaymentLedger.WalletService.entity.User) authentication.getPrincipal();
        return user.getId();
    }


    /**
     * Simple error response DTO.
     */
    @lombok.Getter
    @lombok.Setter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private Integer statusCode;
        private String message;
    }
}

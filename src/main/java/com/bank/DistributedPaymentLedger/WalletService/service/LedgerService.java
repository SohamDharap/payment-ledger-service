package com.bank.DistributedPaymentLedger.WalletService.service;

import com.bank.DistributedPaymentLedger.WalletService.dto.*;
import org.springframework.data.domain.Pageable;

/**
 * Ledger-based payment service using immutable ledger entries.
 * 
 * Core invariants:
 * 1. All ledger entries are immutable (no updates/deletes)
 * 2. Balances are derived from ledger entries, never stored
 * 3. Idempotency keys prevent duplicate payments
 * 4. All monetary amounts use BigDecimal with scale 2
 * 5. Database writes are transactional with proper isolation
 */
public interface LedgerService {

    /**
     * Create a wallet (account) for the authenticated user.
     * Idempotent: Creating twice returns the existing wallet.
     * 
     * @param userId The authenticated user's ID
     * @param request CreateWalletRequest (currency specification)
     * @return CreateWalletResponse with wallet ID and status
     * @throws Exception if user not found or other errors
     */
    CreateWalletResponse createWallet(Long userId, CreateWalletRequest request) throws Exception;

    /**
     * Credit (add money to) a wallet.
     * Idempotent: Multiple requests with the same idempotencyKey create one entry.
     * 
     * @param userId The authenticated user's ID (wallet owner)
     * @param request CreditWalletRequest (amount, idempotency key, source, reference)
     * @return TransactionResponse with new balance and ledger entry ID
     * @throws Exception if wallet not found, amount invalid, or duplicate key conflict
     */
    TransactionResponse creditWallet(Long userId, CreditWalletRequest request) throws Exception;

    /**
     * Debit (withdraw money from) a wallet.
     * Idempotent: Multiple requests with the same idempotencyKey create one entry.
     * Concurrent-safe: Uses pessimistic locking to ensure atomic balance check.
     * 
     * @param userId The authenticated user's ID (wallet owner)
     * @param request DebitWalletRequest (amount, idempotency key, reference)
     * @return TransactionResponse with new balance and ledger entry ID
     * @throws Exception if wallet not found, amount invalid, insufficient balance, or duplicate key conflict
     */
    TransactionResponse debitWallet(Long userId, DebitWalletRequest request) throws Exception;

    /**
     * Get the current balance for a wallet.
     * Balance is computed from ledger entries: SUM(CREDIT) - SUM(DEBIT).
     * 
     * @param userId The authenticated user's ID (wallet owner)
     * @return BalanceResponse with current balance and currency
     * @throws Exception if wallet not found
     */
    BalanceResponse getBalance(Long userId) throws Exception;

    /**
     * Get paginated ledger entries (transaction history) for a wallet.
     * Entries are ordered by timestamp descending (most recent first).
     * Read-only, immutable entries.
     * 
     * @param userId The authenticated user's ID (wallet owner)
     * @param pageable Pagination parameters (page, size, sort)
     * @return LedgerPageResponse with entries and pagination metadata
     * @throws Exception if wallet not found
     */
    LedgerPageResponse getLedgerEntries(Long userId, Pageable pageable) throws Exception;
}

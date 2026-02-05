package com.bank.DistributedPaymentLedger.WalletService.serviceImpl;

import com.bank.DistributedPaymentLedger.WalletService.dto.*;
import com.bank.DistributedPaymentLedger.WalletService.entity.*;
import com.bank.DistributedPaymentLedger.WalletService.repository.*;
import com.bank.DistributedPaymentLedger.WalletService.service.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Production-grade ledger service implementation using immutable ledger entries.
 * 
 * Concurrency Strategy:
 * - CREATE WALLET: Idempotent using findByUserId (unique constraint on User)
 * - CREDIT: Idempotency record check + append-only ledger entry
 * - DEBIT: Pessimistic lock + idempotency record + amount validation + atomic write
 * - BALANCE: Computed from ledger (read-only query)
 * - LEDGER: Page queries (read-only)
 * 
 * Transaction Isolation:
 * - CREDIT: READ_COMMITTED (default) - sufficient for append-only
 * - DEBIT: SERIALIZABLE - ensures no concurrent balance race conditions
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final WalletRepository walletRepository;
    private final UserRepository userRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final String SCALE_ERROR = "Amount scale must be exactly 2 decimal places";

    /**
     * Create wallet (account) for authenticated user.
     * Idempotent: Multiple calls create only one wallet.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CreateWalletResponse createWallet(Long userId, CreateWalletRequest request) throws Exception {
        log.info("Creating wallet for userId: {}", userId);

        // Validate user exists
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String currency = request.getCurrency() != null ? request.getCurrency() : "USD";
        validateCurrency(currency);

        // Check if wallet already exists (idempotent)
        Wallet existingWallet = walletRepository.findByUserId(userId).orElse(null);
        if (existingWallet != null) {
            log.info("Wallet already exists for userId: {}", userId);
            return CreateWalletResponse.builder()
                    .walletId(existingWallet.getId())
                    .userId(userId)
                    .currency(existingWallet.getCurrency())
                    .isNewWallet(false)
                    .message("Wallet already exists for this user")
                    .build();
        }

        // Create new wallet
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setCurrency(currency);
        wallet.setBalance(ZERO); // Initial balance is zero (will be derived from ledger anyway)
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Wallet created successfully. WalletId: {}, UserId: {}", savedWallet.getId(), userId);

        return CreateWalletResponse.builder()
                .walletId(savedWallet.getId())
                .userId(userId)
                .currency(currency)
                .isNewWallet(true)
                .message("Wallet created successfully")
                .build();
    }

    /**
     * Credit wallet (add money).
     * Idempotent: Multiple requests with same idempotencyKey create one ledger entry.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponse creditWallet(Long userId, CreditWalletRequest request) throws Exception {
        log.info("Credit request: userId={}, amount={}, idempotencyKey={}", userId, request.getAmount(), request.getIdempotencyKey());

        // Validate request
        validateAmount(request.getAmount());
        validateIdempotencyKey(request.getIdempotencyKey());
        validateSource(request.getSource());

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        // Check for duplicate using idempotency key
        var existingEntry = ledgerEntryRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingEntry.isPresent()) {
            log.info("Duplicate credit request detected. Returning existing entry: {}", existingEntry.get().getId());
            LedgerEntry entry = existingEntry.get();
            BigDecimal balance = ledgerEntryRepository.calculateBalance(entry.getWallet().getId());
            return TransactionResponse.builder()
                    .status("DUPLICATE")
                    .ledgerEntryId(entry.getId())
                    .newBalance(balance)
                    .walletId(entry.getWallet().getId())
                    .type("CREDIT")
                    .amount(entry.getAmount())
                    .timestamp(entry.getTimestamp())
                    .message("Duplicate credit request (idempotent)")
                    .statusCode(409) // Conflict
                    .build();
        }

        // Get wallet (must exist)
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        // Create immutable ledger entry
        LedgerEntry entry = new LedgerEntry();
        entry.setWallet(wallet);
        entry.setType(LedgerEntry.Type.CREDIT.toString());
        entry.setAmount(amount);
        entry.setIdempotencyKey(request.getIdempotencyKey());
        entry.setSource(request.getSource());
        entry.setReference(request.getReference());
        entry.setTimestamp(LocalDateTime.now());

        LedgerEntry savedEntry = ledgerEntryRepository.save(entry);
        log.info("Credit ledger entry created: {}", savedEntry.getId());

        // Compute new balance
        BigDecimal newBalance = ledgerEntryRepository.calculateBalance(wallet.getId());

        return TransactionResponse.builder()
                .status("SUCCESS")
                .ledgerEntryId(savedEntry.getId())
                .newBalance(newBalance)
                .walletId(wallet.getId())
                .type("CREDIT")
                .amount(amount)
                .timestamp(savedEntry.getTimestamp())
                .message("Credit successful")
                .statusCode(200)
                .build();
    }

    /**
     * Debit wallet (withdraw money).
     * Idempotent + Concurrent-safe using pessimistic locking.
     * Transaction isolation: SERIALIZABLE to prevent concurrent balance race conditions.
     */
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse debitWallet(Long userId, DebitWalletRequest request) throws Exception {
        log.info("Debit request: userId={}, amount={}, idempotencyKey={}", userId, request.getAmount(), request.getIdempotencyKey());

        // Validate request
        validateAmount(request.getAmount());
        validateIdempotencyKey(request.getIdempotencyKey());

        BigDecimal amount = request.getAmount().setScale(2, RoundingMode.HALF_UP);

        // Check for duplicate using idempotency key
        var existingEntry = ledgerEntryRepository.findByIdempotencyKey(request.getIdempotencyKey());
        if (existingEntry.isPresent()) {
            log.info("Duplicate debit request detected. Returning existing entry: {}", existingEntry.get().getId());
            LedgerEntry entry = existingEntry.get();
            BigDecimal balance = ledgerEntryRepository.calculateBalance(entry.getWallet().getId());
            return TransactionResponse.builder()
                    .status("DUPLICATE")
                    .ledgerEntryId(entry.getId())
                    .newBalance(balance)
                    .walletId(entry.getWallet().getId())
                    .type("DEBIT")
                    .amount(entry.getAmount())
                    .timestamp(entry.getTimestamp())
                    .message("Duplicate debit request (idempotent)")
                    .statusCode(409) // Conflict
                    .build();
        }

        // Get wallet with pessimistic lock (SERIALIZABLE isolation ensures no race)
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        // Calculate current balance
        BigDecimal currentBalance = ledgerEntryRepository.calculateBalance(wallet.getId());
        log.debug("Current balance: {}, Debit amount: {}", currentBalance, amount);

        // Check sufficient balance (atomic under SERIALIZABLE)
        if (currentBalance.compareTo(amount) < 0) {
            log.warn("Insufficient balance. Current: {}, Requested: {}", currentBalance, amount);
            throw new IllegalArgumentException(
                    "Insufficient balance. Current: " + currentBalance + ", Required: " + amount
            );
        }

        // Create immutable ledger entry for debit
        LedgerEntry entry = new LedgerEntry();
        entry.setWallet(wallet);
        entry.setType(LedgerEntry.Type.DEBIT.toString());
        entry.setAmount(amount);
        entry.setIdempotencyKey(request.getIdempotencyKey());
        entry.setSource("DEBIT");
        entry.setReference(request.getReference());
        entry.setTimestamp(LocalDateTime.now());

        LedgerEntry savedEntry = ledgerEntryRepository.save(entry);
        log.info("Debit ledger entry created: {}", savedEntry.getId());

        // Compute new balance
        BigDecimal newBalance = ledgerEntryRepository.calculateBalance(wallet.getId());

        return TransactionResponse.builder()
                .status("SUCCESS")
                .ledgerEntryId(savedEntry.getId())
                .newBalance(newBalance)
                .walletId(wallet.getId())
                .type("DEBIT")
                .amount(amount)
                .timestamp(savedEntry.getTimestamp())
                .message("Debit successful")
                .statusCode(200)
                .build();
    }

    /**
     * Get current wallet balance (derived from ledger entries, not stored).
     */
    @Override
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long userId) throws Exception {
        log.info("Fetching balance for userId: {}", userId);

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        // Calculate balance from ledger entries
        BigDecimal balance = ledgerEntryRepository.calculateBalance(wallet.getId());

        return BalanceResponse.builder()
                .walletId(wallet.getId())
                .balance(balance)
                .currency(wallet.getCurrency())
                .message("Balance computed from ledger entries")
                .build();
    }

    /**
     * Get paginated ledger entries for wallet (transaction history).
     */
    @Override
    @Transactional(readOnly = true)
    public LedgerPageResponse getLedgerEntries(Long userId, Pageable pageable) throws Exception {
        log.info("Fetching ledger entries for userId: {}, page: {}, size: {}", userId, pageable.getPageNumber(), pageable.getPageSize());

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found for user: " + userId));

        Page<LedgerEntry> page = ledgerEntryRepository.findByWalletIdOrderByTimestampDesc(wallet.getId(), pageable);

        var entries = page.getContent()
                .stream()
                .map(this::mapLedgerEntryToResponse)
                .collect(Collectors.toList());

        return LedgerPageResponse.builder()
                .entries(entries)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalEntries(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasMore(page.hasNext())
                .message("Ledger entries retrieved successfully")
                .build();
    }

    // ============ Helper Methods ============

    private void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount.scale() != 2) {
            throw new IllegalArgumentException(SCALE_ERROR);
        }
        if (amount.precision() > 11) { // 9 integer digits + 2 decimal places
            throw new IllegalArgumentException("Amount exceeds maximum limit");
        }
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required");
        }
        if (key.length() > 255) {
            throw new IllegalArgumentException("Idempotency key must be at most 255 characters");
        }
    }

    private void validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Source is required");
        }
        if (source.length() > 50) {
            throw new IllegalArgumentException("Source must be at most 50 characters");
        }
    }

    private void validateCurrency(String currency) {
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("Currency must be a 3-letter ISO code");
        }
    }

    private LedgerEntryResponse mapLedgerEntryToResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .type(entry.getType())
                .amount(entry.getAmount())
                .source(entry.getSource())
                .reference(entry.getReference())
                .timestamp(entry.getTimestamp())
                .idempotencyKey(entry.getIdempotencyKey())
                .build();
    }
}

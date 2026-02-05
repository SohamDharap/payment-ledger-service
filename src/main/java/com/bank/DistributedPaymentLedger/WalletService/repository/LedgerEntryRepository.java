package com.bank.DistributedPaymentLedger.WalletService.repository;

import com.bank.DistributedPaymentLedger.WalletService.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repository for immutable ledger entries.
 * All operations are read-only or append-only (no updates/deletes).
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /**
     * Find a ledger entry by its unique idempotency key.
     * Used to detect and handle duplicate requests.
     */
    Optional<LedgerEntry> findByIdempotencyKey(String idempotencyKey);

    /**
     * Get paginated ledger entries for a specific wallet, ordered by timestamp descending.
     * Supports efficient querying of transaction history.
     */
    Page<LedgerEntry> findByWalletIdOrderByTimestampDesc(Long walletId, Pageable pageable);

    /**
     * Calculate the current balance for a wallet.
     * Balance = SUM(CREDIT) - SUM(DEBIT) for all entries in this wallet.
     * 
     * @param walletId The wallet ID
     * @return Current balance as BigDecimal, or 0 if no entries exist
     */
    @Query("""
        SELECT COALESCE(
            SUM(CASE WHEN le.type = 'CREDIT' THEN le.amount ELSE -le.amount END),
            0
        )
        FROM LedgerEntry le
        WHERE le.wallet.id = :walletId
    """)
    BigDecimal calculateBalance(@Param("walletId") Long walletId);

    /**
     * Count ledger entries for a wallet.
     * Useful for validating account history completeness.
     */
    long countByWalletId(Long walletId);
}

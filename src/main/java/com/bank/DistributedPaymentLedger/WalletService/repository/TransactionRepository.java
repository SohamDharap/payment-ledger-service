package com.bank.DistributedPaymentLedger.WalletService.repository;


import com.bank.DistributedPaymentLedger.WalletService.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    // To show transaction history for a specific wallet
    List<Transaction> findByWalletIdOrderByTimestampDesc(Long walletId);

    // To find a transaction pair (Debit + Credit) by their reference ID
    List<Transaction> findByReferenceId(Long referenceId);
}

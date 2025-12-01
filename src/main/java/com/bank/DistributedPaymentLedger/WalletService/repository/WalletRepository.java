package com.bank.DistributedPaymentLedger.WalletService.repository;


import com.bank.DistributedPaymentLedger.WalletService.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    // To find the wallet for a specific user
    Optional<Wallet> findByUserId(Long userId);
}
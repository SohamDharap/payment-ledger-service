package com.bank.DistributedPaymentLedger.WalletService.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type; // "CREDIT" or "DEBIT"

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    // Optional: Links a debit transaction to its corresponding credit transaction
    private Long referenceId;

    public enum TransactionType {
        DEBIT, CREDIT
    }
}
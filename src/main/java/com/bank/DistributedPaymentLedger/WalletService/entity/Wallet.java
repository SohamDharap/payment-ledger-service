package com.bank.DistributedPaymentLedger.WalletService.entity;


import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Data
@Table(name = "wallets")
public class Wallet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false)
    private String currency = "USD";

    @Version // CRITICAL: Enables Optimistic Locking to prevent race conditions
    private Long version;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
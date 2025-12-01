package com.bank.DistributedPaymentLedger.WalletService.repository;


import com.bank.DistributedPaymentLedger.WalletService.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Used by Security (ApplicationConfig)
    // Spring Security expects this method signature
    Optional<User> findByUsername(String username);

    // Used by AuthService for flexible login (email OR mobile)
    Optional<User> findByEmailOrMobileNumber(String email, String mobileNumber);

    // Used by AuthService to prevent duplicate account creation
    boolean existsByEmail(String email);
    boolean existsByMobileNumber(String mobileNumber);
}
package com.bank.DistributedPaymentLedger.WalletService.serviceImpl;

import com.bank.DistributedPaymentLedger.WalletService.dto.ResponseBean;
import com.bank.DistributedPaymentLedger.WalletService.entity.Transaction;
import com.bank.DistributedPaymentLedger.WalletService.entity.Wallet;
import com.bank.DistributedPaymentLedger.WalletService.repository.TransactionRepository;
import com.bank.DistributedPaymentLedger.WalletService.repository.UserRepository;
import com.bank.DistributedPaymentLedger.WalletService.repository.WalletRepository;
import com.bank.DistributedPaymentLedger.WalletService.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletServiceImpl implements WalletService {

    @Autowired
    WalletRepository walletDao;
    @Autowired
    UserRepository userDao;
    @Autowired
    TransactionRepository transactionDao;

    @Override
    @Transactional
    public ResponseBean addMoney(Long userId, BigDecimal amount) {
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseBean("Amount must be positive", 400, "failure");
            }
            Wallet wallet = walletDao.findByUserId(userId).orElseThrow(() -> new RuntimeException("Wallet Not Found"));

            wallet.setBalance(wallet.getBalance().add(amount));
            walletDao.save(wallet);
            // Create a CREDIT transaction record
            createTransaction(wallet, amount, Transaction.TransactionType.CREDIT, String.valueOf(System.currentTimeMillis()));
            return new ResponseBean("Money added successfully", 200, "success");
        } catch (Exception e) {
            return new ResponseBean("Error adding money: " + e.getMessage(), 500, "failure");
        }
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ResponseBean transferFunds(Long senderId, Long receiverId, BigDecimal amount) {
        try {
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return new ResponseBean("Transfer amount must be positive", 400, "FAILURE");
            }
            Wallet senderWallet = walletDao.findByUserId(senderId)
                    .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

            Wallet receiverWallet = walletDao.findByUserId(receiverId)
                    .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

            if (senderWallet.getBalance().compareTo(amount) < 0) {
                return new ResponseBean("Insufficient funds", 400, "FAILURE");
            }
            senderWallet.setBalance(senderWallet.getBalance().subtract(amount));
            walletDao.save(senderWallet);

            receiverWallet.setBalance(receiverWallet.getBalance().add(amount));
            walletDao.save(receiverWallet);

            String referenceId = String.valueOf(System.currentTimeMillis());
            createTransaction(senderWallet, amount, Transaction.TransactionType.DEBIT, referenceId);
            createTransaction(receiverWallet, amount, Transaction.TransactionType.CREDIT, referenceId);

            return new ResponseBean("Transfer successful", 200, "SUCCESS");
        } catch (Exception e) {
            return new ResponseBean("Error adding money: " + e.getMessage(), 500, "failure");
        }
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, Transaction.TransactionType type, String referenceId) {
        Transaction transaction = new Transaction();
        transaction.setWallet(wallet);
        transaction.setAmount(amount);
        transaction.setType(String.valueOf(type));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setReferenceId(Long.valueOf(referenceId));
        transactionDao.save(transaction);
    }
}

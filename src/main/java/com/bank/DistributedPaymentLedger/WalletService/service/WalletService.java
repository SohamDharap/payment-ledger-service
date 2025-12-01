package com.bank.DistributedPaymentLedger.WalletService.service;

import com.bank.DistributedPaymentLedger.WalletService.dto.ResponseBean;

import java.math.BigDecimal;

public interface WalletService {
    ResponseBean addMoney(Long userId, BigDecimal amount);
    ResponseBean transferFunds(Long senderId, Long receiverId, BigDecimal amount);
}

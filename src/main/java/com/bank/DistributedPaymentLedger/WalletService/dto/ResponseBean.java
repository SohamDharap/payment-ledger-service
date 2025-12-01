package com.bank.DistributedPaymentLedger.WalletService.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ResponseBean {
    private String message;
    private int responseCode;
    private String responseMessage;
}

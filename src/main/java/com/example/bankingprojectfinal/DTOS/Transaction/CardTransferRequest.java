package com.example.bankingprojectfinal.DTOS.Transaction;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CardTransferRequest {
    private String fromCardNumber;
    private String toCardNumber;
    private BigDecimal amount;
}

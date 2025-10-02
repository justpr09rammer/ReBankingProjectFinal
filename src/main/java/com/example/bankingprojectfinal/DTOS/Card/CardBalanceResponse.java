package com.example.bankingprojectfinal.DTOS.Card;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CardBalanceResponse {
    private String cardNumber;
    private String currency;
}
package com.example.bankingprojectfinal.Service.Abstraction;

import com.example.bankingprojectfinal.DTOS.Transaction.TransactionDto;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface TransactionService {
    // Customer methods
    TransactionDto transfer(String debitCardNumber, String creditCardNumber, BigDecimal amount);
    Page<TransactionDto> getTransactionsByCurrentUser(Integer page, Integer size);

    // Admin methods
    Page<TransactionDto> getTransactionsByCustomerId(Integer customerId, Integer page, Integer size);
    Page<TransactionDto> getAllTransactions(Integer page, Integer size);
}
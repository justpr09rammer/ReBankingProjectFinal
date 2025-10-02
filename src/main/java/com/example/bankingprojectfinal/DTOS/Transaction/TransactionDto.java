package com.example.bankingprojectfinal.DTOS.Transaction;

import com.example.bankingprojectfinal.Model.Enums.TransactionStatus;
import com.example.bankingprojectfinal.Model.Enums.TransactionType; // Added
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime; // Changed to LocalDateTime

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TransactionDto {
    String transactionId;

    String debitAccountNumber; // Renamed from 'debit' for clarity
    String creditAccountNumber; // Renamed from 'credit' for clarity
    LocalDateTime transactionDate; // Changed to LocalDateTime
    BigDecimal amount;
    TransactionStatus status;
    TransactionType transactionType; // Added
}
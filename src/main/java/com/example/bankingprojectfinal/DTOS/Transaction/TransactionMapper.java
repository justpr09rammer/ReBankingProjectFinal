package com.example.bankingprojectfinal.DTOS.Transaction;

import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.TransactionStatus;
import com.example.bankingprojectfinal.Model.Enums.TransactionType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; // Use LocalDateTime

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    @Mapping(target = "debitAccountNumber", source = "debitAccount.accountNumber")
    @Mapping(target = "creditAccountNumber", source = "creditAccount.accountNumber")
    @Mapping(target = "transactionType", source = "transactionType") // Map transaction type
        // @Mapping(target = "description", source = "description") // Uncomment if TransactionDto has description
    TransactionDto mapToTransactionDto(TransactionEntity transactionEntity);

    List<TransactionDto> mapToTransactionDtoList(List<TransactionEntity> transactionEntities);

    @Named("buildNewTransactionEntity")
    default TransactionEntity buildTransactionEntity(
            AccountEntity debitAccount, AccountEntity creditAccount,
            BigDecimal amount, TransactionType type) { // Removed description parameter
        return TransactionEntity.builder()
                .debitAccount(debitAccount)
                .creditAccount(creditAccount)
                .transactionDate(LocalDate.from(LocalDateTime.now())) // Corrected to use LocalDateTime directly
                .amount(amount)
                .status(TransactionStatus.PENDING) // Default to PENDING for new transactions
                .transactionType(type)
                .build();
    }
}
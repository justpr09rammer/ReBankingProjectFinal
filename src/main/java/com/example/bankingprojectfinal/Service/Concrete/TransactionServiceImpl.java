package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Transaction.TransactionDto;
import com.example.bankingprojectfinal.DTOS.Transaction.TransactionMapper;
import com.example.bankingprojectfinal.Exception.AccountNotActiveException;
import com.example.bankingprojectfinal.Exception.AccountNotFoundException;
import com.example.bankingprojectfinal.Exception.CardNotFoundException;
import com.example.bankingprojectfinal.Exception.LimitExceedsException;
import com.example.bankingprojectfinal.Exception.NotEnoughFundsException;
import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.CardEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.AccountStatus;
import com.example.bankingprojectfinal.Repository.AccountRepository;
import com.example.bankingprojectfinal.Repository.CardRepository;
import com.example.bankingprojectfinal.Repository.TransactionRepository;
import com.example.bankingprojectfinal.Service.Abstraction.TransactionService;
import com.example.bankingprojectfinal.Utils.LimitProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionServiceImpl implements TransactionService {
	TransactionRepository transactionRepository;
	TransactionMapper transactionMapper;
	AccountRepository accountRepository;
	CardRepository cardRepository;
	LimitProperties limitProperties;

	@Override
	public TransactionDto transfer(String debit, String credit, BigDecimal amount) {
		checkCreditNumber(credit);

		if (debit.length() == 20) {
			return transferFromAccount(debit, credit, amount);
		} else {
			throw new IllegalArgumentException("Wrong input for debit");
		}
	}

	private TransactionDto transferFromAccount(String debit, String credit, BigDecimal amount) {
		AccountEntity accountEntity = accountRepository.findByAccountNumber(debit);
		if (accountEntity == null) {
			throw new AccountNotFoundException("Account not found");
		}

		if (!accountEntity.getStatus().equals(AccountStatus.ACTIVE))
			throw new AccountNotActiveException("Account should be active");

		CustomerEntity customerEntity = accountEntity.getCustomer();
		checkCustomerTransactions(customerEntity);

		if (accountEntity.getBalance().compareTo(amount) < 0)
			throw new NotEnoughFundsException("you should have enough amount of oney");

		if (accountEntity.getBalance().compareTo(limitProperties.getMinAcceptableAccountBalance()) < 0)
			throw new LimitExceedsException("your transfer exceeds the limit");

		return createTransaction(customerEntity, debit, credit, amount);
	}

	private void checkCustomerTransactions(CustomerEntity customerEntity) {

		// placeholder for future limits
	}

	private void checkCreditNumber(String credit) {
		if (credit.length() != 20)
			throw new IllegalArgumentException("Wrong input for credit");
	}

	private TransactionDto createTransaction(CustomerEntity customerEntity,
											  String debit, String credit,
											  BigDecimal amount) {
		TransactionEntity transactionEntity = transactionMapper.buildTransactionEntity(customerEntity, debit, credit, amount);

		transactionRepository.save(transactionEntity);
		return transactionMapper.getTransactionDto(transactionEntity);
	}

	@Override
	public Page<TransactionDto> getTransactionsByCustomerId(Integer customerId, Integer page, Integer size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<TransactionEntity> transactionEntityPage = transactionRepository.findByAccount_Customer_Id(customerId, pageable);
		List<TransactionDto> transactionDtoList = transactionMapper.getTransactionDtoList(transactionEntityPage.getContent());
		return new PageImpl<>(transactionDtoList, pageable, transactionEntityPage.getTotalElements());
	}

	@Override
	public Page<TransactionDto> getAllTransactions(Integer page, Integer size) {
		Pageable pageable = PageRequest.of(page, size);
		Page<TransactionEntity> transactionEntities = transactionRepository.findAll(pageable);
		List<TransactionDto> transactionDtoList = transactionMapper.getTransactionDtoList(transactionEntities.getContent());
		return new PageImpl<>(transactionDtoList, pageable, transactionEntities.getTotalElements());
	}
}

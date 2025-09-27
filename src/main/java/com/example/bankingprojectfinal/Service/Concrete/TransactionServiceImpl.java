package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Transaction.TransactionDto;
import com.example.bankingprojectfinal.DTOS.Transaction.TransactionMapper;
import com.example.bankingprojectfinal.Exception.CardNotFoundException;
import com.example.bankingprojectfinal.Exception.LimitExceedsException;
import com.example.bankingprojectfinal.Exception.NotEnoughFundsException;
import com.example.bankingprojectfinal.Model.Entity.CardEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.CardStatus;
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
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionServiceImpl implements TransactionService {
	TransactionRepository transactionRepository;
	TransactionMapper transactionMapper;
	CardRepository cardRepository;
	LimitProperties limitProperties;

	@Override
	public TransactionDto transfer(String debitCardNumber, String creditCardNumber, BigDecimal amount) {
		// Validate both card numbers (typically 16 digits for cards)
		if (debitCardNumber.length() != 16) {
			throw new IllegalArgumentException("Debit card number must be 16 digits");
		}
		if (creditCardNumber.length() != 16) {
			throw new IllegalArgumentException("Credit card number must be 16 digits");
		}

		return transferFromCard(debitCardNumber, creditCardNumber, amount);
	}

	private TransactionDto transferFromCard(String debitCardNumber, String creditCardNumber, BigDecimal amount) {
		// Get debit card with customer information
		CardEntity debitCard = cardRepository.findByCardNumber(debitCardNumber);

		// Get credit card
		CardEntity creditCard = cardRepository.findByCardNumber(creditCardNumber);

		// Validate debit card status
		if (!debitCard.getStatus().equals(CardStatus.ACTIVE)) {
			throw new IllegalArgumentException("Debit card should be active");
		}

		// Validate credit card status
		if (!creditCard.getStatus().equals(CardStatus.ACTIVE)) {
			throw new IllegalArgumentException("Credit card should be active");
		}

		// Check if debit card has sufficient balance
		if (debitCard.getBalance().compareTo(amount) < 0) {
			throw new NotEnoughFundsException("Insufficient funds on debit card");
		}

		// Check minimum balance limit after transfer
		BigDecimal balanceAfterTransfer = debitCard.getBalance().subtract(amount);
		if (balanceAfterTransfer.compareTo(limitProperties.getMinAcceptableAccountBalance()) < 0) {
			throw new LimitExceedsException("Transfer would leave balance below minimum limit");
		}

		// Get customer from debit card's account
		CustomerEntity customerEntity = debitCard.getAccount().getCustomer();

		// Update balances
		debitCard.setBalance(debitCard.getBalance().subtract(amount));
		creditCard.setBalance(creditCard.getBalance().add(amount));

		// Save both cards
		cardRepository.save(debitCard);
		cardRepository.save(creditCard);

		log.info("Transfer completed: {} from card {} to card {}", amount, debitCardNumber, creditCardNumber);

		return createTransaction(customerEntity, debitCardNumber, creditCardNumber, amount);
	}

	private TransactionDto createTransaction(CustomerEntity customerEntity,
											 String debitCardNumber, String creditCardNumber,
											 BigDecimal amount) {
		TransactionEntity transactionEntity = transactionMapper.buildTransactionEntity(
				customerEntity,
				debitCardNumber,
				creditCardNumber,
				amount
		);

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
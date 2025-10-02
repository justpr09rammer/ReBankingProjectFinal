package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Transaction.TransactionDto;
import com.example.bankingprojectfinal.DTOS.Transaction.TransactionMapper;
import com.example.bankingprojectfinal.Exception.CardNotFoundException;
import com.example.bankingprojectfinal.Exception.LimitExceedsException;
import com.example.bankingprojectfinal.Exception.NotEnoughFundsException;
import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.CardEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.CardStatus;
import com.example.bankingprojectfinal.Model.Enums.TransactionStatus;
import com.example.bankingprojectfinal.Model.Enums.TransactionType;
import com.example.bankingprojectfinal.Repository.AccountRepository;
import com.example.bankingprojectfinal.Repository.CardRepository;
import com.example.bankingprojectfinal.Repository.TransactionRepository;
import com.example.bankingprojectfinal.Service.Abstraction.TransactionService;
import com.example.bankingprojectfinal.Utils.LimitProperties;
import com.example.bankingprojectfinal.security.model.User;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionServiceImpl implements TransactionService {
    TransactionRepository transactionRepository;
    TransactionMapper transactionMapper;
    CardRepository cardRepository;
    AccountRepository accountRepository;
    LimitProperties limitProperties; // Assuming this class defines your limits

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("User not authenticated");
    }

    private CustomerEntity getCurrentCustomer() {
        User user = getCurrentUser();
        if (user.getCustomer() == null) {
            throw new IllegalStateException("Customer profile not found for authenticated user");
        }
        return user.getCustomer();
    }

    @Override
    @Transactional
    public TransactionDto transfer(String debitCardNumber, String creditCardNumber, BigDecimal amount) {
        CustomerEntity currentCustomer = getCurrentCustomer();

        log.info("Customer ID {} initiating card-to-card transfer from {} to {} for amount {}",
                currentCustomer.getId(), debitCardNumber, creditCardNumber, amount);

        // --- Input Validations ---
        if (debitCardNumber == null || debitCardNumber.length() != 16 || !debitCardNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid debit card number format. Must be 16 digits.");
        }
        if (creditCardNumber == null || creditCardNumber.length() != 16 || !creditCardNumber.matches("\\d+")) {
            throw new IllegalArgumentException("Invalid credit card number format. Must be 16 digits.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }
        if (debitCardNumber.equals(creditCardNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same card.");
        }

        // --- Card and Account Retrieval & Validation ---
        CardEntity debitCard = cardRepository.findByCardNumber(debitCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Debit card not found with number: " + debitCardNumber));
        CardEntity creditCard = cardRepository.findByCardNumber(creditCardNumber)
                .orElseThrow(() -> new CardNotFoundException("Credit card not found with number: " + creditCardNumber));

        // Ensure the debit card belongs to the authenticated customer
        if (!debitCard.getAccount().getCustomer().getId().equals(currentCustomer.getId())) {
            throw new IllegalStateException("Access Denied: You can only transfer from your own cards.");
        }

        // Validate card statuses
        if (!debitCard.getStatus().equals(CardStatus.ACTIVE)) {
            throw new IllegalArgumentException("Your debit card is not active. Status: " + debitCard.getStatus());
        }
        if (!creditCard.getStatus().equals(CardStatus.ACTIVE)) {
            throw new IllegalArgumentException("Recipient's credit card is not active. Status: " + creditCard.getStatus());
        }

        // Retrieve associated accounts
        AccountEntity debitAccount = debitCard.getAccount();
        AccountEntity creditAccount = creditCard.getAccount();

        // --- Balance and Limit Checks ---
        // Check for sufficient funds in debit account
        if (debitAccount.getBalance().compareTo(amount) < 0) {
            throw new NotEnoughFundsException("Insufficient funds in your account. Current balance: " + debitAccount.getBalance());
        }

        // Check minimum balance limit after transfer
        BigDecimal balanceAfterTransfer = debitAccount.getBalance().subtract(amount);
        if (balanceAfterTransfer.compareTo(limitProperties.getMinAcceptableAccountBalance()) < 0) {
            throw new LimitExceedsException(
                    "Transfer would leave your balance (" + balanceAfterTransfer + ") below the minimum limit (" +
                            limitProperties.getMinAcceptableAccountBalance() + ")."
            );
        }

        // Check daily transaction limit for the *debiting customer*
        checkDailyTransactionLimit(currentCustomer.getId(), amount);

        // --- Perform Transfer ---
        debitAccount.setBalance(debitAccount.getBalance().subtract(amount));
        creditAccount.setBalance(creditAccount.getBalance().add(amount));

        // Save updated accounts
        accountRepository.save(debitAccount);
        accountRepository.save(creditAccount);

        log.info("Account balances updated: Debit Account {} new balance {}, Credit Account {} new balance {}",
                debitAccount.getAccountNumber(), debitAccount.getBalance(),
                creditAccount.getAccountNumber(), creditAccount.getBalance());

        // --- Create Transaction Record ---
        return createTransactionRecord(debitAccount, creditAccount, amount, TransactionType.TRANSFER);
    }

    private void checkDailyTransactionLimit(Integer customerId, BigDecimal amount) {
        // Query for today's total outgoing transfers from accounts owned by this customer
        BigDecimal dailyTotal = transactionRepository.getTodayTotalTransferAmountByDebitAccountCustomer(customerId, LocalDate.now());

        // Initialize dailyTotal if it's null (no previous transactions today)
        if (dailyTotal == null) {
            dailyTotal = BigDecimal.ZERO;
        }

        if (dailyTotal.add(amount).compareTo(limitProperties.getDailyTransactionLimit()) > 0) {
            throw new LimitExceedsException(
                    "Daily transfer limit of " + limitProperties.getDailyTransactionLimit() +
                            " exceeded. Today's transfers: " + dailyTotal + ", requested: " + amount
            );
        }
    }

    private TransactionDto createTransactionRecord(
            AccountEntity debitAccount,
            AccountEntity creditAccount,
            BigDecimal amount,
            TransactionType type
            ) {

        TransactionEntity transactionEntity = transactionMapper.buildTransactionEntity(
                debitAccount,
                creditAccount,
                amount,
                type
        );
        transactionEntity.setStatus(TransactionStatus.COMPLETED); // Mark as completed since balances are updated
        transactionRepository.save(transactionEntity);
        log.info("Transaction ID {} recorded for transfer of {} from account {} to account {}.",
                transactionEntity.getTransactionId(), amount, debitAccount.getAccountNumber(), creditAccount.getAccountNumber());

        return transactionMapper.mapToTransactionDto(transactionEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByCustomerId(Integer customerId, Integer page, Integer size) {
        log.info("Admin fetching transactions for customer ID: {} (Page: {}, Size: {})", customerId, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        // Find transactions where this customer is either the debit or credit account holder
        Page<TransactionEntity> transactionEntityPage = transactionRepository
                .findByDebitAccount_Customer_IdOrCreditAccount_Customer_Id(customerId, customerId, pageable);
        List<TransactionDto> transactionDtoList = transactionMapper.mapToTransactionDtoList(transactionEntityPage.getContent());
        return new PageImpl<>(transactionDtoList, pageable, transactionEntityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsByCurrentUser(Integer page, Integer size) {
        CustomerEntity currentCustomer = getCurrentCustomer();
        log.info("Customer ID {} fetching their transactions (Page: {}, Size: {})",
                currentCustomer.getId(), page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        // Find transactions where the current customer is either the debit or credit account holder
        Page<TransactionEntity> transactionEntityPage = transactionRepository
                .findByDebitAccount_Customer_IdOrCreditAccount_Customer_Id(currentCustomer.getId(), currentCustomer.getId(), pageable);
        List<TransactionDto> transactionDtoList = transactionMapper.mapToTransactionDtoList(transactionEntityPage.getContent());
        return new PageImpl<>(transactionDtoList, pageable, transactionEntityPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getAllTransactions(Integer page, Integer size) {
        log.info("Admin fetching all transactions (Page: {}, Size: {})", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by("transactionDate").descending());
        Page<TransactionEntity> transactionEntities = transactionRepository.findAll(pageable);
        List<TransactionDto> transactionDtoList = transactionMapper.mapToTransactionDtoList(transactionEntities.getContent());
        return new PageImpl<>(transactionDtoList, pageable, transactionEntities.getTotalElements());
    }
}
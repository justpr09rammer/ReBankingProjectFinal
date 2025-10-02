package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.Exception.AccountNotActiveException;
import com.example.bankingprojectfinal.Exception.NotEnoughFundsException;
import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.AccountStatus;
import com.example.bankingprojectfinal.Model.Enums.CustomerStatus;
import com.example.bankingprojectfinal.Model.Enums.TransactionStatus;
import com.example.bankingprojectfinal.Repository.AccountRepository;
import com.example.bankingprojectfinal.Repository.CustomerRepository;
import com.example.bankingprojectfinal.Repository.TransactionRepository;
import com.example.bankingprojectfinal.Utils.LimitProperties;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime; // Use LocalDateTime for consistency
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionSchedule {
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LimitProperties limitProperties;

    // This scheduled task processes transactions that might have been created as PENDING
    // For immediate card-to-card transfers, the status is set to COMPLETED directly in TransactionServiceImpl
    @Scheduled(cron = "0 0 0 * * *") // Runs every day at midnight
    @Transactional
    public void processPendingTransactions() {
        log.info("Scheduled task: Starting to process pending transactions at {}. (Account-to-Account only)", LocalDateTime.now());

        List<TransactionEntity> pendingTransactions = transactionRepository.findByStatus(TransactionStatus.PENDING);
        if (pendingTransactions.isEmpty()) {
            log.info("No pending transactions found to process.");
            return;
        }

        log.info("Found {} pending transactions to process.", pendingTransactions.size());

        for (TransactionEntity transaction : pendingTransactions) {
            try {
                // Ensure accounts are fetched correctly (they are already part of TransactionEntity)
                AccountEntity debitAccount = transaction.getDebitAccount();
                AccountEntity creditAccount = transaction.getCreditAccount();

                // Perform checks before debiting/crediting
                validateAccountForTransaction(debitAccount, transaction.getAmount(), false);
                validateAccountForTransaction(creditAccount, transaction.getAmount(), true); // No balance check for credit account

                // Update account balances
                debitAccount.setBalance(debitAccount.getBalance().subtract(transaction.getAmount()));
                creditAccount.setBalance(creditAccount.getBalance().add(transaction.getAmount()));

                // Save both accounts
                accountRepository.save(debitAccount);
                accountRepository.save(creditAccount);

                transaction.setStatus(TransactionStatus.COMPLETED);
                transaction.setTransactionDate(LocalDate.from(LocalDateTime.now()));
                transactionRepository.save(transaction);
                log.info("Transaction ID {} successfully processed and marked as COMPLETED. Debited: {}, Credited: {}",
                        transaction.getTransactionId(), debitAccount.getAccountNumber(), creditAccount.getAccountNumber());

                // After successful transaction, check customer's monthly activity for suspicion
                checkCustomerMonthlyActivityForSuspicion(debitAccount.getCustomer());

            } catch (Exception e) {
                log.error("Failed to process transaction ID {} (from debit account {} to credit account {}): {}",
                        transaction.getTransactionId(),
                        transaction.getDebitAccount() != null ? transaction.getDebitAccount().getAccountNumber() : "N/A",
                        transaction.getCreditAccount() != null ? transaction.getCreditAccount().getAccountNumber() : "N/A",
                        e.getMessage());
                transaction.setStatus(TransactionStatus.FAILED);
                transactionRepository.save(transaction);
            }
        }
        log.info("Scheduled task: Finished processing pending transactions.");
    }

    private void validateAccountForTransaction(AccountEntity account, BigDecimal amount, boolean isCredit) {
        if (account == null) {
            throw new IllegalStateException("Account entity is null for transaction processing.");
        }
        if (!account.getStatus().equals(AccountStatus.ACTIVE)) {
            throw new AccountNotActiveException("Account is not active: " + account.getAccountNumber());
        }
        if (!isCredit && account.getBalance().compareTo(amount) < 0) {
            throw new NotEnoughFundsException("Insufficient funds in debit account: " + account.getAccountNumber());
        }
        // Add more specific checks if needed, e.g., credit account limits
    }

    private void checkCustomerMonthlyActivityForSuspicion(CustomerEntity customerEntity) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1); // Last month

        // Get total of all COMPLETED transactions (debit or credit) for this customer in the last month
        BigDecimal monthlyTotal = transactionRepository.getMonthlyTotalByCustomer(customerEntity.getId(), startDate, endDate);

        // Assuming a separate limit for monthly activity or suspicion threshold
        // I'm using `getMonthlySuspicionLimit()` here, which you should define in `LimitProperties`.
        // If you intended to use `dailyTransactionLimit` for this, reconsider its name/purpose.
        if (monthlyTotal != null && monthlyTotal.compareTo(limitProperties.getMonthlyTransactionSuspectLimit()) > 0) {
            customerEntity.setStatus(CustomerStatus.SUSPECTED);
            customerRepository.save(customerEntity);
            log.warn("Customer ID {} detected as SUSPECTED due to monthly transaction total ({}) exceeding suspicion limit ({}).",
                    customerEntity.getId(), monthlyTotal, limitProperties.getMonthlyTransactionSuspectLimit());
        }
    }
}
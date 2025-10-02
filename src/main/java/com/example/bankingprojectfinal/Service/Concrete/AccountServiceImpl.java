package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Account.AccountCreateResponse;
import com.example.bankingprojectfinal.DTOS.Account.AccountResponse;
import com.example.bankingprojectfinal.Exception.*;
import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Enums.AccountStatus;
import com.example.bankingprojectfinal.Model.Enums.CustomerStatus;
import com.example.bankingprojectfinal.Repository.AccountRepository;
import com.example.bankingprojectfinal.Repository.CustomerRepository;
import com.example.bankingprojectfinal.Service.Abstraction.AccountService;
import com.example.bankingprojectfinal.Utils.AccountNumberGenerator;
import com.example.bankingprojectfinal.Utils.LimitProperties;
import com.example.bankingprojectfinal.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final LimitProperties limitProperties;
    private final AccountNumberGenerator accountNumberGenerator;

    // Get current authenticated user from JWT token
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("User not authenticated");
    }

    // Get current customer from authenticated user
    private CustomerEntity getCurrentCustomer() {
        User user = getCurrentUser();
        if (user.getCustomer() == null) {
            throw new IllegalStateException("Customer profile not found for authenticated user");
        }
        return user.getCustomer();
    }

    // ==================== CUSTOMER SELF-SERVICE METHODS ====================

    @Override
    @Transactional
    public AccountCreateResponse createAccountForCurrentUser() {
        CustomerEntity customer = getCurrentCustomer();

        log.info("Creating account for current authenticated customer ID: {}", customer.getId());

        if (customer.getStatus() == CustomerStatus.DELETED || customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Cannot create account for customer with status: " + customer.getStatus());
        }

        int accountCount = accountRepository.countByCustomer_Id(customer.getId());
        if (accountCount >= limitProperties.getMaxAccountCountPerCustomer()) {
            throw new IllegalStateException("Customer has reached the maximum account limit of " + limitProperties.getMaxAccountCountPerCustomer());
        }

        String accountNumber;
        do {
            accountNumber = accountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        AccountEntity createdAccount = AccountEntity.builder()
                .accountNumber(accountNumber)
                .customer(customer)
                .balance(BigDecimal.ZERO)
                .openingDate(LocalDate.now())
                .expireDate(LocalDate.now().plusYears(10))
                .status(AccountStatus.ACTIVE)
                .build();

        AccountEntity savedAccount = accountRepository.save(createdAccount);

        return AccountCreateResponse.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .customerId(savedAccount.getCustomer().getId())
                .openingDate(savedAccount.getOpeningDate())
                .expireDate(savedAccount.getExpireDate())
                .status(savedAccount.getStatus())
                .success(true)
                .message("New Account created successfully.")
                .build();
    }

    @Override
    public List<AccountResponse> getAccountsByCurrentUser() {
        CustomerEntity customer = getCurrentCustomer();
        log.info("Fetching accounts for current authenticated customer ID: {}", customer.getId());

        List<AccountEntity> accounts = accountRepository.findByCustomer_Id(customer.getId());
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    @Override
    @Transactional
    public void activateAccount(String accountNumber) {
        User currentUser = getCurrentUser();
        CustomerEntity currentCustomer = getCurrentCustomer();

        log.info("Customer ID {} attempting to activate account: {}", currentCustomer.getId(), accountNumber);

        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account with number " + accountNumber + " not found."));

        // ✅ SECURITY CHECK: Verify the account belongs to the current customer
        if (!account.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new IllegalStateException("You can only activate your own accounts");
        }

        if (account.getStatus() == AccountStatus.DELETED) {
            throw new IllegalStateException("Cannot activate a deleted account");
        }
        // if it is expired allow reactivation
//        if (account.getStatus() == AccountStatus.EXPIRED) {
//            throw new IllegalStateException("Cannot activate an expired account");
//        }
        if (account.getStatus() == AccountStatus.ACTIVE) {
            throw new IllegalStateException("Account is already active");
        }


        account.setStatus(AccountStatus.ACTIVE);
        account.setExpireDate(LocalDate.now().plusYears(1));
        accountRepository.save(account);
        log.info("Account {} activated successfully by customer ID: {}", accountNumber, currentCustomer.getId());
    }

    // ==================== ADMIN METHODS ====================

    @Override
    @Transactional
    public AccountCreateResponse createAccount(Integer customerId) {
        log.info("Admin creating account for customer ID: {}", customerId);

        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found."));

        if (customer.getStatus() == CustomerStatus.DELETED || customer.getStatus() == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Cannot create account for customer with status: " + customer.getStatus());
        }

        int accountCount = accountRepository.countByCustomer_Id(customerId);
        if (accountCount >= limitProperties.getMaxAccountCountPerCustomer()) {
            throw new IllegalStateException("Customer with ID " + customerId + " has reached the maximum account limit.");
        }

        String accountNumber;
        do {
            accountNumber = accountNumberGenerator.generate();
        } while (accountRepository.existsByAccountNumber(accountNumber));

        AccountEntity createdAccount = AccountEntity.builder()
                .accountNumber(accountNumber)
                .customer(customer)
                .balance(BigDecimal.ZERO)
                .openingDate(LocalDate.now())
                .expireDate(LocalDate.now().plusYears(10))
                .status(AccountStatus.ACTIVE)
                .build();

        AccountEntity savedAccount = accountRepository.save(createdAccount);

        return AccountCreateResponse.builder()
                .accountNumber(savedAccount.getAccountNumber())
                .customerId(savedAccount.getCustomer().getId())
                .openingDate(savedAccount.getOpeningDate())
                .expireDate(savedAccount.getExpireDate())
                .status(savedAccount.getStatus())
                .success(true)
                .message("New Account created successfully.")
                .build();
    }

    @Override
    public List<AccountResponse> getAccountsByCustomerId(Integer customerId) {
        CustomerEntity customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + customerId + " not found."));

        List<AccountEntity> accounts = accountRepository.findByCustomer_Id(customerId);
        return accounts.stream()
                .map(this::mapToAccountResponse)
                .toList();
    }

    @Override
    public Page<AccountResponse> getAllActiveAccounts(Integer page, Integer size) {
        log.info("Fetching all active accounts (page: {}, size: {})", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accountEntityPage = accountRepository.findByStatus(AccountStatus.ACTIVE, pageable);
        return mapToAccountResponsePage(accountEntityPage, pageable);
    }

    @Override
    public Page<AccountResponse> getAllAccounts(Integer page, Integer size) {
        log.info("Fetching all accounts (page: {}, size: {})", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accountEntityPage = accountRepository.findAll(pageable);
        return mapToAccountResponsePage(accountEntityPage, pageable);
    }

    @Override
    public Page<AccountResponse> getAllExpiredAccounts(Integer page, Integer size) {
        log.info("Fetching all expired accounts (page: {}, size: {})", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accountEntityPage = accountRepository.findByStatus(AccountStatus.EXPIRED, pageable);
        return mapToAccountResponsePage(accountEntityPage, pageable);
    }

    @Override
    public Page<AccountResponse> getAllDeletedAccounts(Integer page, Integer size) {
        log.info("Fetching all deleted accounts (page: {}, size: {})", page, size);
        Pageable pageable = PageRequest.of(page, size);
        Page<AccountEntity> accountEntityPage = accountRepository.findByStatus(AccountStatus.DELETED, pageable);
        return mapToAccountResponsePage(accountEntityPage, pageable);
    }


    private AccountResponse mapToAccountResponse(AccountEntity entity) {
        return AccountResponse.builder()
                .accountNumber(entity.getAccountNumber())
                .customerId(entity.getCustomer().getId())
                .balance(entity.getBalance())
                .openingDate(entity.getOpeningDate())
                .expireDate(entity.getExpireDate())
                .status(entity.getStatus())
                .build();
    }

    private Page<AccountResponse> mapToAccountResponsePage(Page<AccountEntity> accountEntityPage, Pageable pageable) {
        List<AccountResponse> accountResponseList = accountEntityPage.getContent().stream()
                .map(this::mapToAccountResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(accountResponseList, pageable, accountEntityPage.getTotalElements());
    }

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void expireAccounts() {
        LocalDate today = LocalDate.now();
        List<AccountEntity> expiredAccounts = accountRepository.findByExpireDateBeforeAndStatusNot(
                today, AccountStatus.EXPIRED);

        for (AccountEntity account : expiredAccounts) {
            account.setStatus(AccountStatus.EXPIRED);
            log.info("Account {} expired automatically", account.getAccountNumber());
        }

        if (!expiredAccounts.isEmpty()) {
            accountRepository.saveAll(expiredAccounts);
        }
    }
}
package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Card.*;
import com.example.bankingprojectfinal.Exception.*;
import com.example.bankingprojectfinal.Model.Entity.AccountEntity;
import com.example.bankingprojectfinal.Model.Entity.CardEntity;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Entity.TransactionEntity;
import com.example.bankingprojectfinal.Model.Enums.*;
import com.example.bankingprojectfinal.Repository.AccountRepository;
import com.example.bankingprojectfinal.Repository.CardRepository;
import com.example.bankingprojectfinal.Repository.TransactionRepository;
import com.example.bankingprojectfinal.Service.Abstraction.CardService;
import com.example.bankingprojectfinal.Utils.CardNumberGenerator;
import com.example.bankingprojectfinal.Utils.LimitProperties;
import com.example.bankingprojectfinal.security.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardServiceImpl implements CardService {
    private final TransactionRepository transactionRepository;
    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final LimitProperties limitProperties;
    private final CardNumberGenerator cardNumberGenerator;

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

    // ==================== CUSTOMER METHODS ====================

    @Override
    @Transactional
    public CardCreateResponse createCardForCurrentUser(String accountNumber) {
        CustomerEntity currentCustomer = getCurrentCustomer();

        log.info("Customer ID {} creating card for account: {}", currentCustomer.getId(), accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        if (!account.getCustomer().getId().equals(currentCustomer.getId())) {
            throw new IllegalStateException("You can only create cards for your own accounts");
        }
        if (currentCustomer.getStatus() == CustomerStatus.BLOCKED) {
            throw new IllegalStateException("Customer is blocked and cannot create new cards");
        }

        return createCardInternal(account);
    }

    @Override
    @Transactional
    public ActivateCardResponse activateCardForCurrentUser(String cardNumber) {
        CustomerEntity currentCustomer = getCurrentCustomer();

        log.info("Customer ID {} activating card: {}", currentCustomer.getId(), cardNumber);

        CardEntity card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardNumber));

        if (!card.getAccount().getCustomer().getId().equals(currentCustomer.getId())) {
            throw new IllegalStateException("You can only activate your own cards");
        }

        return activateCardInternal(card);
    }

    // ==================== ADMIN METHODS ====================

    @Override
    @Transactional
    public CardCreateResponse createCard(CreateCardRequest cardRequest) {
        String accountNumber = cardRequest.getAccountNumber();
        log.info("Admin creating card for account: {}", accountNumber);

        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));

        return createCardInternal(account);
    }

    @Override
    @Transactional
    public ActivateCardResponse activateCard(ActivateCardRequest request) {
        String cardNumber = request.getCardNumber();
        log.info("Admin activating card: {}", cardNumber);

        CardEntity card = cardRepository.findByCardNumber(cardNumber)
                .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardNumber));

        return activateCardInternal(card);
    }

    @Override
    @Transactional
    public DepositCardResponse depositCard(DepositCardRequest request) {
        String cardNumber = request.getCardNumber();
        BigDecimal amount = request.getAmount();
        log.info("Depositing {} to card: {}", amount, cardNumber);

        try {
            CardEntity card = cardRepository.findByCardNumber(cardNumber)
                    .orElseThrow(() -> new CardNotFoundException("Card not found: " + cardNumber));

            if (!card.getStatus().equals(CardStatus.ACTIVE)) {
                throw new InvalidCardStatusException("Card is not active. Current status: " + card.getStatus());
            }

            AccountEntity account = card.getAccount();
            if (account == null) {
                throw new AccountNotFoundException("Account linked to card not found");
            }
            if (!account.getStatus().equals(AccountStatus.ACTIVE)) {
                throw new InvalidAccountStatusException("Account is not active. Current status: " + account.getStatus());
            }

            BigDecimal currentBalance = account.getBalance();
            BigDecimal newBalance = currentBalance.add(amount);
            account.setBalance(newBalance);
            accountRepository.save(account);
            TransactionEntity transaction = TransactionEntity.builder()
                    .debitAccount(account)
                    .creditAccount(account)           // The actual account being credited
                    .amount(amount)
                    .transactionType(TransactionType.DEPOSIT)
                    .transactionDate(LocalDate.from(LocalDateTime.now())) // Use LocalDateTime
                    .status(TransactionStatus.COMPLETED)
                    .build();

            TransactionEntity savedTransaction = transactionRepository.save(transaction);


            return DepositCardResponse.builder()
                    .success(true)
                    .message("Successfully deposited " + amount + " to account " + account.getAccountNumber())
                    .cardNumber(cardNumber)
                    .depositedAmount(amount)
                    .newAccountBalance(newBalance)
//                    .transactionId(savedTransaction.getTransactionId())
                    .transactionTimestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Deposit failed for card {}: {}", cardNumber, e.getMessage());
            return DepositCardResponse.builder()
                    .success(false)
                    .message("Deposit failed: " + e.getMessage())
                    .cardNumber(cardNumber)
                    .depositedAmount(amount)
                    .newAccountBalance(BigDecimal.ZERO)
                    .transactionTimestamp(LocalDateTime.now())
                    .build();
        }
    }

    // ==================== PAGINATED ADMIN QUERIES ====================

    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> getAllActiveCards(Integer page, Integer size) {
        log.info("Fetching all active cards");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("issueDate").descending());
        return cardRepository.findByStatus(CardStatus.ACTIVE, pageable).map(this::convertToCardDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> getAllCards(Integer page, Integer size) {
        log.info("Fetching all cards");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("issueDate").descending());
        return cardRepository.findAll(pageable).map(this::convertToCardDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardDto> getAllExpiredCards(Integer page, Integer size) {
        log.info("Fetching all expired cards");
        PageRequest pageable = PageRequest.of(page, size, Sort.by("issueDate").descending());
        return cardRepository.findByStatus(CardStatus.EXPIRED, pageable).map(this::convertToCardDto);
    }

    // ==================== SIMPLE LIST QUERIES ====================

    @Override
    @Transactional(readOnly = true)
    public List<CardDto> getCardsByCurrentUser() {
        CustomerEntity currentCustomer = getCurrentCustomer();
        log.info("Fetching cards for current customer ID: {}", currentCustomer.getId());
        List<CardEntity> cards = cardRepository.findByAccount_Customer_Id(currentCustomer.getId());
        return cards.stream().map(this::convertToCardDto).toList();
    }

    @Override
    public List<CardDto> getCardsByAccountForCurrentUser(String accountNumber) {
        return List.of();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardDto> getCardsByAccount(String accountNumber) {
        log.info("Fetching cards for account: {}", accountNumber);
        if (!accountRepository.existsByAccountNumber(accountNumber)) {
            return List.of();
        }
        List<CardEntity> cards = cardRepository.findByAccount_AccountNumber(accountNumber);
        return cards.stream().map(this::convertToCardDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardDto> getCardsByCustomerId(Integer customerId) {
        log.info("Fetching cards for customer ID: {}", customerId);
        List<CardEntity> cards = cardRepository.findByAccount_Customer_Id(customerId);
        return cards.stream().map(this::convertToCardDto).toList();
    }




    // ==================== HELPER METHODS ====================

    private CardCreateResponse createCardInternal(AccountEntity account) {
        try {
            if (!account.getStatus().equals(AccountStatus.ACTIVE)) {
                throw new InvalidAccountStatusException(
                        "Card cannot be created for account in " + account.getStatus() + " status"
                );
            }
            Integer currentCards = cardRepository.countByAccount_AccountNumber(account.getAccountNumber());

            if (currentCards >= limitProperties.getMaxCardCountPerAccount()) {
                throw new MaximumCardCountException(
                        "Account has reached maximum card limit of " + limitProperties.getMaxCardCountPerAccount()
                );
            }

            String cardNumber;
            do {
                cardNumber = cardNumberGenerator.generate();
            } while (cardRepository.existsByCardNumber(cardNumber));

            CardEntity cardEntity = CardEntity.builder()
                    .cardNumber(cardNumber)
                    .account(account)
                    .issueDate(LocalDate.now())
                    .expireDate(LocalDate.now().plusYears(5))
                    .status(CardStatus.ACTIVE)
                    .build();

            CardEntity savedCard = cardRepository.save(cardEntity);
            return CardCreateResponse.builder()
                    .success(true)
                    .message("Card created successfully")
                    .card(convertToCardDto(savedCard))
                    .build();

        } catch (Exception e) {
            log.error("Card creation failed: {}", e.getMessage());
            return CardCreateResponse.builder()
                    .success(false)
                    .message("Card creation failed: " + e.getMessage())
                    .build();
        }
    }

    private ActivateCardResponse activateCardInternal(CardEntity card) {
        try {
            if (card.getStatus().equals(CardStatus.ACTIVE)) {
                throw new InvalidCardActivationException(
                        "Card has already been activated. Current status: " + card.getStatus()
                );
            }

            LocalDate previousExpireDate = card.getExpireDate();
            card.setStatus(CardStatus.ACTIVE);
            card.setExpireDate(LocalDate.now().plusYears(5));

            CardEntity updatedCard = cardRepository.save(card);

            return ActivateCardResponse.builder()
                    .success(true)
                    .message("Card activated successfully")
                    .previousExpireDate(previousExpireDate)
                    .card(convertToCardDto(updatedCard))
                    .build();

        } catch (Exception e) {
            log.error("Card activation failed: {}", e.getMessage());
            return ActivateCardResponse.builder()
                    .success(false)
                    .message("Card activation failed: " + e.getMessage())
                    .build();
        }
    }

    private CardDto convertToCardDto(CardEntity cardEntity) {
        if (cardEntity == null) return null;

        String accountNumber = null;
        BigDecimal accountBalance = BigDecimal.ZERO;

        if (cardEntity.getAccount() != null) {
            accountNumber = cardEntity.getAccount().getAccountNumber();
            accountBalance = cardEntity.getAccount().getBalance();
        }

        return CardDto.builder()
                .cardNumber(cardEntity.getCardNumber())
                .accountNumber(accountNumber)
                .issueDate(cardEntity.getIssueDate())
                .expireDate(cardEntity.getExpireDate())
                .status(cardEntity.getStatus())
                .build();
    }
}

package com.example.bankingprojectfinal.Service.Abstraction;

import com.example.bankingprojectfinal.DTOS.Card.*;
import org.springframework.data.domain.Page;

import java.util.List;

public interface CardService {
    // Admin methods
    CardCreateResponse createCard(CreateCardRequest cardRequest);
    ActivateCardResponse activateCard(ActivateCardRequest request);
    DepositCardResponse depositCard(DepositCardRequest request);
    List<CardDto> getCardsByAccount(String accountNumber);
    List<CardDto> getCardsByCustomerId(Integer customerId);
    Page<CardDto> getAllActiveCards(Integer page, Integer size);
    Page<CardDto> getAllExpiredCards(Integer page, Integer size);
    Page<CardDto> getAllCards(Integer page, Integer size);

    // Customer self-service methods
    CardCreateResponse createCardForCurrentUser(String accountNumber);
    ActivateCardResponse activateCardForCurrentUser(String cardNumber);
    List<CardDto> getCardsByCurrentUser();
    List<CardDto> getCardsByAccountForCurrentUser(String accountNumber);
}
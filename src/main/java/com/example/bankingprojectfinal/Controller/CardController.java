package com.example.bankingprojectfinal.Controller;

import com.example.bankingprojectfinal.DTOS.Card.*;
import com.example.bankingprojectfinal.Service.Abstraction.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
@Tag(name = "Card Management", description = "APIs for managing bank cards (create, activate, deposit, view)")
public class CardController {

    private final CardService cardService;

    // ==================== ADMIN ENDPOINTS ====================

    @Operation(summary = "Create a new card", description = "Creates a new bank card for a specified account. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data or business rule violation"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CardCreateResponse createCard(
            @Parameter(description = "Details for card creation", required = true)
            @Valid @RequestBody CreateCardRequest cardRequest
    ) {
        return cardService.createCard(cardRequest);
    }

    @Operation(summary = "Activate a card", description = "Changes the status of a NEW card to ACTIVE. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card activated successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "400", description = "Invalid card status for activation"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ActivateCardResponse activateCard(
            @Parameter(description = "Request to activate a card", required = true)
            @Valid @RequestBody ActivateCardRequest request
    ) {
        return cardService.activateCard(request);
    }

    @Operation(summary = "Deposit funds via card", description = "Adds funds to the account linked to an ACTIVE card. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deposit successful"),
            @ApiResponse(responseCode = "404", description = "Card or linked account not found"),
            @ApiResponse(responseCode = "400", description = "Invalid deposit amount or card/account not active"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/deposit")
    @PreAuthorize("hasRole('ADMIN')")
    public DepositCardResponse depositCard(
            @Parameter(description = "Deposit request details", required = true)
            @Valid @RequestBody DepositCardRequest request
    ) {
        return cardService.depositCard(request);
    }

    @Operation(summary = "Get cards by account number", description = "Retrieves cards associated with a specific account number. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards for the account"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/byAccount/{accountNumber}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CardDto> getCardsByAccount(
            @Parameter(description = "Account number to retrieve cards for", required = true, example = "ACC123456789")
            @PathVariable String accountNumber
    ) {
        return cardService.getCardsByAccount(accountNumber);
    }

    @Operation(summary = "Get cards by customer ID", description = "Retrieves cards associated with a specific customer. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards for the customer"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/byCustomer/{customerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CardDto> getCardsByCustomerId(
            @Parameter(description = "ID of the customer to retrieve cards for", required = true, example = "1")
            @PathVariable Integer customerId
    ) {
        return cardService.getCardsByCustomerId(customerId);
    }

    @Operation(summary = "Get all active cards", description = "Retrieves a paginated list of all ACTIVE cards in the system. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of active cards"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getAllActiveCards(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return cardService.getAllActiveCards(page, size);
    }

    @Operation(summary = "Get all cards", description = "Retrieves a paginated list of all cards in the system. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of all cards"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getAllCards(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return cardService.getAllCards(page, size);
    }

    @Operation(summary = "Get all expired cards", description = "Retrieves a paginated list of all EXPIRED cards. Admin only.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of expired cards"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/expired")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CardDto> getAllExpiredCards(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return cardService.getAllExpiredCards(page, size);
    }

    // ==================== CUSTOMER SELF-SERVICE ENDPOINTS ====================

    @Operation(summary = "Create card for my account", description = "Creates a new card for the authenticated customer's account.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Card created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or business rule violation"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this account"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/my-cards/{accountNumber}")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CardCreateResponse createCardForCurrentUser(
            @Parameter(description = "Account number to create card for", required = true, example = "ACC123456789")
            @PathVariable String accountNumber
    ) {
        return cardService.createCardForCurrentUser(accountNumber);
    }

    @Operation(summary = "Activate my card", description = "Activates a card owned by the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card activated successfully"),
            @ApiResponse(responseCode = "404", description = "Card not found"),
            @ApiResponse(responseCode = "400", description = "Invalid card status for activation"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to activate this card"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PutMapping("/my-cards/activate/{cardNumber}")
    @PreAuthorize("hasRole('USER')")
    public ActivateCardResponse activateCardForCurrentUser(
            @Parameter(description = "Card number to activate", required = true, example = "1234567890123456")
            @PathVariable String cardNumber
    ) {
        return cardService.activateCardForCurrentUser(cardNumber);
    }

    @Operation(summary = "Get my cards", description = "Retrieves all cards owned by the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-cards")
    @PreAuthorize("hasRole('USER')")
    public List<CardDto> getCardsByCurrentUser() {
        return cardService.getCardsByCurrentUser();
    }

    @Operation(summary = "Get my cards by account", description = "Retrieves cards for a specific account owned by the authenticated customer.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards for the account"),
            @ApiResponse(responseCode = "404", description = "Account not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Not authorized to access this account"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-cards/account/{accountNumber}")
    @PreAuthorize("hasRole('USER')")
    public List<CardDto> getCardsByAccountForCurrentUser(
            @Parameter(description = "Account number to retrieve cards for", required = true, example = "ACC123456789")
            @PathVariable String accountNumber
    ) {
        return cardService.getCardsByAccountForCurrentUser(accountNumber);
    }
}
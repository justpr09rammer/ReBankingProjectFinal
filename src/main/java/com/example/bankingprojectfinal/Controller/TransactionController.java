package com.example.bankingprojectfinal.Controller;

import com.example.bankingprojectfinal.DTOS.Transaction.TransactionDto;
import com.example.bankingprojectfinal.DTOS.Transaction.TransferRequest;
import com.example.bankingprojectfinal.Service.Abstraction.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement; // For securing endpoints
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity; // Use ResponseEntity for more control
import org.springframework.security.access.prepost.PreAuthorize; // For role-based authorization
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for initiating and viewing bank transactions")
@SecurityRequirement(name = "Bearer Authentication") // Applies JWT security globally for this controller
public class TransactionController {

    private final TransactionService transactionService;

    // --- Customer-facing Endpoints ---

    @Operation(summary = "Initiate a new card-to-card bank transfer",
            description = "Allows an authenticated customer to perform a transfer between two card-linked accounts. Transactions are processed immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction initiated successfully and completed"),
            @ApiResponse(responseCode = "400", description = "Invalid request data (e.g., card format, amount <= 0) or business rule violation (e.g., insufficient funds, card not active, daily limit exceeded, transferring to own card)"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Customer cannot transfer from another's card or is blocked"),
            @ApiResponse(responseCode = "404", description = "Debit or credit card not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 Created on success
    @PreAuthorize("hasRole('CUSTOMER')") // Only customers can initiate transfers
    public TransactionDto initiateTransfer(
            @Parameter(description = "Details for the card-to-card transfer, including debit/credit card numbers, amount, and an optional description.", required = true)
            @Valid @RequestBody TransferRequest request
    ) {
        log.info("Received transfer request from current user: DebitCard={}, CreditCard={}, Amount={}, Description={}",
                request.getDebitCardNumber(), request.getCreditCardNumber(), request.getAmount());

        // The transactionService.transfer method now expects the description
        TransactionDto result = transactionService.transfer(
                request.getDebitCardNumber(),
                request.getCreditCardNumber(),
                request.getAmount()
        );
        log.info("Transfer initiated successfully with transaction ID: {}", result.getTransactionId());
        return result;
    }

    @Operation(summary = "Get transactions for the currently authenticated user",
            description = "Retrieves a paginated list of transactions where the authenticated user is either the sender or receiver.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of transactions for the current user"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden: Not a customer or customer profile not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')") // Only customers can view their own transactions
    public Page<TransactionDto> getTransactionsByCurrentUser(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        log.info("Fetching transactions for current authenticated customer (Page: {}, Size: {})", page, size);
        return transactionService.getTransactionsByCurrentUser(page, size);
    }

    // --- Admin-only Endpoints ---

    @Operation(summary = "ADMIN: Get transactions by customer ID",
            description = "Retrieves a paginated list of transactions associated with a specific customer ID. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of transactions for the customer"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden: User does not have ADMIN role"),
            @ApiResponse(responseCode = "404", description = "Customer ID not found (if implemented in service)"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/admin/byCustomer/{customerId}") // Admin-specific path
    @PreAuthorize("hasRole('ADMIN')") // Only admins can access this
    public Page<TransactionDto> getTransactionsByCustomerId(
            @Parameter(description = "ID of the customer to retrieve transactions for", required = true, example = "101")
            @PathVariable Integer customerId,
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        log.info("Admin fetching transactions for customer ID: {} (Page: {}, Size: {})", customerId, page, size);
        return transactionService.getTransactionsByCustomerId(customerId, page, size);
    }

    @Operation(summary = "ADMIN: Get all transactions",
            description = "Retrieves a paginated list of all bank transactions in the system. Requires ADMIN role.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of all transactions"),
            @ApiResponse(responseCode = "401", description = "Unauthorized: Authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden: User does not have ADMIN role"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/admin/all") // Admin-specific path
    @PreAuthorize("hasRole('ADMIN')") // Only admins can access this
    public Page<TransactionDto> getAllTransactions(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        log.info("Admin fetching all transactions (Page: {}, Size: {})", page, size);
        return transactionService.getAllTransactions(page, size);
    }
}
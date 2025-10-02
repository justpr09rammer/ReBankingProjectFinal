package com.example.bankingprojectfinal.Controller;

import com.example.bankingprojectfinal.DTOS.Customer.CustomerCreateRequest;
import com.example.bankingprojectfinal.DTOS.Customer.CustomerResponse;
import com.example.bankingprojectfinal.Service.Abstraction.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "APIs for managing bank customers")
public class CustomerController {

    private final CustomerService customerService;

    @Operation(summary = "Create customer profile for current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer profile created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Customer profile already exists or duplicate FIN/phone"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/my-profile")
    @PreAuthorize("hasRole('USER')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomerProfile(@RequestBody CustomerCreateRequest request) {
        return customerService.createCustomerForCurrentUser(request);
    }

    @Operation(summary = "Get my customer profile")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved customer profile"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "Customer profile not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/my-profile")
    @PreAuthorize("hasRole('USER')")
    public CustomerResponse getMyCustomerProfile() {
        return customerService.getCustomerByCurrentUser();
    }


    @Operation(summary = "Create a new customer (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "409", description = "Customer with provided FIN code or phone number already exists"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @PostMapping("/admin/create")  // ← CHANGED: Added specific path
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerResponse createCustomer(@RequestBody CustomerCreateRequest request) {
        return customerService.createCustomer(request);
    }

    @Operation(summary = "Get all customers (Admin)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of customers"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/admin/all")  // ← CHANGED: Added specific path
    @PreAuthorize("hasRole('ADMIN')")
    public Page<CustomerResponse> getAllCustomers(
            @Parameter(description = "Page number (0-indexed)", example = "0")
            @RequestParam(defaultValue = "0", required = false) Integer page,
            @Parameter(description = "Number of items per page", example = "10")
            @RequestParam(defaultValue = "10", required = false) Integer size
    ) {
        return customerService.getAllCustomers(page, size);
    }
}
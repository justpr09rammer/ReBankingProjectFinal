package com.example.bankingprojectfinal.Service.Abstraction;

import com.example.bankingprojectfinal.DTOS.Customer.CustomerCreateRequest;
import com.example.bankingprojectfinal.DTOS.Customer.CustomerResponse;
import org.springframework.data.domain.Page;

public interface CustomerService {
    // For authenticated users to create their own customer profile
    CustomerResponse createCustomerForCurrentUser(CustomerCreateRequest request);
    CustomerResponse getCustomerByCurrentUser();
    // Admin methods
    CustomerResponse createCustomer(CustomerCreateRequest customerCreateRequest);
    Page<CustomerResponse> getAllCustomers(Integer page, Integer size);
}
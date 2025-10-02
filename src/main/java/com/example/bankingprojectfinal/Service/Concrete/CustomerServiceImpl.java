package com.example.bankingprojectfinal.Service.Concrete;

import com.example.bankingprojectfinal.DTOS.Customer.CustomerCreateRequest;
import com.example.bankingprojectfinal.DTOS.Customer.CustomerResponse;
import com.example.bankingprojectfinal.Exception.DuplicateResourceException;
import com.example.bankingprojectfinal.Model.Entity.CustomerEntity;
import com.example.bankingprojectfinal.Model.Enums.CustomerStatus;
import com.example.bankingprojectfinal.Repository.CustomerRepository;
import com.example.bankingprojectfinal.Service.Abstraction.CustomerService;
import com.example.bankingprojectfinal.security.model.User;
import com.example.bankingprojectfinal.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    // This method extracts the current user from JWT token in Security Context
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new IllegalStateException("User not authenticated");
    }

    // STEP 3: Create Customer profile for the current authenticated user
    @Override
    @Transactional
    public CustomerResponse createCustomerForCurrentUser(CustomerCreateRequest request) {
        User currentUser = getCurrentUser(); // Gets user from JWT token

        // Check if customer profile already exists
        if (currentUser.getCustomer() != null) {
            throw new IllegalStateException("Customer profile already exists for this user");
        }

        log.info("Creating customer profile for user ID: {}", currentUser.getId());

        // Validate for uniqueness
        if (customerRepository.existsByFinCode(request.getFinCode())) {
            throw new DuplicateResourceException("Customer with FIN code " + request.getFinCode() + " already exists.");
        }
        if (customerRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new DuplicateResourceException("Customer with phone number " + request.getPhoneNumber() + " already exists.");
        }

        // Create Customer and link to User
        CustomerEntity customer = CustomerEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .birthDate(request.getBirthDate())
                .finCode(request.getFinCode())
                .phoneNumber(request.getPhoneNumber())
                .registrationDate(LocalDate.now())
                .user(currentUser)
                .status(CustomerStatus.REGULAR)
                .build();

        CustomerEntity savedCustomer = customerRepository.save(customer);

        currentUser.setCustomer(savedCustomer);
        userRepository.save(currentUser);

        log.info("Customer profile created with ID: {} for user ID: {}", savedCustomer.getId(), currentUser.getId());

        return mapToCustomerResponse(savedCustomer);
    }

    @Override
    public CustomerResponse getCustomerByCurrentUser() {
        User currentUser = getCurrentUser(); // Gets user from JWT token
        if (currentUser.getCustomer() == null) {
            throw new IllegalStateException("Customer profile not found");
        }

        return mapToCustomerResponse(currentUser.getCustomer());
    }

    // Admin method - create customer without user association
    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest customerCreateRequest) {
        log.info("Attempting to create a new customer with FIN: {}", customerCreateRequest.getFinCode());

        // Validate for uniqueness before creating
        if (customerRepository.existsByFinCode(customerCreateRequest.getFinCode())) {
            log.warn("Customer creation failed: Duplicate FIN code found: {}", customerCreateRequest.getFinCode());
            throw new DuplicateResourceException("Customer with FIN code " + customerCreateRequest.getFinCode() + " already exists.");
        }
        if (customerRepository.existsByPhoneNumber(customerCreateRequest.getPhoneNumber())) {
            log.warn("Customer creation failed: Duplicate phone number found: {}", customerCreateRequest.getPhoneNumber());
            throw new DuplicateResourceException("Customer with phone number " + customerCreateRequest.getPhoneNumber() + " already exists.");
        }

        try {
            CustomerEntity customerEntity = CustomerEntity.builder()
                    .firstName(customerCreateRequest.getFirstName())
                    .lastName(customerCreateRequest.getLastName())
                    .birthDate(customerCreateRequest.getBirthDate())
                    .finCode(customerCreateRequest.getFinCode())
                    .phoneNumber(customerCreateRequest.getPhoneNumber())
                    .registrationDate(LocalDate.now())
                    .status(CustomerStatus.REGULAR)
                    // No user association for admin-created customers
                    .build();
            CustomerEntity savedCustomer = customerRepository.save(customerEntity);
            log.info("Customer created successfully with ID: {} and FIN: {}", savedCustomer.getId(), savedCustomer.getFinCode());

            return mapToCustomerResponse(savedCustomer);

        } catch (DuplicateResourceException e) {
            throw e;
        } catch (Exception e) {
            log.error("An unexpected error occurred during customer creation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create customer due to an internal error.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Integer page, Integer size) {
        log.info("Fetching all customers (page: {}, size: {})", page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        Page<CustomerEntity> customerEntities = customerRepository.findAll(pageable);
        return customerEntities.map(this::mapToCustomerResponse);
    }

    private CustomerResponse mapToCustomerResponse(CustomerEntity customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .firstName(customer.getFirstName())
                .lastName(customer.getLastName())
                .birthDate(customer.getBirthDate())
                .finCode(customer.getFinCode())
                .phoneNumber(customer.getPhoneNumber())
                .registrationDate(customer.getRegistrationDate())
                .status(customer.getStatus())
                .userId(customer.getUser() != null ? customer.getUser().getId() : null)
                .build();
    }
}
package com.example.bankingprojectfinal.Service.Abstraction;

import com.example.bankingprojectfinal.DTOS.Account.AccountCreateResponse;
import com.example.bankingprojectfinal.DTOS.Account.AccountResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface AccountService {
    // Admin methods
    AccountCreateResponse createAccount(Integer customerId);
    void activateAccount(String accountNumber);
    Page<AccountResponse> getAllActiveAccounts(Integer page, Integer size);
    Page<AccountResponse> getAllAccounts(Integer page, Integer size);
    Page<AccountResponse> getAllExpiredAccounts(Integer page, Integer size);
    Page<AccountResponse> getAllDeletedAccounts(Integer page, Integer size);
    List<AccountResponse> getAccountsByCustomerId(Integer customerId);

    // Customer self-service methods
    AccountCreateResponse createAccountForCurrentUser();
    List<AccountResponse> getAccountsByCurrentUser();
}
package com.example.bankingprojectfinal.Service.Abstraction;

import com.example.bankingprojectfinal.DTOS.Account.AccountCreateResponse;
import com.example.bankingprojectfinal.DTOS.Account.AccountResponse;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface AccountService {
	AccountCreateResponse createAccount(Integer customerId);
	void activateAccount(String accountNumber);
	void depositAccount(String accountNumber, BigDecimal amount);
	Page<AccountResponse> getAccountsByCustomerId(Integer customerId, Integer page, Integer size);
	Page<AccountResponse> getAllActiveAccounts(Integer page, Integer size);
	Page<AccountResponse> getAllAccounts(Integer page, Integer size);
	Page<AccountResponse> getAllExpiredAccounts(Integer page, Integer size);
}

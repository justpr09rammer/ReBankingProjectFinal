package com.example.bankingprojectfinal.DTOS.Customer;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CustomerCreateRequest {
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String finCode;
    private String phoneNumber;
}
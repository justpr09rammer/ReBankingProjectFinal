package com.example.bankingprojectfinal.DTOS.Customer;

import com.example.bankingprojectfinal.Model.Enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class CustomerResponse {
    private Integer id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private String finCode;
    private String phoneNumber;
    private LocalDate registrationDate;
    private CustomerStatus status;
    private Long userId;
}
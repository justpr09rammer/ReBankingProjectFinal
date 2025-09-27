package com.example.bankingprojectfinal.DTOS.Transaction;// package com.example.bankingprojectfinal.DTOS.Transaction;
// You might want to put this in a dedicated request DTO package, e.g.,
// package com.example.bankingprojectfinal.DTOS.Transaction.Request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request DTO for initiating a bank transfer")
public class TransferRequest {

    @NotBlank(message = "Debit account number cannot be empty")
    private String debitCardNumber;

    private String creditCardNumber;

    @NotNull(message = "Amount cannot be null")
    @DecimalMin(value = "0.01", inclusive = true, message = "Amount must be greater than zero")
    @Schema(description = "The amount to transfer", example = "100.50")
    private BigDecimal amount;
}
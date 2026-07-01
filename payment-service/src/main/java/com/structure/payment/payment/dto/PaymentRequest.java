package com.structure.payment.payment.dto;


import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotBlank
    @Pattern(regexp = "^[a-fA-F0-9\\-]{36}$",
            message = "accountId must be a valid UUID")
    private String accountId;

    @NotNull
    @Min(value = 100,           message = "Minimum amount is ₦1 (100 kobo)")
    @Max(value = 1_000_000_000, message = "Maximum amount is ₦10,000,000")
    private BigDecimal amount;

    @NotBlank
    @Size(min = 1, max = 200)
    private String description;

    @NotBlank
    @Pattern(regexp = "^\\d{10}$",
            message = "beneficiaryAccount must be 10 digits (NUBAN)")
    private String beneficiaryAccount;

    @NotBlank @Size(max = 100)
    private String beneficiaryBank;

    @NotBlank @Size(max = 150)
    private String beneficiaryName;
}

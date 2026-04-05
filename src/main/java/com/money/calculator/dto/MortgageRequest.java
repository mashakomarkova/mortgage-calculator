package com.money.calculator.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class MortgageRequest {

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal annualInterestRate;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal downPayment;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal propertyPrice;

    @NotNull
    @Min(1)
    @Max(value = 35, message = "Loan term cannot exceed 35 years")
    private Integer loanTermYears;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal extraMonthlyPayment;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal monthlySavingsWithoutMortgage;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal annualPropertyGrowthRate;

    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal annualInvestmentReturnRate;

    private boolean calculateCapitalWithoutMortgage;
    private boolean includeExtraMonthlyPayment;
    private boolean includeAnnualPropertyGrowthRate;


}

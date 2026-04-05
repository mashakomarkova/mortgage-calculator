package com.money.calculator.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Getter
@RequiredArgsConstructor
public class MortgagePaymentRow {

    private final int monthNumber;
    private final BigDecimal baseMonthlyPayment;
    private final BigDecimal extraPayment;
    private final BigDecimal totalPayment;
    private final BigDecimal interestAmount;
    private final BigDecimal principalAmount;
    private final BigDecimal remainingBalance;
    private final BigDecimal propertyCurrentValue;
    private final BigDecimal savingsCurrentValue;
    private final BigDecimal netWorthWithoutMortgage;
    private final BigDecimal netWorthWithMortgage;
}
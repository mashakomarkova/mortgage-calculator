package com.money.calculator.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class MortgageResult {

    private final BigDecimal loanAmount;
    private final BigDecimal monthlyPayment;
    private final BigDecimal extraMonthlyPayment;
    private final int originalLoanTermMonths;
    private final int actualLoanTermMonths;
    private final int savedMonths;
    private final int savedYearsPart;
    private final int savedRemainingMonthsPart;
    private boolean calculateCapitalWithoutMortgage;
    private final BigDecimal finalNetWorthWithoutMortgage;
    private final BigDecimal finalNetWorthWithMortgage;
    private final List<MortgagePaymentRow> paymentSchedule;
    private BigDecimal totalPaidIncludingBankFee;
    private BigDecimal totalInterestPaid;
}
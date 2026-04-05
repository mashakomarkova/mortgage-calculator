package com.money.calculator.service;

import com.money.calculator.dto.MortgagePaymentRow;
import com.money.calculator.dto.MortgageRequest;
import com.money.calculator.dto.MortgageResult;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@NoArgsConstructor
public class MortgageService {

    private static final MathContext MC = new MathContext(20, RoundingMode.HALF_UP);
    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE = BigDecimal.ONE;

    public MortgageResult calculate(MortgageRequest request) {
        BigDecimal loanAmount = request.getPropertyPrice().subtract(request.getDownPayment());

        if (loanAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Сумма займа должна быть больше 0");
        }

        boolean calculateCapitalWithoutMortgage = request.isCalculateCapitalWithoutMortgage();

        BigDecimal monthlySavingsWithoutMortgage = ZERO;
        BigDecimal monthlyInvestmentRate = BigDecimal.ZERO;

        if (calculateCapitalWithoutMortgage) {
            monthlySavingsWithoutMortgage = defaultIfNull(request.getMonthlySavingsWithoutMortgage());
            monthlyInvestmentRate = toMonthlyRate(defaultIfNull(request.getAnnualInvestmentReturnRate()));
        }

        int originalLoanTermMonths = request.getLoanTermYears() * 12;

        BigDecimal monthlyMortgageRate = toMonthlyRate(request.getAnnualInterestRate());
        BigDecimal monthlyPropertyGrowthRate = BigDecimal.ZERO;
        if (request.isIncludeAnnualPropertyGrowthRate()) {
            monthlyPropertyGrowthRate = toMonthlyRate(request.getAnnualPropertyGrowthRate());
        }

        BigDecimal baseMonthlyPayment = calculateMonthlyPayment(loanAmount, monthlyMortgageRate, originalLoanTermMonths)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal extraMonthlyPayment = ZERO;
        if (request.isIncludeExtraMonthlyPayment()) {
            extraMonthlyPayment = defaultIfNull(request.getExtraMonthlyPayment()).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal propertyPrice = request.getPropertyPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal downPayment = request.getDownPayment().setScale(2, RoundingMode.HALF_UP);

        List<MortgagePaymentRow> schedule = buildPaymentSchedule(
                propertyPrice,
                downPayment,
                monthlySavingsWithoutMortgage.setScale(2, RoundingMode.HALF_UP),
                loanAmount,
                monthlyMortgageRate,
                monthlyPropertyGrowthRate,
                monthlyInvestmentRate,
                baseMonthlyPayment,
                extraMonthlyPayment,
                calculateCapitalWithoutMortgage
        );
        BigDecimal totalPaidToBank = schedule.stream()
                .map(MortgagePaymentRow::getTotalPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInterestPaid = schedule.stream()
                .map(MortgagePaymentRow::getInterestAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        int actualLoanTermMonths = schedule.size();
        int savedMonths = Math.max(0, originalLoanTermMonths - actualLoanTermMonths);
        int savedYearsPart = savedMonths / 12;
        int savedRemainingMonthsPart = savedMonths % 12;

        MortgagePaymentRow lastRow = schedule.get(schedule.size() - 1);

        return new MortgageResult(
                loanAmount.setScale(2, RoundingMode.HALF_UP),
                baseMonthlyPayment,
                extraMonthlyPayment,
                originalLoanTermMonths,
                actualLoanTermMonths,
                savedMonths,
                savedYearsPart,
                savedRemainingMonthsPart,
                calculateCapitalWithoutMortgage,
                lastRow.getNetWorthWithoutMortgage(),
                lastRow.getNetWorthWithMortgage(),
                schedule,
                totalPaidToBank,
                totalInterestPaid
        );
    }

    private BigDecimal defaultIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal toMonthlyRate(BigDecimal annualRatePercent) {
        return annualRatePercent
                .divide(ONE_HUNDRED, 10, RoundingMode.HALF_UP)
                .divide(MONTHS_IN_YEAR, 10, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal loanAmount, BigDecimal monthlyRate, int totalMonths) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return loanAmount.divide(BigDecimal.valueOf(totalMonths), 10, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusRPowerN = ONE.add(monthlyRate).pow(totalMonths, MC);
        BigDecimal numerator = loanAmount.multiply(monthlyRate, MC).multiply(onePlusRPowerN, MC);
        BigDecimal denominator = onePlusRPowerN.subtract(ONE, MC);

        return numerator.divide(denominator, 10, RoundingMode.HALF_UP);
    }

    private List<MortgagePaymentRow> buildPaymentSchedule(BigDecimal initialPropertyPrice,
                                                          BigDecimal downPayment,
                                                          BigDecimal monthlySavingsWithoutMortgage,
                                                          BigDecimal loanAmount,
                                                          BigDecimal monthlyMortgageRate,
                                                          BigDecimal monthlyPropertyGrowthRate,
                                                          BigDecimal monthlyInvestmentRate,
                                                          BigDecimal baseMonthlyPayment,
                                                          BigDecimal extraMonthlyPayment,
                                                          boolean calculateCapitalWithoutMortgage) {
        List<MortgagePaymentRow> schedule = new ArrayList<>();

        BigDecimal remainingBalance = loanAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal propertyCurrentValue = initialPropertyPrice;
        BigDecimal savingsCurrentValue = calculateCapitalWithoutMortgage ? downPayment : null;
        int month = 1;

        while (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {
            propertyCurrentValue = propertyCurrentValue
                    .multiply(ONE.add(monthlyPropertyGrowthRate), MC)
                    .setScale(2, RoundingMode.HALF_UP);

            if (calculateCapitalWithoutMortgage) {
                savingsCurrentValue = savingsCurrentValue
                        .multiply(ONE.add(monthlyInvestmentRate), MC)
                        .add(monthlySavingsWithoutMortgage, MC)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal interestAmount = remainingBalance.multiply(monthlyMortgageRate, MC)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal plannedPrincipal = baseMonthlyPayment.subtract(interestAmount)
                    .setScale(2, RoundingMode.HALF_UP);

            if (plannedPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Ежемесячный платеж слишком маленький и не покрывает проценты.");
            }

            BigDecimal totalPrincipalPayment = plannedPrincipal.add(extraMonthlyPayment)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal actualPrincipalPayment;
            BigDecimal actualExtraPayment;
            BigDecimal actualTotalPayment;

            if (totalPrincipalPayment.compareTo(remainingBalance) >= 0) {
                actualPrincipalPayment = remainingBalance;

                BigDecimal neededExtra = remainingBalance.subtract(plannedPrincipal)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);

                actualExtraPayment = neededExtra;
                actualTotalPayment = interestAmount.add(plannedPrincipal).add(actualExtraPayment)
                        .setScale(2, RoundingMode.HALF_UP);

                remainingBalance = ZERO;
            } else {
                actualPrincipalPayment = totalPrincipalPayment;
                actualExtraPayment = extraMonthlyPayment;
                actualTotalPayment = baseMonthlyPayment.add(extraMonthlyPayment)
                        .setScale(2, RoundingMode.HALF_UP);

                remainingBalance = remainingBalance.subtract(actualPrincipalPayment)
                        .setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal netWorthWithoutMortgage = calculateCapitalWithoutMortgage ? savingsCurrentValue : null;
            BigDecimal netWorthWithMortgage = propertyCurrentValue.subtract(remainingBalance)
                    .setScale(2, RoundingMode.HALF_UP);

            schedule.add(new MortgagePaymentRow(
                    month,
                    baseMonthlyPayment,
                    actualExtraPayment,
                    actualTotalPayment,
                    interestAmount,
                    actualPrincipalPayment,
                    remainingBalance,
                    propertyCurrentValue,
                    savingsCurrentValue,
                    netWorthWithoutMortgage,
                    netWorthWithMortgage
            ));

            month++;
        }

        return schedule;
    }
}
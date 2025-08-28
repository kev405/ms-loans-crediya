package co.com.crediya.model.typeloan;

import co.com.crediya.model.value.InterestRate;
import co.com.crediya.model.value.Money;

public record TypeLoan(
        String id,
        String name,
        Money minimumAmount,
        Money maximumAmount,
        InterestRate annualInterestRate,
        boolean automaticValidation
) {}

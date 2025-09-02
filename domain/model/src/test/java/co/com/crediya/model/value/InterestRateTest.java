package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class InterestRateTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "0.02", "0.5", "1"})
    void valid_rates_in_0_to_1(String s) {
        var expected = new BigDecimal(s);
        var r = new InterestRate(expected);
        assertEquals(0, r.annualPercent().compareTo(expected));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.0001", "-1.0001", "-2"})
    void invalid_rates_throw(String s) {
        assertThrows(DomainValidationException.class, () -> {
            BigDecimal bd = (s == null) ? null : new BigDecimal(s);
            new InterestRate(bd);
        });
    }

    @Test
    void equals_hashcode_toString_and_accessor() {
        var r1 = new InterestRate(new BigDecimal("0.10"));
        var r2 = new InterestRate(new BigDecimal("0.10"));
        var r3 = new InterestRate(new BigDecimal("0.20"));

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
        assertNotEquals(r1, r3);
        assertTrue(r1.toString().contains("0.10"));
        assertEquals(0, r1.annualPercent().compareTo(new BigDecimal("0.10")));
    }
}

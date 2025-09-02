package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @ParameterizedTest
    @ValueSource(strings = {"0", "1", "123.45", "1000000"})
    void valid_money_constructs_ok(String s) {
        var expected = new BigDecimal(s);
        var m = new Money(expected);
        assertEquals(0, m.value().compareTo(expected)); // igualdad numérica
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-0.01", "-1", "-100"})
    void invalid_money_throws(String s) {
        assertThrows(DomainValidationException.class, () -> {
            BigDecimal bd = (s == null) ? null : new BigDecimal(s);
            new Money(bd);
        });
    }

    @Test
    void equals_hashcode_toString_and_accessor() {
        var m1 = new Money(new BigDecimal("123.00"));
        var m2 = new Money(new BigDecimal("123.00"));
        var m3 = new Money(new BigDecimal("99.99"));

        assertEquals(m1, m2);
        assertEquals(m1.hashCode(), m2.hashCode());
        assertNotEquals(m1, m3);
        assertTrue(m1.toString().contains("123"));
        assertEquals(0, m1.value().compareTo(new BigDecimal("123.00")));
    }
}

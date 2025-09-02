package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TermMonthsTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 24, 60, 360})
    void valid_terms_construct_ok(int n) {
        var t = new TermMonths(n);
        assertEquals(n, t.value());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -12})
    void invalid_terms_throw(int n) {
        assertThrows(DomainValidationException.class, () -> new TermMonths(n));
    }

    @Test
    void equals_hashcode_toString_and_accessor() {
        var t1 = new TermMonths(12);
        var t2 = new TermMonths(12);
        var t3 = new TermMonths(6);

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
        assertTrue(t1.toString().contains("12"));
        assertEquals(12, t1.value());
    }
}

package co.com.crediya.model.value;

import co.com.crediya.model.exceptions.DomainValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmailTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "a@b.com", "john.doe+tag@sub.example.co",
            "user_name@domain.io", "x@y.z"
    })
    void valid_emails_construct_ok(String s) {
        var e = new Email(s);
        assertEquals(s, e.value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "a@", "a@b", "no-at.com", "a@b.", "@b.com"})
    void invalid_emails_throw(String s) {
        assertThrows(DomainValidationException.class, () -> new Email(s));
    }

    @Test
    void equals_hashcode_toString_and_accessor() {
        var e1 = new Email("test@example.com");
        var e2 = new Email("test@example.com");
        var e3 = new Email("other@example.com");

        assertEquals(e1, e2);
        assertEquals(e1.hashCode(), e2.hashCode());
        assertNotEquals(e1, e3);
        assertTrue(e1.toString().contains("test@example.com"));
        assertEquals("test@example.com", e1.value());
    }
}

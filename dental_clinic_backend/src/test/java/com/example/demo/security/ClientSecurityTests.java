package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.junit.jupiter.api.Test;

class ClientSecurityTests {
    @Test
    void passwordsAreSaltedAndVerified() {
        String first = PasswordHasher.hash("secret-pass");
        String second = PasswordHasher.hash("secret-pass");

        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("secret-pass", first));
        assertFalse(PasswordHasher.matches("other-pass", first));
        assertFalse(PasswordHasher.matches("secret-pass", "garbage"));
    }

    @Test
    void phoneNumbersShareOneCanonicalForm() {
        assertEquals("+21622123456", PhoneNumbers.normalize("22 123 456"));
        assertEquals("+21622123456", PhoneNumbers.normalize("+216 22-123-456"));
        assertEquals("+21622123456", PhoneNumbers.normalize("0021622123456"));
        assertEquals("22123456", PhoneNumbers.last8("+216 22 123 456"));
        assertThrows(IllegalArgumentException.class, () -> PhoneNumbers.normalize("123"));
    }

    @Test
    void limiterBlocksAfterTheAllowanceAndResets() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1));

        assertTrue(limiter.tryAcquire("k"));
        assertTrue(limiter.tryAcquire("k"));
        assertFalse(limiter.tryAcquire("k"));
        assertFalse(limiter.isAllowed("k"));
        assertTrue(limiter.isAllowed("other"));
        limiter.reset("k");
        assertTrue(limiter.isAllowed("k"));
    }
}

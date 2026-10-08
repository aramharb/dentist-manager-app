package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.example.demo.security.PasswordHasher;

class LoginSecurityTests {
    @Test
    void accountLocksAfterFiveFailuresAndUnlocks() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 4; i++) attempts.recordFailure("Doctor", "1.1.1." + i);
        attempts.ensureAllowed("doctor", "9.9.9.9");
        assertFalse(attempts.isLocked("doctor"));

        attempts.recordFailure("DOCTOR", "1.1.1.9");

        assertTrue(attempts.isLocked("doctor"));
        assertThrows(TooManyRequestsException.class, () -> attempts.ensureAllowed("doctor", "9.9.9.9"));
        attempts.unlock("doctor");
        attempts.ensureAllowed("doctor", "9.9.9.9");
    }

    @Test
    void successResetsTheFailureCount() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 4; i++) attempts.recordFailure("sec", null);
        attempts.recordSuccess("sec");
        for (int i = 0; i < 4; i++) attempts.recordFailure("sec", null);

        assertFalse(attempts.isLocked("sec"));
    }

    @Test
    void unknownUsernamesAreLockedLikeRealOnes() {
        LoginAttemptService attempts = new LoginAttemptService();
        for (int i = 0; i < 5; i++) attempts.recordFailure("nobody", null);

        assertTrue(attempts.isLocked("nobody"));
    }

    @Test
    void legacyPlainPasswordsStillVerifyUntilMigrated() {
        assertTrue(PasswordHasher.matchesStoredOrLegacy("drwajih", "drwajih"));
        assertFalse(PasswordHasher.matchesStoredOrLegacy("other", "drwajih"));
        String hashed = PasswordHasher.hash("secret-pass");
        assertTrue(PasswordHasher.isHashed(hashed));
        assertTrue(PasswordHasher.matchesStoredOrLegacy("secret-pass", hashed));
        assertFalse(PasswordHasher.matchesStoredOrLegacy(hashed, hashed));
    }

    @Test
    void namesAreComparedIgnoringCaseAccentsAndOrder() {
        assertEquals(DuplicateClientGuard.normalizeName("Ahmed", "Ben  Ali"),
                DuplicateClientGuard.normalizeName("ben ali", "AHMED"));
        assertEquals(DuplicateClientGuard.normalizeName("Hélène", "Mejri"),
                DuplicateClientGuard.normalizeName("helene", "MEJRI"));
        assertFalse(DuplicateClientGuard.normalizeName("Sara", "One")
                .equals(DuplicateClientGuard.normalizeName("Ahmed", "One")));
    }
}

package com.example.demo.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Salted PBKDF2-HMAC-SHA256 password hashes, stored as {@code pbkdf2$iterations$salt$hash}. */
public final class PasswordHasher {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return "pbkdf2$" + ITERATIONS + "$" + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith("pbkdf2$");
    }

    /** Verifies a password against a hash, or (for accounts not yet migrated) against the legacy plain value. */
    public static boolean matchesStoredOrLegacy(String password, String stored) {
        if (stored == null) return false;
        if (isHashed(stored)) return matches(password, stored);
        return MessageDigest.isEqual(password.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                stored.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static boolean matches(String password, String stored) {
        try {
            String[] parts = stored.split("\\$");
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(password, Base64.getDecoder().decode(parts[2]), Integer.parseInt(parts[1]));
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)).getEncoded();
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

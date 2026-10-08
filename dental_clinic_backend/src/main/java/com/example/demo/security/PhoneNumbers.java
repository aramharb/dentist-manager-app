package com.example.demo.security;

/** Phone numbers are compared in one canonical form: +216XXXXXXXX for Tunisian numbers, +digits otherwise. */
public final class PhoneNumbers {
    private PhoneNumbers() {
    }

    public static String normalize(String raw) {
        if (raw == null) throw new IllegalArgumentException("Phone number is required.");
        String digits = raw.replaceAll("[^0-9]", "");
        if (raw.trim().startsWith("00")) digits = digits.substring(2);
        else if (digits.length() == 8) digits = "216" + digits;
        if (digits.length() < 9 || digits.length() > 15) {
            throw new IllegalArgumentException("Phone number is not valid. Example: 22 123 456.");
        }
        return "+" + digits;
    }

    /** Last 8 digits, used to suggest matching existing client files whatever their stored format. */
    public static String last8(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        return digits.length() <= 8 ? digits : digits.substring(digits.length() - 8);
    }
}

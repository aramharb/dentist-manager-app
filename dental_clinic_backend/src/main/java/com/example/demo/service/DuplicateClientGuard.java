package com.example.demo.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.security.PhoneNumbers;

/**
 * Refuses a second client file with the same phone number and the same name in a cabinet. Names are compared
 * ignoring case, accents, spacing and word order ("Ben Ali Ahmed" = "ahmed  ben ali"). A different name on the
 * same number is allowed (a family sharing a phone). Checked per cabinet, never across cabinets.
 */
@Service
public class DuplicateClientGuard {
    private final JdbcTemplate jdbc;

    public DuplicateClientGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @param excludedPatientId the file being edited, or null when creating one */
    @Transactional
    public void assertNoDuplicate(Long cabinetId, String firstName, String lastName, String phone, Long excludedPatientId) {
        if (cabinetId == null || phone == null || phone.replaceAll("[^0-9]", "").length() < 6) return;
        String last8 = PhoneNumbers.last8(phone);
        // Two concurrent registrations of the same client queue up here instead of both succeeding.
        jdbc.query("select pg_advisory_xact_lock(hashtext(?))", rs -> { }, cabinetId + "|" + last8);

        String wanted = normalizeName(firstName, lastName);
        List<Object[]> candidates = jdbc.query("""
                select id, patient_number, first_name, last_name from patient
                where cabinet_id = ?
                  and right(regexp_replace(coalesce(phone_number, ''), '[^0-9]', '', 'g'), 8) = ?
                  and (?::bigint is null or id <> ?)
                """, (rs, row) -> new Object[] { rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4) },
                cabinetId, last8, excludedPatientId, excludedPatientId);
        for (Object[] candidate : candidates) {
            if (wanted.equals(normalizeName((String) candidate[2], (String) candidate[3]))) {
                throw new BusinessRuleException("This client already exists in this cabinet: " + candidate[2] + " "
                        + candidate[3] + " (file " + candidate[1] + ") has the same phone number. "
                        + "Open the existing file instead of creating another one.");
            }
        }
    }

    static String normalizeName(String firstName, String lastName) {
        String text = ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName));
        text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        return Arrays.stream(text.split("[^\\p{L}\\p{N}]+")).filter(part -> !part.isBlank()).sorted()
                .collect(Collectors.joining(" "));
    }
}

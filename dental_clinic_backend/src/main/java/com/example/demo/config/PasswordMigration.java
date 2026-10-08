package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.security.PasswordHasher;

/** Replaces any password still stored as plain text by a salted hash (runs at startup, idempotent). */
@Component
@Order(0)
public class PasswordMigration implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PasswordMigration.class);
    private final JdbcTemplate jdbc;

    public PasswordMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        var rows = jdbc.query("select id, password from login where password not like 'pbkdf2$%'",
                (rs, row) -> new Object[] { rs.getLong(1), rs.getString(2) });
        for (Object[] row : rows) {
            jdbc.update("update login set password = ? where id = ?", PasswordHasher.hash((String) row[1]), row[0]);
        }
        if (!rows.isEmpty()) log.info("Hashed {} account password(s) that were stored in plain text.", rows.size());
    }
}

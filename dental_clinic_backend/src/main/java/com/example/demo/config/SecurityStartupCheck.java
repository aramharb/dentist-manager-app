package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Loud warnings at startup when development defaults are still in use. */
@Component
public class SecurityStartupCheck implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SecurityStartupCheck.class);
    private final String jwtSecret;
    private final String dbPassword;

    public SecurityStartupCheck(@Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${spring.datasource.password:}") String dbPassword) {
        this.jwtSecret = jwtSecret;
        this.dbPassword = dbPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (jwtSecret.startsWith("change-this-development-jwt-secret")) {
            log.warn("SECURITY: the development JWT secret is in use. Set JWT_SECRET to a long random value in production.");
        }
        if ("123456".equals(dbPassword) || "postgres".equals(dbPassword)) {
            log.warn("SECURITY: the database password is a development default. Set DB_PASSWORD in production.");
        }
    }
}

package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.LoginUser;
import com.example.demo.repository.LoginUserRepository;

/**
 * Creates the first admin account from the ADMIN_USERNAME / ADMIN_PASSWORD
 * environment variables when it does not exist yet. An existing account is
 * never modified, so password changes made from the admin page are kept.
 */
@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final LoginUserRepository userRepository;
    private final String username;
    private final String password;

    public AdminAccountInitializer(LoginUserRepository userRepository,
            @Value("${ADMIN_USERNAME:admin}") String username,
            @Value("${ADMIN_PASSWORD:}") String password) {
        this.userRepository = userRepository;
        this.username = username.trim().toLowerCase();
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsernameIgnoreCase(username)) return;
        if (password.isBlank()) {
            log.warn("No admin account \"{}\" exists and ADMIN_PASSWORD is not set; skipping admin creation.", username);
            return;
        }
        LoginUser admin = new LoginUser();
        admin.setUsername(username);
        admin.setFullName("Administrator");
        admin.setRole("admin");
        admin.setPassword(com.example.demo.security.PasswordHasher.hash(password));
        admin.setActive(true);
        userRepository.save(admin);
        log.info("Created admin account \"{}\".", username);
    }
}

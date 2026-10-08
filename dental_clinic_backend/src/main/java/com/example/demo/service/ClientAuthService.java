package com.example.demo.service;

import java.time.Duration;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ClientDto;
import com.example.demo.entity.ClientAccount;
import com.example.demo.repository.ClientAccountRepository;
import com.example.demo.security.AuthenticationException;
import com.example.demo.security.JwtTokenService;
import com.example.demo.security.PasswordHasher;
import com.example.demo.security.PhoneNumbers;
import com.example.demo.security.RateLimiter;

@Service
public class ClientAuthService {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password");

    private final ClientAccountRepository accounts;
    private final JwtTokenService tokenService;
    private final RateLimiter failedLogins = new RateLimiter(5, Duration.ofMinutes(15));
    private final RateLimiter registrations = new RateLimiter(10, Duration.ofHours(1));

    public ClientAuthService(ClientAccountRepository accounts, JwtTokenService tokenService) {
        this.accounts = accounts;
        this.tokenService = tokenService;
    }

    @Transactional
    public ClientDto.AuthResponse register(ClientDto.RegisterRequest request, String clientAddress) {
        if (!registrations.tryAcquire(clientAddress)) {
            throw new TooManyRequestsException("Too many sign-ups from this connection. Try again later.");
        }
        String phone = PhoneNumbers.normalize(request.phone());
        String fullName = request.fullName().trim();
        if (fullName.length() < 2) throw new IllegalArgumentException("Full name is required.");
        if (request.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (accounts.existsByPhone(phone)) {
            throw new BusinessRuleException("This phone number already has an account. Sign in instead.");
        }
        ClientAccount account = new ClientAccount();
        account.setPhone(phone);
        account.setFullName(fullName);
        account.setPasswordHash(PasswordHasher.hash(request.password()));
        account.setActive(true);
        return authenticated(accounts.save(account));
    }

    @Transactional(readOnly = true)
    public ClientDto.AuthResponse login(ClientDto.LoginRequest request, String clientAddress) {
        String phone;
        try {
            phone = PhoneNumbers.normalize(request.phone());
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationException("Invalid phone number or password.");
        }
        String key = phone + "|" + clientAddress;
        if (!failedLogins.isAllowed(phone) || !failedLogins.isAllowed(key)) {
            throw new TooManyRequestsException("Too many failed attempts. Try again in a few minutes.");
        }
        ClientAccount account = accounts.findByPhone(phone).filter(a -> Boolean.TRUE.equals(a.getActive())).orElse(null);
        boolean ok = PasswordHasher.matches(request.password(), account == null ? DUMMY_HASH : account.getPasswordHash());
        if (account == null || !ok) {
            failedLogins.tryAcquire(phone);
            failedLogins.tryAcquire(key);
            throw new AuthenticationException("Invalid phone number or password.");
        }
        failedLogins.reset(phone);
        failedLogins.reset(key);
        return authenticated(account);
    }

    @Transactional(readOnly = true)
    public ClientDto.Account me(Long accountId) {
        return accounts.findById(accountId).filter(a -> Boolean.TRUE.equals(a.getActive())).map(this::toAccount)
                .orElseThrow(() -> new AuthenticationException("This account is no longer active."));
    }

    private ClientDto.AuthResponse authenticated(ClientAccount account) {
        return new ClientDto.AuthResponse(tokenService.issueClient(account), toAccount(account));
    }

    private ClientDto.Account toAccount(ClientAccount account) {
        return new ClientDto.Account(account.getId(), account.getFullName(), account.getPhone());
    }
}

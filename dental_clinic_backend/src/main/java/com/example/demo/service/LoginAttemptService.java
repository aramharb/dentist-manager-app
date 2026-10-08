package com.example.demo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import com.example.demo.security.RateLimiter;

/**
 * Brute-force protection for staff sign-in. After 5 failed attempts in 15 minutes an account is locked, for
 * longer each time it happens again (15 min, 30 min, 1 h ... up to 24 h). Unknown usernames are tracked the
 * same way, so locking never reveals whether an account exists. An address that fails too often is also paused.
 * An admin (or the cabinet manager, for the cabinet's members) can unlock an account at any time.
 */
@Service
public class LoginAttemptService {
    static final int MAX_FAILURES = 5;
    static final Duration WINDOW = Duration.ofMinutes(15);
    static final Duration BASE_LOCK = Duration.ofMinutes(15);
    static final Duration MAX_LOCK = Duration.ofHours(24);

    private static final class State {
        final Deque<Instant> failures = new ArrayDeque<>();
        Instant lockedUntil;
        int level;
    }

    private final ConcurrentMap<String, State> accounts = new ConcurrentHashMap<>();
    private final RateLimiter addressFailures = new RateLimiter(20, WINDOW);

    /** Throws when the account or the address is currently locked. */
    public void ensureAllowed(String username, String address) {
        State state = accounts.get(key(username));
        if (state != null) {
            synchronized (state) {
                if (state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now())) {
                    long minutes = Math.max(1, Duration.between(Instant.now(), state.lockedUntil).toMinutes() + 1);
                    throw new TooManyRequestsException(
                            "Too many failed attempts. This account is locked for " + minutes + " more minute(s).");
                }
            }
        }
        if (address != null && !addressFailures.isAllowed(address)) {
            throw new TooManyRequestsException("Too many failed attempts from this connection. Try again later.");
        }
    }

    public void recordFailure(String username, String address) {
        if (address != null) addressFailures.tryAcquire(address);
        State state = accounts.computeIfAbsent(key(username), ignored -> new State());
        synchronized (state) {
            Instant now = Instant.now();
            state.failures.addLast(now);
            while (!state.failures.isEmpty() && state.failures.peekFirst().isBefore(now.minus(WINDOW))) {
                state.failures.pollFirst();
            }
            if (state.failures.size() >= MAX_FAILURES) {
                state.level++;
                Duration lock = BASE_LOCK.multipliedBy(1L << Math.min(state.level - 1, 7));
                if (lock.compareTo(MAX_LOCK) > 0) lock = MAX_LOCK;
                state.lockedUntil = now.plus(lock);
                state.failures.clear();
            }
        }
    }

    public void recordSuccess(String username) {
        accounts.remove(key(username));
    }

    public boolean isLocked(String username) {
        State state = accounts.get(key(username));
        if (state == null) return false;
        synchronized (state) {
            return state.lockedUntil != null && state.lockedUntil.isAfter(Instant.now());
        }
    }

    public void unlock(String username) {
        accounts.remove(key(username));
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}

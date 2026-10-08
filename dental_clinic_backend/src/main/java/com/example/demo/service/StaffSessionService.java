package com.example.demo.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.security.SessionRegistry;

/**
 * Sessions of staff accounts: an absolute lifetime, an inactivity timeout and a cap on simultaneous
 * sessions per account (the oldest ones are closed when it is exceeded).
 */
@Service
public class StaffSessionService implements SessionRegistry {
    private final JdbcTemplate jdbc;
    private final long lifetimeSeconds;
    private final long idleSeconds;
    private final int maxSessions;

    public StaffSessionService(JdbcTemplate jdbc,
            @Value("${app.auth.jwt-lifetime-seconds:28800}") long lifetimeSeconds,
            @Value("${app.auth.idle-timeout-seconds:7200}") long idleSeconds,
            @Value("${app.auth.max-sessions:3}") int maxSessions) {
        this.jdbc = jdbc;
        this.lifetimeSeconds = lifetimeSeconds;
        this.idleSeconds = idleSeconds;
        this.maxSessions = Math.max(1, maxSessions);
    }

    @Override
    public String open(Long userId, String clientAddress) {
        String id = UUID.randomUUID().toString();
        jdbc.update("""
                insert into staff_session (id, user_id, expires_at, client_address)
                values (?, ?, now() + (? * interval '1 second'), ?)
                """, id, userId, lifetimeSeconds, clientAddress == null ? null : clientAddress);
        // Keep the newest sessions only: closing the oldest ones logs those devices out.
        jdbc.update("""
                update staff_session set revoked_at = now()
                where user_id = ? and revoked_at is null and id not in (
                    select id from staff_session where user_id = ? and revoked_at is null
                    order by created_at desc limit ?)
                """, userId, userId, maxSessions);
        jdbc.update("delete from staff_session where expires_at < now() - interval '1 day'");
        return id;
    }

    @Override
    public boolean touch(String sessionId, Long userId) {
        if (sessionId == null || sessionId.isBlank()) return false;
        return jdbc.update("""
                update staff_session
                set last_seen_at = case when last_seen_at < now() - interval '1 minute' then now() else last_seen_at end
                where id = ? and user_id = ? and revoked_at is null and expires_at > now()
                  and last_seen_at > now() - (? * interval '1 second')
                """, sessionId, userId, idleSeconds) == 1;
    }

    @Override
    public void close(String sessionId) {
        if (sessionId != null) jdbc.update("update staff_session set revoked_at = now() where id = ? and revoked_at is null", sessionId);
    }

    @Override
    public void closeAll(Long userId) {
        jdbc.update("update staff_session set revoked_at = now() where user_id = ? and revoked_at is null", userId);
    }
}

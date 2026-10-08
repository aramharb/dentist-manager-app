package com.example.demo.security;

/** Server-side record of staff sessions, so tokens can expire from inactivity and be revoked. */
public interface SessionRegistry {
    /** Opens a session for the user and returns its id (carried in the token as {@code jti}). */
    String open(Long userId, String clientAddress);

    /** True when the session exists, is not revoked or expired, and the user was active recently. */
    boolean touch(String sessionId, Long userId);

    void close(String sessionId);

    void closeAll(Long userId);
}

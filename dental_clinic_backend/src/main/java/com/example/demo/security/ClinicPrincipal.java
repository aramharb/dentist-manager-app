package com.example.demo.security;

import java.security.Principal;

public record ClinicPrincipal(Long userId, String username, String role, Long cabinetId, String sessionId)
        implements Principal {
    public ClinicPrincipal(Long userId, String username, String role, Long cabinetId) {
        this(userId, username, role, cabinetId, null);
    }

    /** Role of a client (patient) account; such a principal can only use the /api/client endpoints. */
    public static final String CLIENT_ROLE = "client";

    public ClinicPrincipal(Long userId, String username, String role) {
        this(userId, username, role, null);
    }

    @Override
    public String getName() {
        return username;
    }

    public boolean hasRole(String expectedRole) {
        return expectedRole != null && expectedRole.equalsIgnoreCase(role);
    }

    /** The cabinet this user works in; the platform admin has none. */
    public Long requireCabinetId() {
        if (cabinetId == null) throw new AuthenticationException("This account does not belong to a cabinet.");
        return cabinetId;
    }

    public static ClinicPrincipal require(Principal principal) {
        if (principal instanceof ClinicPrincipal clinicPrincipal) return clinicPrincipal;
        throw new AuthenticationException("Authenticated clinic user is required.");
    }
}

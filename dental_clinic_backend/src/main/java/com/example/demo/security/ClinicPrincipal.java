package com.example.demo.security;

import java.security.Principal;

public record ClinicPrincipal(Long userId, String username, String role, Long cabinetId) implements Principal {
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

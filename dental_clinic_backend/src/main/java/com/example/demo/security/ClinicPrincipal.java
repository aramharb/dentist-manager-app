package com.example.demo.security;

import java.security.Principal;

public record ClinicPrincipal(Long userId, String username, String role) implements Principal {
    @Override
    public String getName() {
        return username;
    }

    public boolean hasRole(String expectedRole) {
        return expectedRole != null && expectedRole.equalsIgnoreCase(role);
    }

    public static ClinicPrincipal require(Principal principal) {
        if (principal instanceof ClinicPrincipal clinicPrincipal) return clinicPrincipal;
        throw new AuthenticationException("Authenticated clinic user is required.");
    }
}

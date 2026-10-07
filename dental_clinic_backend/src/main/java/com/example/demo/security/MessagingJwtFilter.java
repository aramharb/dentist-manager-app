package com.example.demo.security;

import java.io.IOException;
import java.security.Principal;

import com.example.demo.tenant.CabinetContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class MessagingJwtFilter extends OncePerRequestFilter {
    private final JwtTokenService tokenService;

    public MessagingJwtFilter(JwtTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.OPTIONS.matches(request.getMethod())
                || !path.startsWith("/api/")
                || path.equals("/api/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            ClinicPrincipal principal = tokenService.verify(bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION)));
            CabinetContext.set(principal.cabinetId());
            try {
                filterChain.doFilter(new PrincipalRequest(request, principal), response);
            } finally {
                CabinetContext.clear();
            }
        } catch (AuthenticationException exception) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\",\"messages\":[\"Invalid or expired authentication token.\"]}");
        }
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AuthenticationException("Authentication token is required.");
        }
        return authorization.substring(7).trim();
    }

    private static final class PrincipalRequest extends HttpServletRequestWrapper {
        private final ClinicPrincipal principal;

        private PrincipalRequest(HttpServletRequest request, ClinicPrincipal principal) {
            super(request);
            this.principal = principal;
        }

        @Override
        public Principal getUserPrincipal() {
            return principal;
        }

        @Override
        public String getRemoteUser() {
            return principal.getName();
        }

        @Override
        public boolean isUserInRole(String role) {
            return role != null && role.equalsIgnoreCase(principal.role());
        }
    }
}

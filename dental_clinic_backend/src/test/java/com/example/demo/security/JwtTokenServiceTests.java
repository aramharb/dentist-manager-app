package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.entity.LoginUser;
import com.example.demo.repository.LoginUserRepository;

import tools.jackson.databind.ObjectMapper;

class JwtTokenServiceTests {
    private JwtTokenService tokenService;
    private LoginUser user;

    @BeforeEach
    void setUp() {
        LoginUserRepository userRepository = mock(LoginUserRepository.class);
        user = new LoginUser();
        setId(user, 7L);
        user.setUsername("doctor");
        user.setFullName("Doctor Test");
        user.setRole("doctor");
        user.setPassword("password");
        user.setActive(true);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        tokenService = new JwtTokenService(new ObjectMapper(), userRepository,
                "a-test-secret-with-more-than-thirty-two-bytes", 3600);
    }

    @Test
    void issuedTokenRestoresTrustedUserIdentity() {
        ClinicPrincipal principal = tokenService.verify(tokenService.issue(user));

        assertEquals(7L, principal.userId());
        assertEquals("doctor", principal.getName());
        assertEquals("doctor", principal.role());
    }

    @Test
    void changedTokenIsRejected() {
        String token = tokenService.issue(user);
        String changed = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThrows(AuthenticationException.class, () -> tokenService.verify(changed));
    }

    private void setId(LoginUser target, Long id) {
        try {
            Field field = LoginUser.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

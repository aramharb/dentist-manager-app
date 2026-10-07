package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.controller.LoginRequest;
import com.example.demo.controller.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.SessionUserDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.security.JwtTokenService;

@Service
public class AuthService {
    private final LoginUserRepository userRepository;
    private final JwtTokenService tokenService;
    private final UserPresenceService presenceService;

    public AuthService(LoginUserRepository userRepository, JwtTokenService tokenService,
            UserPresenceService presenceService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.presenceService = presenceService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String username = normalize(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        LoginUser user = userRepository.findByUsernameIgnoreCaseAndActiveTrue(username)
                .filter(candidate -> candidate.getPassword().equals(password))
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));
        return new LoginResponse(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                tokenService.issue(user), "Login successful.");
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> users() {
        return userRepository.findByActiveTrueOrderByRoleAscFullNameAsc().stream()
                .map(user -> new UserDto.Response(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                        presenceService.isOnline(user.getUsername())))
                .toList();
    }

    @Transactional(readOnly = true)
    public SessionUserDto currentUser(ClinicPrincipal principal) {
        LoginUser user = userRepository.findById(principal.userId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new com.example.demo.security.AuthenticationException(
                        "Authenticated user is no longer active."));
        return toSessionUser(user);
    }

    public SessionUserDto toSessionUser(LoginUser user) {
        String role = normalize(user.getRole());
        List<String> permissions = "doctor".equals(role)
                ? List.of("dashboard:doctor", "patients:read", "patients:write", "patients:delete",
                        "appointments:write", "treatments:write", "expenses:read", "messages:write",
                        "staff-actions:undo")
                : List.of("dashboard:secretary", "patients:read", "patients:write", "appointments:write",
                        "expenses:write", "messages:write");
        return new SessionUserDto(user.getId(), user.getUsername(), user.getFullName(), role,
                Boolean.TRUE.equals(user.getActive()), permissions);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

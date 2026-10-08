package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.controller.LoginRequest;
import com.example.demo.controller.LoginResponse;
import com.example.demo.dto.UserDto;
import com.example.demo.dto.SessionUserDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Cabinet;
import com.example.demo.repository.CabinetRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.security.PasswordHasher;
import com.example.demo.security.SessionRegistry;
import com.example.demo.security.JwtTokenService;

@Service
public class AuthService {
    private final LoginUserRepository userRepository;
    private final JwtTokenService tokenService;
    private final UserPresenceService presenceService;
    private final CabinetRepository cabinetRepository;
    private final LoginAttemptService loginAttempts;
    private final SessionRegistry sessionRegistry;
    private static final String DUMMY_HASH = PasswordHasher.hash("not-a-real-password");

    public AuthService(LoginUserRepository userRepository, JwtTokenService tokenService,
            UserPresenceService presenceService, CabinetRepository cabinetRepository,
            LoginAttemptService loginAttempts, SessionRegistry sessionRegistry) {
        this.loginAttempts = loginAttempts;
        this.sessionRegistry = sessionRegistry;
        this.cabinetRepository = cabinetRepository;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.presenceService = presenceService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, String clientAddress) {
        String username = normalize(request.getUsername());
        String password = request.getPassword() == null ? "" : request.getPassword();
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalArgumentException("Username and password are required.");
        }
        loginAttempts.ensureAllowed(username, clientAddress);
        LoginUser user = userRepository.findByUsernameIgnoreCaseAndActiveTrue(username).orElse(null);
        // Always do one hash verification, so response time does not tell whether the username exists.
        boolean valid = PasswordHasher.matchesStoredOrLegacy(password, user == null ? DUMMY_HASH : user.getPassword());
        if (user == null || !valid) {
            loginAttempts.recordFailure(username, clientAddress);
            throw new IllegalArgumentException("Invalid username or password.");
        }
        Cabinet cabinet = cabinetOf(user);
        if (cabinet != null && !Boolean.TRUE.equals(cabinet.getActive())) {
            throw new IllegalArgumentException("Your cabinet is disabled. Contact the administrator.");
        }
        loginAttempts.recordSuccess(username);
        if (!PasswordHasher.isHashed(user.getPassword())) {
            // Accounts created before passwords were hashed are upgraded on their next successful sign-in.
            user.setPassword(PasswordHasher.hash(password));
            userRepository.save(user);
        }
        LoginResponse response = new LoginResponse(user.getId(), user.getUsername(), user.getFullName(),
                user.getRole(), tokenService.issue(user, clientAddress), "Login successful.");
        if (cabinet != null) {
            response.setCabinetId(cabinet.getId());
            response.setCabinetName(cabinet.getName());
        }
        return response;
    }

    /** Ends the caller's session: the token stops working immediately. */
    public void logout(ClinicPrincipal principal) {
        sessionRegistry.close(principal.sessionId());
    }

    @Transactional(readOnly = true)
    public List<UserDto.Response> users(ClinicPrincipal principal) {
        return userRepository.findByCabinetIdAndActiveTrueOrderByRoleAscFullNameAsc(principal.requireCabinetId()).stream()
                .filter(user -> !"admin".equalsIgnoreCase(user.getRole()))
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
        List<String> permissions = switch (role) {
            case "doctor" -> List.of("dashboard:doctor", "patients:read", "patients:write", "patients:delete",
                    "appointments:write", "treatments:write", "expenses:read", "messages:write",
                    "staff-actions:undo");
            case "admin" -> List.of("dashboard:admin", "users:read", "users:write");
            case "manager" -> List.of("dashboard:manager", "cabinet-users:read", "cabinet-users:write");
            default -> List.of("dashboard:secretary", "patients:read", "patients:write", "appointments:write",
                    "expenses:write", "messages:write");
        };
        Cabinet cabinet = cabinetOf(user);
        return new SessionUserDto(user.getId(), user.getUsername(), user.getFullName(), role,
                Boolean.TRUE.equals(user.getActive()), permissions,
                cabinet == null ? null : cabinet.getId(), cabinet == null ? null : cabinet.getName());
    }

    private Cabinet cabinetOf(LoginUser user) {
        return user.getCabinetId() == null ? null : cabinetRepository.findById(user.getCabinetId()).orElse(null);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}

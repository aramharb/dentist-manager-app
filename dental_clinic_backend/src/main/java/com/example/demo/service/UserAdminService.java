package com.example.demo.service;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AdminUserDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.security.ClinicPrincipal;

@Service
public class UserAdminService {
    private static final Set<String> ROLES = Set.of("doctor", "secretaire", "admin");
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final LoginUserRepository userRepository;

    public UserAdminService(LoginUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminUserDto.Response> list() {
        return userRepository.findAllByOrderByRoleAscFullNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserDto.Response create(AdminUserDto.CreateRequest request) {
        String username = username(request.username());
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("Username \"" + username + "\" is already taken.");
        }
        LoginUser user = new LoginUser();
        user.setUsername(username);
        user.setFullName(fullName(request.fullName()));
        user.setRole(role(request.role()));
        user.setPassword(password(request.password()));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserDto.Response update(Long id, AdminUserDto.UpdateRequest request, ClinicPrincipal actor) {
        LoginUser user = find(id);
        String username = username(request.username());
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("Username \"" + username + "\" is already taken.");
        }
        String role = role(request.role());
        boolean active = request.active() == null ? Boolean.TRUE.equals(user.getActive()) : request.active();

        boolean wasActiveAdmin = "admin".equals(user.getRole()) && Boolean.TRUE.equals(user.getActive());
        boolean staysActiveAdmin = "admin".equals(role) && active;
        if (user.getId().equals(actor.userId()) && !staysActiveAdmin) {
            throw new BusinessRuleException("You cannot deactivate your own account or remove your own admin role.");
        }
        if (wasActiveAdmin && !staysActiveAdmin && userRepository.countByRoleIgnoreCaseAndActiveTrue("admin") <= 1) {
            throw new BusinessRuleException("At least one active admin account is required.");
        }

        user.setUsername(username);
        user.setFullName(fullName(request.fullName()));
        user.setRole(role);
        user.setActive(active);
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public void resetPassword(Long id, AdminUserDto.PasswordRequest request) {
        LoginUser user = find(id);
        user.setPassword(password(request.password()));
        userRepository.save(user);
    }

    private LoginUser find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private AdminUserDto.Response toResponse(LoginUser user) {
        return new AdminUserDto.Response(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                Boolean.TRUE.equals(user.getActive()), user.getCreatedAt(), user.getUpdatedAt());
    }

    private String username(String value) {
        String username = value == null ? "" : value.trim().toLowerCase();
        if (!username.matches("[a-z0-9._-]{3,80}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-80 characters: letters, digits, dot, dash or underscore.");
        }
        return username;
    }

    private String fullName(String value) {
        String fullName = value == null ? "" : value.trim();
        if (fullName.isEmpty() || fullName.length() > 160) {
            throw new IllegalArgumentException("Full name is required (max 160 characters).");
        }
        return fullName;
    }

    private String role(String value) {
        String role = value == null ? "" : value.trim().toLowerCase();
        if (!ROLES.contains(role)) {
            throw new IllegalArgumentException("Role must be doctor, secretaire or admin.");
        }
        return role;
    }

    private String password(String value) {
        if (value == null || value.length() < MIN_PASSWORD_LENGTH || value.length() > 255) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        return value;
    }
}

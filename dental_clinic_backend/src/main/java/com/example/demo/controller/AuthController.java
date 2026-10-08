package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDto;
import com.example.demo.service.AuthService;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;

import com.example.demo.dto.SessionUserDto;
import com.example.demo.security.ClinicPrincipal;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        try {
            return ResponseEntity.ok(authService.login(request, http.getRemoteAddr()));
        } catch (IllegalArgumentException exception) {
            HttpStatus status = exception.getMessage().startsWith("Invalid") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new LoginResponse(null, exception.getMessage()));
        }
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(Principal principal) {
        authService.logout(ClinicPrincipal.require(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public List<UserDto.Response> users(Principal principal) {
        return authService.users(ClinicPrincipal.require(principal));
    }

    @GetMapping("/auth/me")
    public SessionUserDto me(Principal principal) {
        return authService.currentUser(ClinicPrincipal.require(principal));
    }
}

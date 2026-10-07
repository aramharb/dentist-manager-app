package com.example.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDto;
import com.example.demo.service.AuthService;

import java.util.List;
import java.security.Principal;

import com.example.demo.dto.SessionUserDto;
import com.example.demo.security.ClinicPrincipal;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (IllegalArgumentException exception) {
            HttpStatus status = exception.getMessage().startsWith("Invalid") ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(new LoginResponse(null, exception.getMessage()));
        }
    }

    @GetMapping("/users")
    public List<UserDto.Response> users() {
        return authService.users();
    }

    @GetMapping("/auth/me")
    public SessionUserDto me(Principal principal) {
        return authService.currentUser(ClinicPrincipal.require(principal));
    }
}

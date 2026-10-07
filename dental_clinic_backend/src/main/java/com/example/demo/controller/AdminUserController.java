package com.example.demo.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.AdminUserDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.UserAdminService;

@RestController
@RequestMapping("/api/admin/users")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class AdminUserController {
    private final UserAdminService userAdminService;

    public AdminUserController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<AdminUserDto.Response> list(Principal principal) {
        requireAdmin(principal);
        return userAdminService.list();
    }

    @PostMapping
    public ResponseEntity<AdminUserDto.Response> create(@RequestBody AdminUserDto.CreateRequest request,
            Principal principal) {
        requireAdmin(principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(userAdminService.create(request));
    }

    @PutMapping("/{id}")
    public AdminUserDto.Response update(@PathVariable Long id, @RequestBody AdminUserDto.UpdateRequest request,
            Principal principal) {
        return userAdminService.update(id, request, requireAdmin(principal));
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody AdminUserDto.PasswordRequest request,
            Principal principal) {
        requireAdmin(principal);
        userAdminService.resetPassword(id, request);
        return ResponseEntity.noContent().build();
    }

    private ClinicPrincipal requireAdmin(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("admin")) {
            throw new MessagingAccessDeniedException("Only an admin can manage user accounts.");
        }
        return actor;
    }
}

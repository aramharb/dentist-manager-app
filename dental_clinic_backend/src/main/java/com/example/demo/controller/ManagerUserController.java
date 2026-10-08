package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AdminUserDto;
import com.example.demo.dto.InvitationDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.UserAdminService;

/**
 * Account management for the cabinet manager: the doctors and secretaries of the manager's own
 * cabinet, nothing else. The cabinet always comes from the login, never from the request.
 */
@RestController
@RequestMapping("/api/manager/users")
public class ManagerUserController {
    private final UserAdminService userService;

    public ManagerUserController(UserAdminService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<AdminUserDto.Response> list(Principal principal) {
        return userService.listMembers(requireManager(principal).requireCabinetId());
    }

    @PostMapping
    public ResponseEntity<AdminUserDto.Created> create(@RequestBody AdminUserDto.MemberCreateRequest request,
            Principal principal) {
        ClinicPrincipal actor = requireManager(principal);
        AdminUserDto.Created created = userService.createMember(actor.requireCabinetId(), request, actor);
        return ResponseEntity.created(URI.create("/api/manager/users/" + created.user().id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public AdminUserDto.Response update(@PathVariable Long id, @RequestBody AdminUserDto.MemberUpdateRequest request,
            Principal principal) {
        return userService.updateMember(requireManager(principal).requireCabinetId(), id, request);
    }

    @PostMapping("/{id}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long id, Principal principal) {
        userService.unlockMember(requireManager(principal).requireCabinetId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/invitation")
    public ResponseEntity<InvitationDto.Created> invite(@PathVariable Long id, Principal principal) {
        ClinicPrincipal actor = requireManager(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userService.inviteMember(actor.requireCabinetId(), id, actor));
    }

    private ClinicPrincipal requireManager(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("manager")) {
            throw new MessagingAccessDeniedException("Only the cabinet manager can manage the cabinet's accounts.");
        }
        return actor;
    }
}

package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.InvitationDto;
import com.example.demo.service.InvitationService;

/** Public endpoints (no login): the one-time token in the path is the credential. */
@RestController
@RequestMapping("/api/invitations")
public class InvitationController {
    private final InvitationService invitationService;

    public InvitationController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/{token}")
    public InvitationDto.Preview preview(@PathVariable String token) {
        return invitationService.preview(token);
    }

    @PostMapping("/{token}/accept")
    public ResponseEntity<Void> accept(@PathVariable String token, @RequestBody InvitationDto.Accept request) {
        invitationService.accept(token, request.password());
        return ResponseEntity.noContent().build();
    }
}

package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.CabinetDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.CabinetAdminService;
import com.example.demo.service.MessagingAccessDeniedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/cabinets")
@CrossOrigin(origins = { "http://localhost:4200", "http://127.0.0.1:4200" })
public class AdminCabinetController {
    private final CabinetAdminService cabinetService;

    public AdminCabinetController(CabinetAdminService cabinetService) {
        this.cabinetService = cabinetService;
    }

    @GetMapping
    public List<CabinetDto.Response> list(Principal principal) {
        requireAdmin(principal);
        return cabinetService.list();
    }

    @GetMapping("/{id}")
    public CabinetDto.Response get(@PathVariable Long id, Principal principal) {
        requireAdmin(principal);
        return cabinetService.get(id);
    }

    @PostMapping
    public ResponseEntity<CabinetDto.Response> create(@Valid @RequestBody CabinetDto.Request request,
            Principal principal) {
        requireAdmin(principal);
        CabinetDto.Response response = cabinetService.create(request);
        return ResponseEntity.created(URI.create("/api/admin/cabinets/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public CabinetDto.Response update(@PathVariable Long id, @Valid @RequestBody CabinetDto.Request request,
            Principal principal) {
        requireAdmin(principal);
        return cabinetService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        requireAdmin(principal);
        cabinetService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Principal principal) {
        if (!ClinicPrincipal.require(principal).hasRole("admin")) {
            throw new MessagingAccessDeniedException("Only an admin can manage cabinets.");
        }
    }
}

package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.MaterialDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MaterialService;
import com.example.demo.service.MessagingAccessDeniedException;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/materials")
@CrossOrigin(origins = {"http://localhost:4200", "http://127.0.0.1:4200"})
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public List<MaterialDto.Response> findAll(Principal principal) {
        requireStaff(principal);
        return materialService.findAll();
    }

    @PostMapping
    public ResponseEntity<MaterialDto.Response> create(@Valid @RequestBody MaterialDto.Request request,
            Principal principal) {
        MaterialDto.Response response = materialService.create(request, requireStaff(principal));
        return ResponseEntity.created(URI.create("/api/materials/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public MaterialDto.Response update(@PathVariable Long id, @Valid @RequestBody MaterialDto.Request request,
            Principal principal) {
        return materialService.update(id, request, requireStaff(principal));
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can manage materials.");
        }
        return actor;
    }
}

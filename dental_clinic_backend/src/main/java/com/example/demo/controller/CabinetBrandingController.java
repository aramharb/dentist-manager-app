package com.example.demo.controller;

import java.io.IOException;
import java.security.Principal;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.BrandingDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.CabinetBrandingService;
import com.example.demo.service.MessagingAccessDeniedException;

import jakarta.validation.Valid;

/**
 * Identity of a cabinet. The manager edits their own cabinet (taken from the login); the admin edits
 * any cabinet through the {@code /api/admin/cabinets/{id}/branding} routes.
 */
@RestController
public class CabinetBrandingController {
    private final CabinetBrandingService brandingService;
    private final com.example.demo.service.RecordAccessLogService accessLogService;

    public CabinetBrandingController(CabinetBrandingService brandingService,
            com.example.demo.service.RecordAccessLogService accessLogService) {
        this.accessLogService = accessLogService;
        this.brandingService = brandingService;
    }

    // ------------------------------------------------------------------ manager (own cabinet)

    @GetMapping("/api/manager/cabinet")
    public BrandingDto.Response myBranding(Principal principal) {
        return brandingService.get(manager(principal).requireCabinetId());
    }

    @PutMapping("/api/manager/cabinet")
    public BrandingDto.Response updateMine(@Valid @RequestBody BrandingDto.Request request, Principal principal) {
        return brandingService.update(manager(principal).requireCabinetId(), request);
    }

    @PostMapping("/api/manager/cabinet/images/{kind}")
    public BrandingDto.Response uploadMine(@PathVariable String kind, @RequestParam("file") MultipartFile file,
            Principal principal) throws IOException {
        return brandingService.saveImage(manager(principal).requireCabinetId(), kind, file.getBytes());
    }

    @DeleteMapping("/api/manager/cabinet/images/{kind}")
    public BrandingDto.Response deleteMine(@PathVariable String kind, Principal principal) {
        return brandingService.deleteImage(manager(principal).requireCabinetId(), kind);
    }

    // ------------------------------------------------------------------ admin (any cabinet)

    @GetMapping("/api/admin/cabinets/{id}/branding")
    public BrandingDto.Response get(@PathVariable Long id, Principal principal) {
        admin(principal);
        return brandingService.get(id);
    }

    @PutMapping("/api/admin/cabinets/{id}/branding")
    public BrandingDto.Response update(@PathVariable Long id, @Valid @RequestBody BrandingDto.Request request,
            Principal principal) {
        admin(principal);
        return brandingService.update(id, request);
    }

    @PostMapping("/api/admin/cabinets/{id}/branding/images/{kind}")
    public BrandingDto.Response upload(@PathVariable Long id, @PathVariable String kind,
            @RequestParam("file") MultipartFile file, Principal principal) throws IOException {
        admin(principal);
        return brandingService.saveImage(id, kind, file.getBytes());
    }

    @DeleteMapping("/api/admin/cabinets/{id}/branding/images/{kind}")
    public BrandingDto.Response delete(@PathVariable Long id, @PathVariable String kind, Principal principal) {
        admin(principal);
        return brandingService.deleteImage(id, kind);
    }

    // ------------------------------------------------------------------ access journal

    @GetMapping("/api/manager/access-log")
    public java.util.List<com.example.demo.dto.AccessLogDto.Entry> myAccessLog(
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset,
            Principal principal) {
        return accessLogService.recent(manager(principal).requireCabinetId(), limit, offset);
    }

    @GetMapping("/api/admin/cabinets/{id}/access-log")
    public java.util.List<com.example.demo.dto.AccessLogDto.Entry> accessLog(@PathVariable Long id,
            @RequestParam(defaultValue = "100") int limit, @RequestParam(defaultValue = "0") int offset,
            Principal principal) {
        admin(principal);
        return accessLogService.recent(id, limit, offset);
    }

    private ClinicPrincipal manager(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("manager")) {
            throw new MessagingAccessDeniedException("Only the cabinet manager can change the cabinet's identity.");
        }
        return actor;
    }

    private void admin(Principal principal) {
        if (!ClinicPrincipal.require(principal).hasRole("admin")) {
            throw new MessagingAccessDeniedException("Only an admin can change another cabinet's identity.");
        }
    }
}

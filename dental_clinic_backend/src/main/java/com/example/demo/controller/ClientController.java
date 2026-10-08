package com.example.demo.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.BrandingDto;
import com.example.demo.dto.ClientDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.CabinetBrandingService;
import com.example.demo.service.ClientAuthService;
import com.example.demo.service.ClientPortalService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/** Public cabinet pages and the client (patient) space. */
@RestController
public class ClientController {
    private final ClientAuthService authService;
    private final ClientPortalService portalService;
    private final CabinetBrandingService brandingService;

    public ClientController(ClientAuthService authService, ClientPortalService portalService,
            CabinetBrandingService brandingService) {
        this.authService = authService;
        this.portalService = portalService;
        this.brandingService = brandingService;
    }

    // ------------------------------------------------------------------ public (no login)

    @GetMapping("/api/public/cabinets")
    public List<BrandingDto.Response> cabinets() {
        return portalService.activeCabinets();
    }

    @GetMapping("/api/public/cabinets/{id}")
    public BrandingDto.Response cabinet(@PathVariable Long id) {
        return portalService.activeCabinet(id);
    }

    @GetMapping("/api/public/cabinets/{id}/images/{kind}")
    public ResponseEntity<byte[]> image(@PathVariable Long id, @PathVariable String kind) {
        portalService.activeCabinet(id);
        return brandingService.image(id, kind)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(1)).cachePublic())
                        .header("X-Content-Type-Options", "nosniff")
                        .body(image.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/api/client/register")
    public ResponseEntity<ClientDto.AuthResponse> register(@Valid @RequestBody ClientDto.RegisterRequest request,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, http.getRemoteAddr()));
    }

    @PostMapping("/api/client/login")
    public ClientDto.AuthResponse login(@Valid @RequestBody ClientDto.LoginRequest request, HttpServletRequest http) {
        return authService.login(request, http.getRemoteAddr());
    }

    // ------------------------------------------------------------------ signed-in client

    @GetMapping("/api/client/me")
    public ClientDto.Account me(Principal principal) {
        return authService.me(accountId(principal));
    }

    /** Right of access: a download of everything held about the signed-in client. */
    @GetMapping("/api/client/me/export")
    public ResponseEntity<ClientDto.Export> export(Principal principal) {
        Long id = accountId(principal);
        ClientDto.Export export = new ClientDto.Export(java.time.LocalDateTime.now(), authService.me(id),
                portalService.memberships(id), portalService.myRequests(id));
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"my-data.json\"")
                .body(export);
    }

    /** Right to erasure: removes the account (confirmed with the password). */
    @DeleteMapping("/api/client/me")
    public ResponseEntity<Void> deleteMe(@Valid @RequestBody ClientDto.DeleteAccountRequest request, Principal principal) {
        authService.deleteAccount(accountId(principal), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/client/memberships")
    public List<ClientDto.Membership> memberships(Principal principal) {
        return portalService.memberships(accountId(principal));
    }

    @GetMapping("/api/client/cabinets/{cabinetId}/membership")
    public ClientDto.Membership membership(@PathVariable Long cabinetId, Principal principal) {
        return portalService.membership(accountId(principal), cabinetId);
    }

    @PutMapping("/api/client/cabinets/{cabinetId}/membership")
    public ClientDto.Membership join(@PathVariable Long cabinetId, @Valid @RequestBody ClientDto.JoinRequest request,
            Principal principal) {
        return portalService.join(accountId(principal), cabinetId, request);
    }

    @GetMapping("/api/client/cabinets/{cabinetId}/doctors")
    public List<ClientDto.Doctor> doctors(@PathVariable Long cabinetId, Principal principal) {
        return portalService.doctors(accountId(principal), cabinetId);
    }

    @GetMapping("/api/client/cabinets/{cabinetId}/doctors/{doctorId}/free-times")
    public List<ClientDto.SlotDay> freeTimes(@PathVariable Long cabinetId, @PathVariable Long doctorId,
            @RequestParam(required = false) LocalDate from, @RequestParam(defaultValue = "7") int days,
            Principal principal) {
        return portalService.freeTimes(accountId(principal), cabinetId, doctorId, from, days);
    }

    @PostMapping("/api/client/cabinets/{cabinetId}/requests")
    public ResponseEntity<ClientDto.MyRequest> request(@PathVariable Long cabinetId,
            @Valid @RequestBody ClientDto.BookingBody body, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalService.request(accountId(principal), cabinetId, body));
    }

    @GetMapping("/api/client/requests")
    public List<ClientDto.MyRequest> requests(Principal principal) {
        return portalService.myRequests(accountId(principal));
    }

    @PostMapping("/api/client/requests/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long id, Principal principal) {
        portalService.cancel(accountId(principal), id);
        return ResponseEntity.noContent().build();
    }

    private Long accountId(Principal principal) {
        ClinicPrincipal client = ClinicPrincipal.require(principal);
        if (!client.hasRole(ClinicPrincipal.CLIENT_ROLE)) {
            throw new com.example.demo.security.AuthenticationException("A client account is required.");
        }
        return client.userId();
    }
}

package com.example.demo.controller;

import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CatalogDto;
import com.example.demo.dto.HistoryDto;
import com.example.demo.dto.PhotoDto;
import com.example.demo.dto.PrescriptionDto;
import com.example.demo.dto.ProcedureDto;
import com.example.demo.dto.TreatmentDto;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.TreatmentService;

import jakarta.validation.Valid;

@RestController
public class TreatmentController {
    private final TreatmentService treatmentService;

    public TreatmentController(TreatmentService treatmentService) {
        this.treatmentService = treatmentService;
    }

    @GetMapping("/api/treatments/my-patients")
    public List<TreatmentDto.PatientSummary> myPatients(@RequestParam(required = false) String q,
            Principal principal) {
        return treatmentService.myPatients(requireDoctor(principal), q);
    }

    @GetMapping("/api/patients/{patientId}/treatments")
    public List<TreatmentDto.Response> byPatient(@PathVariable Long patientId, Principal principal) {
        return treatmentService.findByPatient(patientId, requireDoctor(principal));
    }

    @PostMapping("/api/patients/{patientId}/treatments")
    public ResponseEntity<TreatmentDto.Response> create(@PathVariable Long patientId,
            @Valid @RequestBody TreatmentDto.Request request, Principal principal) {
        TreatmentDto.Response response = treatmentService.create(patientId, request, requireDoctor(principal));
        return ResponseEntity.created(URI.create("/api/treatments/" + response.id())).body(response);
    }

    @GetMapping("/api/treatments/{id}")
    public TreatmentDto.Response get(@PathVariable Long id, Principal principal) {
        return treatmentService.findById(id, requireDoctor(principal));
    }

    @PutMapping("/api/treatments/{id}")
    public TreatmentDto.Response update(@PathVariable Long id, @Valid @RequestBody TreatmentDto.Request request,
            Principal principal) {
        return treatmentService.update(id, request, requireDoctor(principal));
    }

    @DeleteMapping("/api/treatments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        treatmentService.delete(id, requireDoctor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/treatments/{id}/procedures")
    public List<ProcedureDto.Response> procedures(@PathVariable Long id, Principal principal) {
        return treatmentService.procedures(id, requireDoctor(principal));
    }

    @PostMapping("/api/treatments/{id}/procedures")
    public ResponseEntity<ProcedureDto.Response> addProcedure(@PathVariable Long id,
            @Valid @RequestBody ProcedureDto.Request request, Principal principal) {
        ProcedureDto.Response response = treatmentService.addProcedure(id, request, requireDoctor(principal));
        return ResponseEntity.created(URI.create("/api/procedures/" + response.id())).body(response);
    }

    @PutMapping("/api/procedures/{id}")
    public ProcedureDto.Response updateProcedure(@PathVariable Long id,
            @Valid @RequestBody ProcedureDto.Request request, Principal principal) {
        return treatmentService.updateProcedure(id, request, requireDoctor(principal));
    }

    @DeleteMapping("/api/procedures/{id}")
    public ResponseEntity<Void> deleteProcedure(@PathVariable Long id, Principal principal) {
        treatmentService.deleteProcedure(id, requireDoctor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/treatments/{id}/timeline")
    public List<HistoryDto.Response> timeline(@PathVariable Long id, Principal principal) {
        return treatmentService.timeline(id, requireDoctor(principal));
    }

    @PostMapping("/api/treatments/{id}/history")
    public HistoryDto.Response addHistory(@PathVariable Long id, @Valid @RequestBody HistoryDto.Request request,
            Principal principal) {
        return treatmentService.addHistory(id, request, requireDoctor(principal));
    }

    @GetMapping("/api/treatments/{id}/photos")
    public List<PhotoDto.Response> photos(@PathVariable Long id, Principal principal) {
        return treatmentService.photos(id, requireDoctor(principal));
    }

    @PostMapping("/api/treatments/{id}/photos")
    public PhotoDto.Response addPhoto(@PathVariable Long id, @Valid @RequestBody PhotoDto.Request request,
            Principal principal) {
        return treatmentService.addPhoto(id, request, requireDoctor(principal));
    }

    @DeleteMapping("/api/photos/{id}")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id, Principal principal) {
        treatmentService.deletePhoto(id, requireDoctor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/treatments/{id}/prescriptions")
    public List<PrescriptionDto.Response> prescriptions(@PathVariable Long id, Principal principal) {
        return treatmentService.prescriptions(id, requireDoctor(principal));
    }

    @PostMapping("/api/treatments/{id}/prescriptions")
    public PrescriptionDto.Response addPrescription(@PathVariable Long id,
            @Valid @RequestBody PrescriptionDto.Request request, Principal principal) {
        return treatmentService.addPrescription(id, request, requireDoctor(principal));
    }

    @PutMapping("/api/prescriptions/{id}")
    public PrescriptionDto.Response updatePrescription(@PathVariable Long id,
            @Valid @RequestBody PrescriptionDto.Request request, Principal principal) {
        return treatmentService.updatePrescription(id, request, requireDoctor(principal));
    }

    @DeleteMapping("/api/prescriptions/{id}")
    public ResponseEntity<Void> deletePrescription(@PathVariable Long id, Principal principal) {
        treatmentService.deletePrescription(id, requireDoctor(principal));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/procedure-catalog")
    public List<CatalogDto.ProcedureCatalogResponse> catalog(@RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeInactive, Principal principal) {
        ClinicPrincipal actor = requireStaff(principal);
        return treatmentService.searchCatalog(q, includeInactive && actor.hasRole("doctor"), actor);
    }

    @PostMapping("/api/procedure-catalog")
    public CatalogDto.ProcedureCatalogResponse createCatalog(@Valid @RequestBody CatalogDto.Request request,
            Principal principal) {
        return treatmentService.createCatalog(request, requireDoctor(principal));
    }

    @PutMapping("/api/procedure-catalog/{id}")
    public CatalogDto.ProcedureCatalogResponse updateCatalog(@PathVariable Long id,
            @Valid @RequestBody CatalogDto.Request request, Principal principal) {
        return treatmentService.updateCatalog(id, request, requireDoctor(principal));
    }

    @DeleteMapping("/api/procedure-catalog/{id}")
    public ResponseEntity<Void> deactivateCatalog(@PathVariable Long id, Principal principal) {
        treatmentService.deactivateCatalog(id, requireDoctor(principal));
        return ResponseEntity.noContent().build();
    }

    private ClinicPrincipal requireDoctor(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor")) {
            throw new MessagingAccessDeniedException("Only doctors can access clinical treatment records.");
        }
        return actor;
    }

    private ClinicPrincipal requireStaff(Principal principal) {
        ClinicPrincipal actor = ClinicPrincipal.require(principal);
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Clinic staff access is required.");
        }
        return actor;
    }
}

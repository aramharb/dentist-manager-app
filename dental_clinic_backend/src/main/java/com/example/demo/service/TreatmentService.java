package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.CatalogDto;
import com.example.demo.dto.HistoryDto;
import com.example.demo.dto.PhotoDto;
import com.example.demo.dto.PrescriptionDto;
import com.example.demo.dto.ProcedureDto;
import com.example.demo.dto.TreatmentDto;
import com.example.demo.security.ClinicPrincipal;

public interface TreatmentService {
    List<TreatmentDto.PatientSummary> myPatients(ClinicPrincipal actor, String query);
    List<TreatmentDto.Response> findByPatient(Long patientId, ClinicPrincipal actor);
    TreatmentDto.Response findById(Long id, ClinicPrincipal actor);
    TreatmentDto.Response create(Long patientId, TreatmentDto.Request request, ClinicPrincipal actor);
    TreatmentDto.Response update(Long id, TreatmentDto.Request request, ClinicPrincipal actor);
    void delete(Long id, ClinicPrincipal actor);
    List<ProcedureDto.Response> procedures(Long treatmentId, ClinicPrincipal actor);
    ProcedureDto.Response addProcedure(Long treatmentId, ProcedureDto.Request request, ClinicPrincipal actor);
    ProcedureDto.Response updateProcedure(Long id, ProcedureDto.Request request, ClinicPrincipal actor);
    void deleteProcedure(Long id, ClinicPrincipal actor);
    List<HistoryDto.Response> timeline(Long treatmentId, ClinicPrincipal actor);
    HistoryDto.Response addHistory(Long treatmentId, HistoryDto.Request request, ClinicPrincipal actor);
    List<PhotoDto.Response> photos(Long treatmentId, ClinicPrincipal actor);
    PhotoDto.Response addPhoto(Long treatmentId, PhotoDto.Request request, ClinicPrincipal actor);
    void deletePhoto(Long id, ClinicPrincipal actor);
    List<PrescriptionDto.Response> prescriptions(Long treatmentId, ClinicPrincipal actor);
    PrescriptionDto.Response addPrescription(Long treatmentId, PrescriptionDto.Request request, ClinicPrincipal actor);
    PrescriptionDto.Response updatePrescription(Long id, PrescriptionDto.Request request, ClinicPrincipal actor);
    void deletePrescription(Long id, ClinicPrincipal actor);
    List<CatalogDto.ProcedureCatalogResponse> searchCatalog(String query, boolean includeInactive,
            ClinicPrincipal actor);
    CatalogDto.ProcedureCatalogResponse createCatalog(CatalogDto.Request request, ClinicPrincipal actor);
    CatalogDto.ProcedureCatalogResponse updateCatalog(Long id, CatalogDto.Request request, ClinicPrincipal actor);
    void deactivateCatalog(Long id, ClinicPrincipal actor);
}

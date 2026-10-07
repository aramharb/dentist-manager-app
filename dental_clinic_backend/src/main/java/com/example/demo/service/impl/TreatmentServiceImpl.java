package com.example.demo.service.impl;

import com.example.demo.tenant.CabinetContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CatalogDto;
import com.example.demo.dto.HistoryDto;
import com.example.demo.dto.PhotoDto;
import com.example.demo.dto.PrescriptionDto;
import com.example.demo.dto.ProcedureDto;
import com.example.demo.dto.TreatmentDto;
import com.example.demo.entity.HistoryEventType;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Patient;
import com.example.demo.entity.PhotoType;
import com.example.demo.entity.Prescription;
import com.example.demo.entity.ProcedureCatalog;
import com.example.demo.entity.ProcedureStatus;
import com.example.demo.entity.Treatment;
import com.example.demo.entity.TreatmentHistory;
import com.example.demo.entity.TreatmentPhoto;
import com.example.demo.entity.TreatmentPriority;
import com.example.demo.entity.TreatmentProcedure;
import com.example.demo.entity.TreatmentStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.TreatmentMapper;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.PrescriptionRepository;
import com.example.demo.repository.ProcedureCatalogRepository;
import com.example.demo.repository.ToothRepository;
import com.example.demo.repository.TreatmentHistoryRepository;
import com.example.demo.repository.TreatmentPhotoRepository;
import com.example.demo.repository.TreatmentProcedureRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.repository.TreatmentTypeRepository;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.BusinessRuleException;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.TreatmentRealtimeNotifier;
import com.example.demo.service.TreatmentService;

@Service
public class TreatmentServiceImpl implements TreatmentService {
    private final PatientRepository patientRepository;
    private final TreatmentRepository treatmentRepository;
    private final TreatmentTypeRepository treatmentTypeRepository;
    private final ProcedureCatalogRepository catalogRepository;
    private final ToothRepository toothRepository;
    private final TreatmentProcedureRepository procedureRepository;
    private final TreatmentHistoryRepository historyRepository;
    private final TreatmentPhotoRepository photoRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final LoginUserRepository userRepository;
    private final TreatmentMapper mapper;
    private final TreatmentRealtimeNotifier realtimeNotifier;

    public TreatmentServiceImpl(PatientRepository patientRepository, TreatmentRepository treatmentRepository,
            TreatmentTypeRepository treatmentTypeRepository, ProcedureCatalogRepository catalogRepository,
            ToothRepository toothRepository, TreatmentProcedureRepository procedureRepository,
            TreatmentHistoryRepository historyRepository, TreatmentPhotoRepository photoRepository,
            PrescriptionRepository prescriptionRepository, LoginUserRepository userRepository,
            TreatmentMapper mapper, TreatmentRealtimeNotifier realtimeNotifier) {
        this.patientRepository = patientRepository;
        this.treatmentRepository = treatmentRepository;
        this.treatmentTypeRepository = treatmentTypeRepository;
        this.catalogRepository = catalogRepository;
        this.toothRepository = toothRepository;
        this.procedureRepository = procedureRepository;
        this.historyRepository = historyRepository;
        this.photoRepository = photoRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.realtimeNotifier = realtimeNotifier;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDto.PatientSummary> myPatients(ClinicPrincipal actor, String query) {
        requireDoctor(actor);
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        return patientRepository.findByAssignedDoctorIdOrderByLastNameAscFirstNameAsc(actor.userId()).stream()
                .filter(patient -> normalized.isBlank() || patientSearchText(patient).contains(normalized))
                .map(patient -> toPatientSummary(patient, actor.userId()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TreatmentDto.Response> findByPatient(Long patientId, ClinicPrincipal actor) {
        requireOwnedPatient(patientId, actor);
        return treatmentRepository.findByPatientIdAndDoctorIdOrderByUpdatedAtDesc(patientId, actor.userId()).stream()
                .map(this::toTreatment).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TreatmentDto.Response findById(Long id, ClinicPrincipal actor) {
        return toTreatment(getOwnedTreatment(id, actor));
    }

    @Override
    @Transactional
    public TreatmentDto.Response create(Long patientId, TreatmentDto.Request request, ClinicPrincipal actor) {
        Patient patient = requireOwnedPatient(patientId, actor);
        Treatment treatment = new Treatment();
        treatment.setPatient(patient);
        treatment.setDoctor(requireActiveDoctor(actor.userId()));
        treatment.setEstimatedBill(BigDecimal.ZERO);
        treatment.setPaidAmount(BigDecimal.ZERO);
        treatment.setEstimatedDurationMinutes(0);
        treatment.setProgressPercent(0);
        applyTreatment(treatment, request);
        Treatment saved = treatmentRepository.save(treatment);
        addSystemHistory(saved, HistoryEventType.CREATED, "Treatment created", saved.getObjective(), actor.getName());
        if (patient.getRegistrationTreatment() == null) patient.setRegistrationTreatment(saved);
        syncRegistrationPatient(saved);
        realtimeNotifier.clinicalChange("TREATMENT_PLAN_CREATED", saved);
        return toTreatment(saved);
    }

    @Override
    @Transactional
    public TreatmentDto.Response update(Long id, TreatmentDto.Request request, ClinicPrincipal actor) {
        Treatment treatment = getOwnedTreatment(id, actor);
        applyTreatment(treatment, request);
        Treatment saved = treatmentRepository.save(treatment);
        syncRegistrationPatient(saved);
        realtimeNotifier.clinicalChange("TREATMENT_PLAN_UPDATED", saved);
        return toTreatment(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, ClinicPrincipal actor) {
        Treatment treatment = getOwnedTreatment(id, actor);
        realtimeNotifier.clinicalChange("TREATMENT_PLAN_DELETED", treatment);
        treatmentRepository.delete(treatment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureDto.Response> procedures(Long treatmentId, ClinicPrincipal actor) {
        getOwnedTreatment(treatmentId, actor);
        return proceduresFor(treatmentId).stream().map(mapper::toProcedure).toList();
    }

    @Override
    @Transactional
    public ProcedureDto.Response addProcedure(Long treatmentId, ProcedureDto.Request request,
            ClinicPrincipal actor) {
        Treatment treatment = getOwnedTreatment(treatmentId, actor);
        TreatmentProcedure procedure = new TreatmentProcedure();
        procedure.setTreatment(treatment);
        applyProcedure(procedure, request, actor, true);
        TreatmentProcedure saved = procedureRepository.saveAndFlush(procedure);
        if (saved.getStatus() == ProcedureStatus.COMPLETED) {
            addSystemHistory(treatment, HistoryEventType.PROCEDURE_COMPLETED,
                    "Procedure completed", saved.getName(), actor.getName());
        }
        recalculateTreatment(treatment);
        realtimeNotifier.clinicalChange("PROCEDURE_CREATED", treatment);
        return mapper.toProcedure(saved);
    }

    @Override
    @Transactional
    public ProcedureDto.Response updateProcedure(Long id, ProcedureDto.Request request, ClinicPrincipal actor) {
        TreatmentProcedure procedure = getProcedure(id);
        authorizeTreatment(actor, procedure.getTreatment());
        boolean wasCompleted = procedure.getStatus() == ProcedureStatus.COMPLETED;
        applyProcedure(procedure, request, actor, false);
        TreatmentProcedure saved = procedureRepository.saveAndFlush(procedure);
        if (!wasCompleted && saved.getStatus() == ProcedureStatus.COMPLETED) {
            addSystemHistory(saved.getTreatment(), HistoryEventType.PROCEDURE_COMPLETED,
                    "Procedure completed", saved.getName(), actor.getName());
        }
        recalculateTreatment(saved.getTreatment());
        realtimeNotifier.clinicalChange(saved.getStatus() == ProcedureStatus.COMPLETED
                ? "PROCEDURE_COMPLETED" : "PROCEDURE_UPDATED", saved.getTreatment());
        return mapper.toProcedure(saved);
    }

    @Override
    @Transactional
    public void deleteProcedure(Long id, ClinicPrincipal actor) {
        TreatmentProcedure procedure = getProcedure(id);
        Treatment treatment = procedure.getTreatment();
        authorizeTreatment(actor, treatment);
        procedureRepository.delete(procedure);
        procedureRepository.flush();
        recalculateTreatment(treatment);
        realtimeNotifier.clinicalChange("PROCEDURE_DELETED", treatment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistoryDto.Response> timeline(Long treatmentId, ClinicPrincipal actor) {
        getOwnedTreatment(treatmentId, actor);
        return historyRepository.findByTreatmentIdOrderByEventAtDesc(treatmentId).stream()
                .map(mapper::toHistory).toList();
    }

    @Override
    @Transactional
    public HistoryDto.Response addHistory(Long treatmentId, HistoryDto.Request request, ClinicPrincipal actor) {
        TreatmentHistory history = new TreatmentHistory();
        history.setTreatment(getOwnedTreatment(treatmentId, actor));
        history.setEventType(request.eventType() == null ? HistoryEventType.NOTE : request.eventType());
        history.setTitle(request.title());
        history.setDescription(request.description());
        history.setEventAt(request.eventAt() == null ? LocalDateTime.now() : request.eventAt());
        history.setCreatedBy(actor.getName());
        return mapper.toHistory(historyRepository.save(history));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PhotoDto.Response> photos(Long treatmentId, ClinicPrincipal actor) {
        getOwnedTreatment(treatmentId, actor);
        return photoRepository.findByTreatmentIdOrderByUploadedAtDesc(treatmentId).stream()
                .map(mapper::toPhoto).toList();
    }

    @Override
    @Transactional
    public PhotoDto.Response addPhoto(Long treatmentId, PhotoDto.Request request, ClinicPrincipal actor) {
        Treatment treatment = getOwnedTreatment(treatmentId, actor);
        TreatmentPhoto photo = new TreatmentPhoto();
        photo.setTreatment(treatment);
        photo.setPhotoType(request.photoType() == null ? PhotoType.OTHER : request.photoType());
        photo.setFileName(request.fileName());
        photo.setContentType(request.contentType());
        photo.setUrl(request.url());
        photo.setDescription(request.description());
        photo.setUploadedBy(actor.getName());
        TreatmentPhoto saved = photoRepository.save(photo);
        addSystemHistory(treatment, HistoryEventType.PHOTO_ADDED, "Photo added", saved.getFileName(), actor.getName());
        return mapper.toPhoto(saved);
    }

    @Override
    @Transactional
    public void deletePhoto(Long id, ClinicPrincipal actor) {
        TreatmentPhoto photo = photoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", id));
        authorizeTreatment(actor, photo.getTreatment());
        photoRepository.delete(photo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PrescriptionDto.Response> prescriptions(Long treatmentId, ClinicPrincipal actor) {
        getOwnedTreatment(treatmentId, actor);
        return prescriptionRepository.findByTreatmentIdOrderByIssuedAtDesc(treatmentId).stream()
                .map(mapper::toPrescription).toList();
    }

    @Override
    @Transactional
    public PrescriptionDto.Response addPrescription(Long treatmentId, PrescriptionDto.Request request,
            ClinicPrincipal actor) {
        Treatment treatment = getOwnedTreatment(treatmentId, actor);
        Prescription prescription = new Prescription();
        prescription.setTreatment(treatment);
        applyPrescription(prescription, request);
        Prescription saved = prescriptionRepository.save(prescription);
        addSystemHistory(treatment, HistoryEventType.PRESCRIPTION_GENERATED,
                "Prescription generated", saved.getMedicineName(), actor.getName());
        return mapper.toPrescription(saved);
    }

    @Override
    @Transactional
    public PrescriptionDto.Response updatePrescription(Long id, PrescriptionDto.Request request,
            ClinicPrincipal actor) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
        authorizeTreatment(actor, prescription.getTreatment());
        applyPrescription(prescription, request);
        return mapper.toPrescription(prescriptionRepository.save(prescription));
    }

    @Override
    @Transactional
    public void deletePrescription(Long id, ClinicPrincipal actor) {
        Prescription prescription = prescriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription", id));
        authorizeTreatment(actor, prescription.getTreatment());
        prescriptionRepository.delete(prescription);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogDto.ProcedureCatalogResponse> searchCatalog(String query, boolean includeInactive,
            ClinicPrincipal actor) {
        requireStaff(actor);
        String normalized = query == null ? "" : query.trim();
        List<ProcedureCatalog> results = includeInactive
                ? (normalized.isBlank() ? catalogRepository.findAllByOrderByNameAsc()
                        : catalogRepository.findByNameContainingIgnoreCaseOrderByNameAsc(normalized))
                : (normalized.isBlank() ? catalogRepository.findByActiveTrueOrderByNameAsc()
                        : catalogRepository.findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(normalized));
        return results.stream().map(mapper::toCatalog).toList();
    }

    @Override
    @Transactional
    public CatalogDto.ProcedureCatalogResponse createCatalog(CatalogDto.Request request, ClinicPrincipal actor) {
        requireDoctor(actor);
        String code = normalizeCode(request.code(), request.name());
        if (catalogRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("A catalog treatment with this code already exists.");
        }
        ProcedureCatalog catalog = new ProcedureCatalog();
        applyCatalog(catalog, request, code);
        ProcedureCatalog saved = catalogRepository.save(catalog);
        realtimeNotifier.catalogChange("TREATMENT_CATALOG_UPDATED", saved.getId());
        return mapper.toCatalog(saved);
    }

    @Override
    @Transactional
    public CatalogDto.ProcedureCatalogResponse updateCatalog(Long id, CatalogDto.Request request,
            ClinicPrincipal actor) {
        requireDoctor(actor);
        ProcedureCatalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog treatment", id));
        String code = normalizeCode(request.code(), request.name());
        if (catalogRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException("A catalog treatment with this code already exists.");
        }
        applyCatalog(catalog, request, code);
        ProcedureCatalog saved = catalogRepository.save(catalog);
        realtimeNotifier.catalogChange("TREATMENT_CATALOG_UPDATED", saved.getId());
        return mapper.toCatalog(saved);
    }

    @Override
    @Transactional
    public void deactivateCatalog(Long id, ClinicPrincipal actor) {
        requireDoctor(actor);
        ProcedureCatalog catalog = catalogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catalog treatment", id));
        catalog.setActive(false);
        catalogRepository.save(catalog);
        realtimeNotifier.catalogChange("TREATMENT_CATALOG_UPDATED", catalog.getId());
    }

    private TreatmentDto.PatientSummary toPatientSummary(Patient patient, Long doctorId) {
        Treatment latest = treatmentRepository.findByPatientIdAndDoctorIdOrderByUpdatedAtDesc(patient.getId(), doctorId)
                .stream().findFirst().orElse(null);
        return new TreatmentDto.PatientSummary(patient.getId(), patient.getPatientNumber(), patient.getFirstName(),
                patient.getLastName(), patient.getPhoneNumber(), latest == null ? patient.getCurrentTreatment() : latest.getObjective(),
                latest == null ? null : latest.getStatus(), latest == null ? 0 : latest.getProgressPercent(),
                patient.getAssignedDoctor() == null ? null : patient.getAssignedDoctor().getId(),
                patient.getAssignedDoctor() == null ? null : patient.getAssignedDoctor().getFullName(),
                latest == null
                        ? (patient.getLastVisit() == null ? null : patient.getLastVisit().atStartOfDay())
                        : latest.getLastVisit(),
                latest == null ? patient.getPaidAmount() : latest.getPaidAmount(),
                latest == null ? patient.getExpectedAmount() : latest.getEstimatedBill(),
                latest == null ? patient.getUnpaidBalance()
                        : latest.getEstimatedBill().subtract(latest.getPaidAmount()));
    }

    private String patientSearchText(Patient patient) {
        return (patient.getPatientNumber() + " " + patient.getFirstName() + " " + patient.getLastName() + " "
                + (patient.getPhoneNumber() == null ? "" : patient.getPhoneNumber())).toLowerCase(Locale.ROOT);
    }

    private TreatmentDto.Response toTreatment(Treatment treatment) {
        return mapper.toTreatment(treatment, proceduresFor(treatment.getId()));
    }

    private List<TreatmentProcedure> proceduresFor(Long treatmentId) {
        return procedureRepository.findByTreatmentIdOrderByToothNumberAscNameAsc(treatmentId);
    }

    private Patient requireOwnedPatient(Long patientId, ClinicPrincipal actor) {
        requireDoctor(actor);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));
        if (patient.getAssignedDoctor() == null || !actor.userId().equals(patient.getAssignedDoctor().getId())) {
            throw new MessagingAccessDeniedException("Doctors can only access their own patients.");
        }
        return patient;
    }

    private Treatment getOwnedTreatment(Long id, ClinicPrincipal actor) {
        Treatment treatment = treatmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Treatment", id));
        authorizeTreatment(actor, treatment);
        return treatment;
    }

    private void authorizeTreatment(ClinicPrincipal actor, Treatment treatment) {
        requireDoctor(actor);
        if (treatment.getDoctor() == null || !actor.userId().equals(treatment.getDoctor().getId())) {
            throw new MessagingAccessDeniedException("Doctors can only access their own treatment plans.");
        }
    }

    private TreatmentProcedure getProcedure(Long id) {
        return procedureRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Procedure", id));
    }

    private LoginUser requireActiveDoctor(Long id) {
        return userRepository.findById(id)
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && "doctor".equalsIgnoreCase(user.getRole())
                        && CabinetContext.isCurrent(user.getCabinetId()))
                .orElseThrow(() -> new ResourceNotFoundException("Active doctor", id));
    }

    private void applyTreatment(Treatment treatment, TreatmentDto.Request request) {
        treatment.setTreatmentType(request.treatmentTypeId() == null ? null
                : treatmentTypeRepository.findById(request.treatmentTypeId())
                        .orElseThrow(() -> new ResourceNotFoundException("Treatment type", request.treatmentTypeId())));
        treatment.setObjective(request.objective().trim());
        treatment.setStatus(request.status() == null ? TreatmentStatus.PLANNED : request.status());
        treatment.setPriority(request.priority() == null ? TreatmentPriority.NORMAL : request.priority());
        treatment.setUpcomingAppointment(request.upcomingAppointment());
        treatment.setLastVisit(request.lastVisit());
        treatment.setDoctorNotes(trim(request.doctorNotes()));
    }

    private void applyProcedure(TreatmentProcedure procedure, ProcedureDto.Request request,
            ClinicPrincipal actor, boolean creating) {
        ProcedureCatalog catalog = request.procedureCatalogId() == null ? null
                : catalogRepository.findById(request.procedureCatalogId())
                        .filter(item -> Boolean.TRUE.equals(item.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Active catalog treatment",
                                request.procedureCatalogId()));
        boolean allTeeth = Boolean.TRUE.equals(request.allTeeth());
        Integer toothNumber = allTeeth ? null : request.toothNumber();
        if (!allTeeth && toothNumber == null) {
            throw new IllegalArgumentException("Select a tooth before adding a procedure.");
        }
        procedure.setAllTeeth(allTeeth);
        procedure.setToothNumber(toothNumber);
        procedure.setTooth(allTeeth ? null : toothRepository.findByFdiNumber(toothNumber)
                .orElseThrow(() -> new IllegalArgumentException("The selected tooth number is invalid.")));
        procedure.setToothDescription(trim(request.toothDescription()));
        procedure.setProcedureCatalog(catalog);
        procedure.setName(catalog == null ? request.name().trim() : catalog.getName());
        BigDecimal requestedCost = request.cost();
        if (requestedCost == null) {
            if (catalog != null && creating) requestedCost = catalog.getDefaultCost();
            else if (!creating) requestedCost = procedure.getCost();
            else throw new IllegalArgumentException("A custom treatment price is required.");
        }
        procedure.setCost(requestedCost);
        procedure.setDurationMinutes(request.durationMinutes() == null
                ? (catalog == null ? 30 : catalog.getDefaultDurationMinutes()) : request.durationMinutes());
        ProcedureStatus status = request.status() == null ? ProcedureStatus.PLANNED : request.status();
        procedure.setStatus(status);
        procedure.setPractitioner(requireActiveDoctor(actor.userId()).getFullName());
        if (status == ProcedureStatus.IN_PROGRESS && procedure.getStartedAt() == null) {
            procedure.setStartedAt(LocalDateTime.now());
        }
        if (status == ProcedureStatus.COMPLETED) {
            if (procedure.getCompletedAt() == null) procedure.setCompletedAt(LocalDateTime.now());
            procedure.setCompletedBy(requireActiveDoctor(actor.userId()));
        } else {
            procedure.setCompletedAt(null);
            procedure.setCompletedBy(null);
        }
        procedure.setNotes(trim(request.notes()));
    }

    private void applyCatalog(ProcedureCatalog catalog, CatalogDto.Request request, String code) {
        catalog.setCode(code);
        catalog.setName(request.name().trim());
        catalog.setCategory(request.category() == null || request.category().isBlank()
                ? "General" : request.category().trim());
        catalog.setDefaultCost(request.defaultCost() == null ? BigDecimal.ZERO : request.defaultCost());
        catalog.setDefaultDurationMinutes(request.defaultDurationMinutes() == null ? 30
                : request.defaultDurationMinutes());
        catalog.setDescription(trim(request.description()));
        catalog.setActive(request.active() == null || request.active());
    }

    private String normalizeCode(String requestedCode, String name) {
        String source = requestedCode == null || requestedCode.isBlank() ? name : requestedCode;
        String code = source.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (code.isBlank()) throw new IllegalArgumentException("A valid catalog code is required.");
        return code.length() > 40 ? code.substring(0, 40) : code;
    }

    private void recalculateTreatment(Treatment treatment) {
        List<TreatmentProcedure> procedures = proceduresFor(treatment.getId());
        List<TreatmentProcedure> active = procedures.stream()
                .filter(procedure -> procedure.getStatus() != ProcedureStatus.CANCELLED).toList();
        long completed = active.stream().filter(procedure -> procedure.getStatus() == ProcedureStatus.COMPLETED).count();
        BigDecimal plannedValue = active.stream().map(TreatmentProcedure::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        treatment.setEstimatedDurationMinutes(active.stream().mapToInt(TreatmentProcedure::getDurationMinutes).sum());
        treatment.setProgressPercent(active.isEmpty() ? 0
                : (int) Math.round((completed * 100.0) / active.size()));
        treatment.setEstimatedBill(plannedValue.max(treatment.getPaidAmount()));
        if (treatment.getStatus() != TreatmentStatus.CANCELLED && treatment.getStatus() != TreatmentStatus.ON_HOLD) {
            if (!active.isEmpty() && completed == active.size()) treatment.setStatus(TreatmentStatus.COMPLETED);
            else if (completed > 0 || active.stream().anyMatch(p -> p.getStatus() == ProcedureStatus.IN_PROGRESS))
                treatment.setStatus(TreatmentStatus.IN_PROGRESS);
            else treatment.setStatus(TreatmentStatus.PLANNED);
        }
        treatmentRepository.save(treatment);
        syncRegistrationPatient(treatment);
    }

    private void addSystemHistory(Treatment treatment, HistoryEventType type, String title,
            String description, String actorName) {
        TreatmentHistory history = new TreatmentHistory();
        history.setTreatment(treatment);
        history.setEventType(type);
        history.setTitle(title);
        history.setDescription(description);
        history.setCreatedBy(actorName);
        historyRepository.save(history);
    }

    private void syncRegistrationPatient(Treatment treatment) {
        Patient patient = treatment.getPatient();
        Treatment registrationTreatment = patient.getRegistrationTreatment();
        if (registrationTreatment == null) patient.setRegistrationTreatment(treatment);
        else if (!registrationTreatment.getId().equals(treatment.getId())) return;
        patient.setCurrentTreatment(treatment.getObjective());
        patient.setExpectedAmount(treatment.getEstimatedBill());
        patient.setPaidAmount(treatment.getPaidAmount());
        patient.setUnpaidBalance(treatment.getEstimatedBill().subtract(treatment.getPaidAmount()).max(BigDecimal.ZERO));
        patient.setLastVisit(java.time.LocalDate.now());
        patientRepository.save(patient);
    }

    private void applyPrescription(Prescription prescription, PrescriptionDto.Request request) {
        prescription.setMedicineName(request.medicineName().trim());
        prescription.setDosage(request.dosage().trim());
        prescription.setDuration(request.duration().trim());
        prescription.setInstructions(request.instructions());
        prescription.setIssuedAt(request.issuedAt() == null ? LocalDateTime.now() : request.issuedAt());
        prescription.setPdfUrl(request.pdfUrl());
    }

    private void requireDoctor(ClinicPrincipal actor) {
        if (!actor.hasRole("doctor")) {
            throw new MessagingAccessDeniedException("Only doctors can access clinical treatment records.");
        }
    }

    private void requireStaff(ClinicPrincipal actor) {
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Clinic staff access is required.");
        }
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
}

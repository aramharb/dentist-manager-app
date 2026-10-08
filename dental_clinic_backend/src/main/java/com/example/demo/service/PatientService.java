package com.example.demo.service;

import com.example.demo.tenant.CabinetContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.controller.PatientRequest;
import com.example.demo.controller.PatientResponse;
import com.example.demo.entity.Patient;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ProcedureCatalogRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.entity.ProcedureCatalog;
import com.example.demo.entity.Treatment;
import com.example.demo.entity.LoginUser;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.security.ClinicPrincipal;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final ProcedureCatalogRepository procedureCatalogRepository;
    private final TreatmentRepository treatmentRepository;
    private final LoginUserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private DuplicateClientGuard duplicateGuard;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setDuplicateGuard(DuplicateClientGuard duplicateGuard) {
        this.duplicateGuard = duplicateGuard;
    }

    private void assertNotDuplicate(PatientRequest request, Long excludedPatientId) {
        if (duplicateGuard != null) {
            duplicateGuard.assertNoDuplicate(com.example.demo.tenant.CabinetContext.current(), request.getFirstName(),
                    request.getLastName(), request.getPhoneNumber(), excludedPatientId);
        }
    }

    public PatientService(PatientRepository patientRepository, ProcedureCatalogRepository procedureCatalogRepository,
            TreatmentRepository treatmentRepository, LoginUserRepository userRepository,
            AppointmentRepository appointmentRepository) {
        this.patientRepository = patientRepository;
        this.procedureCatalogRepository = procedureCatalogRepository;
        this.treatmentRepository = treatmentRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll(ClinicPrincipal actor) {
        requireStaff(actor);
        List<Patient> patients = actor.hasRole("doctor")
                ? patientRepository.findByAssignedDoctorIdOrderByLastNameAscFirstNameAsc(actor.userId())
                : patientRepository.findAll();
        return patients.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(Long id, ClinicPrincipal actor) {
        Patient patient = getPatient(id);
        authorizePatient(actor, patient);
        return toResponse(patient);
    }

    @Transactional
    public PatientResponse findByIdForUpdate(Long id) {
        return toResponse(patientRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PatientNotFoundException(id)));
    }

    @Transactional
    public PatientResponse create(PatientRequest request, ClinicPrincipal actor) {
        requireStaff(actor);
        assertNotDuplicate(request, null);
        Patient patient = new Patient();
        patient.setAssignedDoctor(resolveAssignedDoctor(request.getAssignedDoctorUserId(), actor));
        applyRequest(patient, request);
        patient.setPatientNumber(resolvePatientNumber(request.getPatientNumber()));
        Patient saved = patientRepository.save(patient);
        syncRegistrationTreatment(saved);
        return toResponse(patientRepository.save(saved));
    }

    @Transactional
    public PatientResponse update(Long id, PatientRequest request, ClinicPrincipal actor) {
        Patient patient = getPatient(id);
        authorizePatient(actor, patient);
        String requestedNumber = normalize(request.getPatientNumber());

        if (StringUtils.hasText(requestedNumber)
                && patientRepository.existsByPatientNumberIgnoreCaseAndIdNot(requestedNumber, id)) {
            throw new DuplicatePatientNumberException(requestedNumber);
        }

        assertNotDuplicate(request, id);
        patient.setAssignedDoctor(resolveAssignedDoctor(request.getAssignedDoctorUserId(), actor));
        applyRequest(patient, request);
        if (StringUtils.hasText(requestedNumber)) {
            patient.setPatientNumber(requestedNumber);
        }
        Patient saved = patientRepository.save(patient);
        syncRegistrationTreatment(saved);
        return toResponse(patientRepository.save(saved));
    }

    @Transactional
    public void delete(Long id, ClinicPrincipal actor) {
        Patient patient = patientRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PatientNotFoundException(id));
        authorizePatient(actor, patient);
        BigDecimal remainingBalance = defaultMoney(patient.getExpectedAmount())
                .subtract(defaultMoney(patient.getPaidAmount()));
        if (remainingBalance.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessRuleException("A patient with a remaining balance cannot be deleted.");
        }
        if (patientRepository.deleteByIdDirectly(id) != 1) {
            throw new PatientNotFoundException(id);
        }
    }

    @Transactional
    public PatientResponse restore(PatientResponse snapshot) {
        Patient patient = getPatient(snapshot.getId());
        patient.setPatientNumber(snapshot.getPatientNumber());
        patient.setFirstName(snapshot.getFirstName());
        patient.setLastName(snapshot.getLastName());
        patient.setGender(snapshot.getGender());
        patient.setBirthDate(snapshot.getBirthDate());
        patient.setAddress(snapshot.getAddress());
        patient.setPhoneNumber(snapshot.getPhoneNumber());
        patient.setEmail(snapshot.getEmail());
        patient.setBloodType(snapshot.getBloodType());
        patient.setAllergies(snapshot.getAllergies());
        patient.setCurrentTreatment(snapshot.getCurrentTreatment());
        patient.setSelectedTreatment(snapshot.getSelectedTreatmentId() == null ? null
                : procedureCatalogRepository.findById(snapshot.getSelectedTreatmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("Procedure catalog", snapshot.getSelectedTreatmentId())));
        patient.setExpectedAmount(defaultMoney(snapshot.getExpectedAmount()));
        patient.setPaidAmount(defaultMoney(snapshot.getPaidAmount()));
        patient.setUnpaidBalance(defaultMoney(snapshot.getUnpaidBalance()));
        patient.setCnamCovered(Boolean.TRUE.equals(snapshot.getCnamCovered()));
        patient.setCnamNumber(snapshot.getCnamNumber());
        patient.setFirstVisit(snapshot.getFirstVisit());
        patient.setLastVisit(snapshot.getLastVisit());
        patient.setNotes(snapshot.getNotes());
        patient.setAssignedDoctor(snapshot.getAssignedDoctorUserId() == null ? null
                : requireDoctor(snapshot.getAssignedDoctorUserId()));
        syncRegistrationTreatment(patient);
        return toResponse(patientRepository.save(patient));
    }

    private Patient getPatient(Long id) {
        return patientRepository.findById(id).orElseThrow(() -> new PatientNotFoundException(id));
    }

    private String resolvePatientNumber(String patientNumber) {
        String normalized = normalize(patientNumber);
        if (!StringUtils.hasText(normalized)) {
            normalized = "PAT-" + System.currentTimeMillis();
        }
        if (patientRepository.existsByPatientNumberIgnoreCase(normalized)) {
            throw new DuplicatePatientNumberException(normalized);
        }
        return normalized;
    }

    private void applyRequest(Patient patient, PatientRequest request) {
        patient.setFirstName(normalize(request.getFirstName()));
        patient.setLastName(normalize(request.getLastName()));
        patient.setGender(normalize(request.getGender()));
        patient.setBirthDate(request.getBirthDate());
        patient.setAddress(normalize(request.getAddress()));
        patient.setPhoneNumber(normalize(request.getPhoneNumber()));
        patient.setEmail(normalize(request.getEmail()));
        patient.setBloodType(normalize(request.getBloodType()));
        patient.setAllergies(normalize(request.getAllergies()));
        ProcedureCatalog selectedTreatment = request.getSelectedTreatmentId() == null
                ? null
                : procedureCatalogRepository.findById(request.getSelectedTreatmentId())
                        .filter(item -> Boolean.TRUE.equals(item.getActive()))
                        .orElseThrow(() -> new ResourceNotFoundException("Procedure catalog", request.getSelectedTreatmentId()));
        BigDecimal expectedAmount = selectedTreatment == null
                ? defaultMoney(request.getExpectedAmount())
                : selectedTreatment.getDefaultCost();
        BigDecimal paidAmount = defaultMoney(request.getPaidAmount());
        if (paidAmount.compareTo(expectedAmount) > 0) {
            throw new IllegalArgumentException("Paid amount cannot exceed the expected amount.");
        }

        patient.setSelectedTreatment(selectedTreatment);
        patient.setCurrentTreatment(selectedTreatment == null
                ? normalize(request.getCurrentTreatment())
                : selectedTreatment.getName());
        patient.setExpectedAmount(expectedAmount);
        patient.setPaidAmount(paidAmount);
        patient.setCnamCovered(Boolean.TRUE.equals(request.getCnamCovered()));
        patient.setCnamNumber(normalize(request.getCnamNumber()));
        patient.setFirstVisit(request.getFirstVisit() == null ? LocalDate.now() : request.getFirstVisit());
        patient.setLastVisit(LocalDate.now());
        patient.setUnpaidBalance(expectedAmount.subtract(paidAmount));
        patient.setNotes(normalize(request.getNotes()));
    }

    private PatientResponse toResponse(Patient patient) {
        PatientResponse response = new PatientResponse();
        response.setId(patient.getId());
        response.setPatientNumber(patient.getPatientNumber());
        response.setFirstName(patient.getFirstName());
        response.setLastName(patient.getLastName());
        response.setGender(patient.getGender());
        response.setBirthDate(patient.getBirthDate());
        response.setAddress(patient.getAddress());
        response.setPhoneNumber(patient.getPhoneNumber());
        response.setEmail(patient.getEmail());
        response.setBloodType(patient.getBloodType());
        response.setAllergies(patient.getAllergies());
        response.setCurrentTreatment(patient.getCurrentTreatment());
        response.setCnamCovered(patient.getCnamCovered());
        response.setCnamNumber(patient.getCnamNumber());
        response.setFirstVisit(patient.getFirstVisit());
        response.setLastVisit(patient.getLastVisit());
        response.setNextAppointment(appointmentRepository.findNextDateByPatientId(patient.getId()));
        response.setSelectedTreatmentId(patient.getSelectedTreatment() == null ? null : patient.getSelectedTreatment().getId());
        response.setExpectedAmount(patient.getExpectedAmount());
        response.setPaidAmount(patient.getPaidAmount());
        response.setUnpaidBalance(patient.getUnpaidBalance());
        response.setAssignedDoctorUserId(patient.getAssignedDoctor() == null ? null : patient.getAssignedDoctor().getId());
        response.setAssignedDoctorName(patient.getAssignedDoctor() == null ? null : patient.getAssignedDoctor().getFullName());
        Treatment registrationTreatment = patient.getRegistrationTreatment();
        if (registrationTreatment != null) {
            response.setRegistrationTreatmentId(registrationTreatment.getId());
            response.setTreatmentStatus(registrationTreatment.getStatus());
            response.setTreatmentPriority(registrationTreatment.getPriority());
            response.setTreatmentProgressPercent(registrationTreatment.getProgressPercent());
        }
        response.setNotes(patient.getNotes());
        response.setLastModified(patient.getLastModified());
        return response;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void syncRegistrationTreatment(Patient patient) {
        if (!StringUtils.hasText(patient.getCurrentTreatment())) {
            return;
        }

        Treatment treatment = patient.getRegistrationTreatment();
        if (treatment == null) {
            treatment = new Treatment();
            treatment.setPatient(patient);
            treatment.setDoctorNotes("Created from the patient registration treatment.");
        }
        treatment.setDoctor(patient.getAssignedDoctor());
        treatment.setObjective(patient.getCurrentTreatment());
        treatment.setEstimatedBill(patient.getExpectedAmount());
        treatment.setPaidAmount(patient.getPaidAmount());
        treatment.setLastVisit(LocalDateTime.now());
        patient.setRegistrationTreatment(treatmentRepository.save(treatment));
    }

    private LoginUser resolveAssignedDoctor(Long requestedDoctorId, ClinicPrincipal actor) {
        if (actor.hasRole("doctor")) return requireDoctor(actor.userId());
        if (!actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can manage patients.");
        }
        if (requestedDoctorId == null) {
            throw new IllegalArgumentException("An assigned doctor is required.");
        }
        return requireDoctor(requestedDoctorId);
    }

    private LoginUser requireDoctor(Long doctorUserId) {
        return userRepository.findById(doctorUserId)
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && "doctor".equalsIgnoreCase(user.getRole())
                        && CabinetContext.isCurrent(user.getCabinetId()))
                .orElseThrow(() -> new ResourceNotFoundException("Active doctor", doctorUserId));
    }

    private void authorizePatient(ClinicPrincipal actor, Patient patient) {
        requireStaff(actor);
        if (actor.hasRole("doctor") && (patient.getAssignedDoctor() == null
                || !actor.userId().equals(patient.getAssignedDoctor().getId()))) {
            throw new MessagingAccessDeniedException("Doctors can only access their own patients.");
        }
    }

    private void requireStaff(ClinicPrincipal actor) {
        if (!actor.hasRole("doctor") && !actor.hasRole("secretaire")) {
            throw new MessagingAccessDeniedException("Only doctors and secretaries can access patients.");
        }
    }
}

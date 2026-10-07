package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.controller.PatientRequest;
import com.example.demo.controller.PatientResponse;
import com.example.demo.entity.Patient;
import com.example.demo.entity.ProcedureCatalog;
import com.example.demo.entity.LoginUser;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.ProcedureCatalogRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.security.ClinicPrincipal;

class PatientServiceTests {

    private PatientRepository patientRepository;
    private ProcedureCatalogRepository catalogRepository;
    private PatientService patientService;
    private TreatmentRepository treatmentRepository;
    private LoginUserRepository userRepository;
    private AppointmentRepository appointmentRepository;
    private LoginUser doctor;
    private final ClinicPrincipal doctorPrincipal = new ClinicPrincipal(1L, "doctor", "doctor");

    @BeforeEach
    void setUp() {
        patientRepository = mock(PatientRepository.class);
        catalogRepository = mock(ProcedureCatalogRepository.class);
        treatmentRepository = mock(TreatmentRepository.class);
        userRepository = mock(LoginUserRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        doctor = mock(LoginUser.class);
        when(doctor.getId()).thenReturn(1L);
        when(doctor.getRole()).thenReturn("doctor");
        when(doctor.getActive()).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctor));
        patientService = new PatientService(patientRepository, catalogRepository, treatmentRepository, userRepository,
                appointmentRepository);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(treatmentRepository.save(any(com.example.demo.entity.Treatment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void predefinedTreatmentUsesCatalogPriceAndCalculatesBalance() {
        ProcedureCatalog catalog = mock(ProcedureCatalog.class);
        when(catalog.getId()).thenReturn(7L);
        when(catalog.getName()).thenReturn("Dental Crown");
        when(catalog.getDefaultCost()).thenReturn(new BigDecimal("650.00"));
        when(catalog.getActive()).thenReturn(true);
        when(catalogRepository.findById(7L)).thenReturn(Optional.of(catalog));

        PatientRequest request = validRequest();
        request.setSelectedTreatmentId(7L);
        request.setExpectedAmount(new BigDecimal("1.00"));
        request.setPaidAmount(new BigDecimal("200.00"));

        PatientResponse response = patientService.create(request, doctorPrincipal);

        assertEquals("Dental Crown", response.getCurrentTreatment());
        assertEquals(new BigDecimal("650.00"), response.getExpectedAmount());
        assertEquals(new BigDecimal("200.00"), response.getPaidAmount());
        assertEquals(new BigDecimal("450.00"), response.getUnpaidBalance());
        assertEquals(LocalDate.now(), response.getLastVisit());
    }

    @Test
    void customTreatmentRejectsPaymentAboveExpectedAmount() {
        PatientRequest request = validRequest();
        request.setCurrentTreatment("Custom treatment");
        request.setExpectedAmount(new BigDecimal("100.00"));
        request.setPaidAmount(new BigDecimal("101.00"));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> patientService.create(request, doctorPrincipal));

        assertEquals("Paid amount cannot exceed the expected amount.", error.getMessage());
    }

    @Test
    void deleteRemovesExistingPatient() {
        Patient patient = new Patient();
        patient.setAssignedDoctor(doctor);
        when(patientRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(patient));
        when(patientRepository.deleteByIdDirectly(12L)).thenReturn(1);

        patientService.delete(12L, doctorPrincipal);

        verify(patientRepository).deleteByIdDirectly(12L);
    }

    @Test
    void deleteRejectsPatientWithRemainingBalance() {
        Patient patient = new Patient();
        patient.setAssignedDoctor(doctor);
        patient.setExpectedAmount(new BigDecimal("100.00"));
        patient.setPaidAmount(new BigDecimal("75.00"));
        when(patientRepository.findByIdForUpdate(12L)).thenReturn(Optional.of(patient));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> patientService.delete(12L, doctorPrincipal));

        assertEquals("A patient with a remaining balance cannot be deleted.", error.getMessage());
        verify(patientRepository, never()).deleteByIdDirectly(12L);
    }

    private PatientRequest validRequest() {
        PatientRequest request = new PatientRequest();
        request.setFirstName("Test");
        request.setLastName("Patient");
        request.setGender("Female");
        request.setPhoneNumber("12345678");
        return request;
    }
}

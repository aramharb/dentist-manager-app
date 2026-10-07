package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.entity.AppointmentPriority;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.DoctorWorkingHours;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Patient;
import com.example.demo.entity.appointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorWorkingHoursRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.service.impl.AppointmentServiceImpl;

class AppointmentServiceImplTests {
    private static final LocalDate MONDAY = LocalDate.now().plusWeeks(1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    private static final Long PATIENT_ID = 10L;
    private static final Long DOCTOR_ID = 7L;

    private AppointmentRepository appointmentRepository;
    private PatientRepository patientRepository;
    private TreatmentRepository treatmentRepository;
    private LoginUserRepository userRepository;
    private DoctorWorkingHoursRepository hoursRepository;
    private AppointmentServiceImpl service;
    private Patient patient;
    private LoginUser doctor;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        patientRepository = mock(PatientRepository.class);
        treatmentRepository = mock(TreatmentRepository.class);
        userRepository = mock(LoginUserRepository.class);
        hoursRepository = mock(DoctorWorkingHoursRepository.class);
        service = new AppointmentServiceImpl(appointmentRepository, patientRepository, treatmentRepository,
                userRepository, hoursRepository);

        patient = mock(Patient.class);
        when(patient.getId()).thenReturn(PATIENT_ID);
        when(patient.getFirstName()).thenReturn("Test");
        when(patient.getLastName()).thenReturn("Patient");
        when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

        doctor = mock(LoginUser.class);
        when(doctor.getId()).thenReturn(DOCTOR_ID);
        when(doctor.getFullName()).thenReturn("Dr. Active");
        when(doctor.getRole()).thenReturn("doctor");
        when(doctor.getActive()).thenReturn(true);
        when(userRepository.findByIdForUpdate(DOCTOR_ID)).thenReturn(Optional.of(doctor));

        DoctorWorkingHours hours = new DoctorWorkingHours();
        hours.setDoctor(doctor);
        hours.setDayOfWeek(1);
        hours.setWorking(true);
        hours.setStartTime(LocalTime.of(8, 0));
        hours.setEndTime(LocalTime.of(17, 0));
        when(hoursRepository.findByDoctorIdAndDayOfWeek(DOCTOR_ID, 1)).thenReturn(Optional.of(hours));
        when(appointmentRepository.save(any(appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void overlapOrGapBelowThirtyMinutesIsRejected() {
        when(appointmentRepository.existsSchedulingConflict(DOCTOR_ID, MONDAY,
                LocalTime.of(9, 30), LocalTime.of(11, 0), null)).thenReturn(true);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.create(request(LocalTime.of(10, 0), LocalTime.of(10, 30))));

        assertEquals("This appointment overlaps with another appointment or is less than 30 minutes away.",
                error.getMessage());
        verify(appointmentRepository, never()).save(any(appointment.class));
    }

    @Test
    void exactlyThirtyMinuteGapIsAllowedAndDurationComesFromRange() {
        AppointmentResponse response = service.create(request(LocalTime.of(10, 0), LocalTime.of(10, 30)));

        assertEquals(LocalTime.of(10, 0), response.startTime());
        assertEquals(LocalTime.of(10, 30), response.endTime());
        assertEquals(30, response.durationMinutes());
        assertEquals("Dr. Active", response.providerName());
        verify(appointmentRepository).existsSchedulingConflict(DOCTOR_ID, MONDAY,
                LocalTime.of(9, 30), LocalTime.of(11, 0), null);
    }

    @Test
    void longAppointmentUsesItsCompleteRange() {
        AppointmentResponse response = service.create(request(LocalTime.of(9, 0), LocalTime.of(12, 0)));

        assertEquals(180, response.durationMinutes());
        verify(appointmentRepository).existsSchedulingConflict(DOCTOR_ID, MONDAY,
                LocalTime.of(8, 30), LocalTime.of(12, 30), null);
    }

    @Test
    void updateExcludesTheAppointmentBeingEdited() {
        appointment existing = entity(55L, AppointmentStatus.SCHEDULED);
        when(appointmentRepository.findByIdForUpdate(55L)).thenReturn(Optional.of(existing));

        service.update(55L, request(LocalTime.of(13, 0), LocalTime.of(14, 0)));

        verify(appointmentRepository).existsSchedulingConflict(DOCTOR_ID, MONDAY,
                LocalTime.of(12, 30), LocalTime.of(14, 30), 55L);
    }

    @Test
    void providerIdIsRequiredAndProviderNameIsNeverTrusted() {
        AppointmentRequest missingProvider = new AppointmentRequest(PATIENT_ID, null, MONDAY,
                LocalTime.of(10, 0), LocalTime.of(10, 30), null, null, null, "Dr. Spoofed",
                AppointmentPriority.NORMAL, AppointmentStatus.SCHEDULED, null);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> service.create(missingProvider));

        assertEquals("An active doctor must be selected.", error.getMessage());
        verify(appointmentRepository, never()).save(any(appointment.class));
    }

    @Test
    void inactiveOrNonDoctorProviderIsRejected() {
        when(doctor.getActive()).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(LocalTime.of(10, 0), LocalTime.of(10, 30))));

        verify(hoursRepository, never()).findByDoctorIdAndDayOfWeek(any(), any());
    }

    @Test
    void appointmentOutsideWorkingHoursIsRejected() {
        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.create(request(LocalTime.of(16, 30), LocalTime.of(17, 30))));

        assertEquals("The appointment must fit inside the doctor's working hours.", error.getMessage());
        verify(appointmentRepository, never()).existsSchedulingConflict(any(), any(), any(), any(), any());
    }

    @Test
    void cancellationIsIdempotent() {
        appointment cancelled = entity(77L, AppointmentStatus.CANCELLED);
        when(appointmentRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(cancelled));

        AppointmentResponse response = service.cancel(77L);

        assertEquals(AppointmentStatus.CANCELLED, response.status());
        verify(appointmentRepository, never()).save(any(appointment.class));
    }

    @Test
    void legacyStartAndDurationInputRemainsReadable() {
        AppointmentRequest legacy = new AppointmentRequest(PATIENT_ID, null, MONDAY, null, null,
                LocalTime.of(14, 0), 45, DOCTOR_ID, "Ignored", AppointmentPriority.NORMAL,
                AppointmentStatus.SCHEDULED, null);

        AppointmentResponse response = service.create(legacy);

        assertEquals(LocalTime.of(14, 0), response.startTime());
        assertEquals(LocalTime.of(14, 45), response.endTime());
        assertEquals(45, response.durationMinutes());
    }

    private AppointmentRequest request(LocalTime start, LocalTime end) {
        return new AppointmentRequest(PATIENT_ID, null, MONDAY, start, end, null, 999,
                DOCTOR_ID, "Dr. Spoofed", AppointmentPriority.NORMAL, AppointmentStatus.SCHEDULED, " test ");
    }

    private appointment entity(Long id, AppointmentStatus status) {
        appointment value = new appointment();
        value.setId(id);
        value.setPatient(patient);
        value.setDate(MONDAY);
        value.setHeure(LocalTime.of(10, 0));
        value.setEndTime(LocalTime.of(10, 30));
        value.setDurationMinutes(30);
        value.setProviderName("Dr. Active");
        value.setProviderUser(doctor);
        value.setPriority(AppointmentPriority.NORMAL);
        value.setStatus(status);
        return value;
    }
}

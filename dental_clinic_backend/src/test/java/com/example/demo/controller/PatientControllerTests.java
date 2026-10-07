package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;

import com.example.demo.security.ClinicPrincipal;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.PatientService;
import com.example.demo.service.StaffActionService;

class PatientControllerTests {
    private final PatientService patientService = mock(PatientService.class);
    private final PatientController controller = new PatientController(patientService, mock(StaffActionService.class));

    @Test
    void secretaryCannotDeletePatient() {
        ClinicPrincipal secretary = new ClinicPrincipal(2L, "secretaire", "secretaire");

        assertThrows(MessagingAccessDeniedException.class, () -> controller.delete(12L, secretary));

        verify(patientService, never()).delete(12L, secretary);
    }

    @Test
    void doctorCanRequestPatientDeletion() {
        ClinicPrincipal doctor = new ClinicPrincipal(1L, "doctor", "doctor");

        controller.delete(12L, doctor);

        verify(patientService).delete(12L, doctor);
    }
}

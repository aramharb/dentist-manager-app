package com.example.demo.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.example.demo.controller.PatientResponse;
import com.example.demo.dto.StaffActionDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.StaffAction;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.StaffActionRepository;
import com.example.demo.security.ClinicPrincipal;

import tools.jackson.databind.ObjectMapper;

class StaffActionServiceTests {
    private StaffActionRepository actionRepository;
    private LoginUserRepository userRepository;
    private PatientService patientService;
    private AppointmentService appointmentService;
    private ExpenseService expenseService;
    private SimpMessagingTemplate messagingTemplate;
    private ObjectMapper objectMapper;
    private StaffActionService service;
    private LoginUser secretary;
    private LoginUser doctorUser;

    @BeforeEach
    void setUp() {
        actionRepository = mock(StaffActionRepository.class);
        userRepository = mock(LoginUserRepository.class);
        patientService = mock(PatientService.class);
        appointmentService = mock(AppointmentService.class);
        expenseService = mock(ExpenseService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        objectMapper = new ObjectMapper();
        service = new StaffActionService(actionRepository, userRepository, patientService, appointmentService,
                expenseService, messagingTemplate, objectMapper);

        secretary = user(2L, "sawsen", "Sawsen", "secretaire");
        doctorUser = user(1L, "doctor", "Dr. Wajih", "doctor");
        when(userRepository.findById(1L)).thenReturn(Optional.of(doctorUser));
        when(userRepository.findById(2L)).thenReturn(Optional.of(secretary));
        when(actionRepository.save(any(StaffAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(actionRepository.saveAndFlush(any(StaffAction.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void balanceChangeCreatesClearAuditAndNotifiesDoctor() {
        when(userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(null, "doctor")).thenReturn(List.of(doctorUser));
        PatientResponse before = patient(12L, "Ahmed", "Ben Ali", "300.00");
        PatientResponse after = patient(12L, "Ahmed", "Ben Ali", "150.00");

        service.recordAction(new ClinicPrincipal(2L, "sawsen", "secretaire"),
                StaffActionService.PATIENT_BALANCE_UPDATED, StaffActionService.PATIENT, 12L, null, before, after);

        ArgumentCaptor<StaffAction> audit = ArgumentCaptor.forClass(StaffAction.class);
        verify(actionRepository).saveAndFlush(audit.capture());
        assertEquals("Sawsen updated Ahmed Ben Ali's balance\n300 DT → 150 DT", audit.getValue().getDescription());
        assertEquals(StaffActionService.ACTIVE, audit.getValue().getStatus());
        verify(messagingTemplate).convertAndSendToUser(eq("doctor"), eq("/queue/activity"), anyString());
    }

    @Test
    void doctorCanUndoBalanceWhenCurrentStateStillMatches() {
        PatientResponse before = patient(12L, "Ahmed", "Ben Ali", "300.00");
        PatientResponse after = patient(12L, "Ahmed", "Ben Ali", "150.00");
        StaffAction action = balanceAction(before, after);
        when(actionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(action));
        when(patientService.findByIdForUpdate(12L)).thenReturn(after);

        StaffActionDto.Response response = service.undo(5L, new ClinicPrincipal(1L, "doctor", "doctor"));

        ArgumentCaptor<PatientResponse> restored = ArgumentCaptor.forClass(PatientResponse.class);
        verify(patientService).restore(restored.capture());
        assertEquals(new BigDecimal("300.00"), restored.getValue().getUnpaidBalance());
        assertEquals(StaffActionService.UNDONE, response.status());
        assertEquals("Dr. Wajih", response.undoneBy());
        assertNotNull(response.undoneAt());
    }

    @Test
    void undoRejectsStaleActionInsteadOfOverwritingNewerBusinessData() {
        PatientResponse before = patient(12L, "Ahmed", "Ben Ali", "300.00");
        PatientResponse auditedAfter = patient(12L, "Ahmed", "Ben Ali", "150.00");
        PatientResponse current = patient(12L, "Ahmed", "Ben Ali", "100.00");
        StaffAction action = balanceAction(before, auditedAfter);
        when(actionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(action));
        when(patientService.findByIdForUpdate(12L)).thenReturn(current);

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.undo(5L, new ClinicPrincipal(1L, "doctor", "doctor")));

        assertEquals("Cannot undo because this data was modified after the original action.", error.getMessage());
        verify(patientService, never()).restore(any(PatientResponse.class));
        verify(actionRepository, never()).save(action);
    }

    @Test
    void sameActionCannotBeUndoneTwice() {
        StaffAction action = balanceAction(patient(12L, "Ahmed", "Ben Ali", "300.00"),
                patient(12L, "Ahmed", "Ben Ali", "150.00"));
        action.setStatus(StaffActionService.UNDONE);
        when(actionRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(action));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> service.undo(5L, new ClinicPrincipal(1L, "doctor", "doctor")));

        assertEquals("This action has already been undone.", error.getMessage());
        verify(patientService, never()).findByIdForUpdate(any());
    }

    @Test
    void secretaryCannotUndoAnAuditRecord() {
        MessagingAccessDeniedException error = assertThrows(MessagingAccessDeniedException.class,
                () -> service.undo(5L, new ClinicPrincipal(2L, "sawsen", "secretaire")));

        assertEquals("Only a doctor can review or undo secretary actions.", error.getMessage());
        verify(actionRepository, never()).findByIdForUpdate(any());
    }

    private StaffAction balanceAction(PatientResponse before, PatientResponse after) {
        StaffAction action = new StaffAction();
        action.setActor(secretary);
        action.setActionType(StaffActionService.PATIENT_BALANCE_UPDATED);
        action.setResourceType(StaffActionService.PATIENT);
        action.setResourceId(12L);
        action.setDescription("balance update");
        action.setBeforeState(objectMapper.writeValueAsString(before));
        action.setAfterState(objectMapper.writeValueAsString(after));
        action.setStatus(StaffActionService.ACTIVE);
        action.setUndoable(true);
        return action;
    }

    private PatientResponse patient(Long id, String firstName, String lastName, String balance) {
        PatientResponse patient = new PatientResponse();
        patient.setId(id);
        patient.setFirstName(firstName);
        patient.setLastName(lastName);
        patient.setExpectedAmount(new BigDecimal("300.00"));
        patient.setPaidAmount(new BigDecimal("300.00").subtract(new BigDecimal(balance)));
        patient.setUnpaidBalance(new BigDecimal(balance));
        return patient;
    }

    private LoginUser user(Long id, String username, String fullName, String role) {
        LoginUser user = mock(LoginUser.class);
        when(user.getId()).thenReturn(id);
        when(user.getUsername()).thenReturn(username);
        when(user.getFullName()).thenReturn(fullName);
        when(user.getRole()).thenReturn(role);
        when(user.getActive()).thenReturn(true);
        return user;
    }
}

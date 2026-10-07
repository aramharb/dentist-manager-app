package com.example.demo.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.controller.PatientResponse;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.dto.ExpenseDto;
import com.example.demo.dto.DoctorWorkingHoursDto;
import com.example.demo.dto.StaffActionDto;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.StaffAction;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.StaffActionRepository;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.tenant.CabinetContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
public class StaffActionService {
    public static final String PATIENT = "PATIENT";
    public static final String APPOINTMENT = "APPOINTMENT";
    public static final String EXPENSE = "EXPENSE";
    public static final String DOCTOR_AVAILABILITY = "DOCTOR_AVAILABILITY";

    public static final String PATIENT_CREATED = "PATIENT_CREATED";
    public static final String PATIENT_UPDATED = "PATIENT_UPDATED";
    public static final String PATIENT_BALANCE_UPDATED = "PATIENT_BALANCE_UPDATED";
    public static final String APPOINTMENT_CREATED = "APPOINTMENT_CREATED";
    public static final String APPOINTMENT_UPDATED = "APPOINTMENT_UPDATED";
    public static final String APPOINTMENT_CANCELLED = "APPOINTMENT_CANCELLED";
    public static final String EXPENSE_CREATED = "EXPENSE_CREATED";
    public static final String EXPENSE_UPDATED = "EXPENSE_UPDATED";
    public static final String EXPENSE_DELETED = "EXPENSE_DELETED";
    public static final String DOCTOR_AVAILABILITY_UPDATED = "DOCTOR_AVAILABILITY_UPDATED";

    public static final String ACTIVE = "ACTIVE";
    public static final String UNDONE = "UNDONE";
    private static final String UNDO_CONFLICT =
            "Cannot undo because this data was modified after the original action.";
    private static final DateTimeFormatter APPOINTMENT_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final StaffActionRepository actionRepository;
    private final LoginUserRepository userRepository;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final ExpenseService expenseService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public StaffActionService(StaffActionRepository actionRepository, LoginUserRepository userRepository,
            PatientService patientService, AppointmentService appointmentService, ExpenseService expenseService,
            SimpMessagingTemplate messagingTemplate, ObjectMapper objectMapper) {
        this.actionRepository = actionRepository;
        this.userRepository = userRepository;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.expenseService = expenseService;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void recordAction(ClinicPrincipal actor, String actionType, String resourceType, Long resourceId,
            String fallbackDescription, Object beforeState, Object afterState) {
        if (!actor.hasRole("secretaire")) return;

        LoginUser actorUser = requireUser(actor.userId());
        StaffAction action = new StaffAction();
        action.setActor(actorUser);
        action.setActionType(actionType);
        action.setResourceType(resourceType);
        action.setResourceId(resourceId);
        action.setDescription(buildDescription(actionType, actorUser.getFullName(), beforeState, afterState,
                fallbackDescription));
        action.setBeforeState(toJson(beforeState));
        action.setAfterState(toJson(afterState));
        action.setStatus(ACTIVE);
        action.setUndoable(isUndoable(actionType)
                && !isAppointmentProviderReassignment(resourceType, beforeState, afterState));
        StaffActionDto.Response response = toResponse(actionRepository.saveAndFlush(action));
        sendAfterCommit(response);
    }

    @Transactional(readOnly = true)
    public List<StaffActionDto.Response> recent(ClinicPrincipal viewer) {
        requireDoctor(viewer);
        return actionRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .filter(action -> canViewAction(viewer, action))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StaffActionDto.Response undo(Long actionId, ClinicPrincipal doctor) {
        requireDoctor(doctor);
        StaffAction action = actionRepository.findByIdForUpdate(actionId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff action", actionId));
        if (UNDONE.equals(action.getStatus()) || action.getUndoneAt() != null) {
            throw new BusinessRuleException("This action has already been undone.");
        }
        if (!action.isUndoable()) {
            throw new BusinessRuleException("This action cannot be undone safely.");
        }
        if (!canViewAction(doctor, action)) {
            throw new MessagingAccessDeniedException("Doctors can only undo actions for their own appointments.");
        }

        assertCurrentStateStillMatches(action);
        undoChange(action);
        action.setStatus(UNDONE);
        action.setUndoneAt(LocalDateTime.now());
        action.setUndoneBy(requireUser(doctor.userId()));
        StaffActionDto.Response response = toResponse(actionRepository.save(action));
        sendAfterCommit(response);
        return response;
    }

    private void assertCurrentStateStillMatches(StaffAction action) {
        Object currentState;
        if (EXPENSE.equals(action.getResourceType()) && EXPENSE_DELETED.equals(action.getActionType())) {
            if (expenseService.exists(action.getResourceId())) throw new BusinessRuleException(UNDO_CONFLICT);
            return;
        }

        try {
            currentState = switch (action.getResourceType()) {
                case PATIENT -> patientService.findByIdForUpdate(action.getResourceId());
                case APPOINTMENT -> appointmentService.findByIdForUpdate(action.getResourceId());
                case EXPENSE -> expenseService.findByIdForUpdate(action.getResourceId());
                default -> throw new BusinessRuleException("This type of action cannot be undone.");
            };
        } catch (ResourceNotFoundException | PatientNotFoundException exception) {
            throw new BusinessRuleException(UNDO_CONFLICT);
        }

        JsonNode expected = comparable(parseJson(action.getAfterState()));
        JsonNode current = comparable(parseJson(toJson(currentState)));
        if (expected == null || !expected.equals(current)) {
            throw new BusinessRuleException(UNDO_CONFLICT);
        }
    }

    private void undoChange(StaffAction action) {
        switch (action.getResourceType()) {
            case PATIENT -> undoPatient(action);
            case APPOINTMENT -> undoAppointment(action);
            case EXPENSE -> undoExpense(action);
            default -> throw new BusinessRuleException("This type of action cannot be undone.");
        }
    }

    private void undoPatient(StaffAction action) {
        if (PATIENT_UPDATED.equals(action.getActionType())
                || PATIENT_BALANCE_UPDATED.equals(action.getActionType())) {
            patientService.restore(read(action.getBeforeState(), PatientResponse.class));
        } else {
            throw new BusinessRuleException("This patient action cannot be undone.");
        }
    }

    private void undoAppointment(StaffAction action) {
        if (APPOINTMENT_CREATED.equals(action.getActionType())) {
            appointmentService.delete(action.getResourceId());
        } else if (APPOINTMENT_UPDATED.equals(action.getActionType())
                || APPOINTMENT_CANCELLED.equals(action.getActionType())) {
            appointmentService.restore(read(action.getBeforeState(), AppointmentResponse.class));
        } else {
            throw new BusinessRuleException("This appointment action cannot be undone.");
        }
    }

    private void undoExpense(StaffAction action) {
        if (EXPENSE_CREATED.equals(action.getActionType())) {
            expenseService.delete(action.getResourceId());
        } else if (EXPENSE_UPDATED.equals(action.getActionType())) {
            expenseService.restore(read(action.getBeforeState(), ExpenseDto.Response.class));
        } else if (EXPENSE_DELETED.equals(action.getActionType())) {
            expenseService.restoreDeleted(read(action.getBeforeState(), ExpenseDto.Response.class));
        } else {
            throw new BusinessRuleException("This expense action cannot be undone.");
        }
    }

    private StaffActionDto.Response toResponse(StaffAction action) {
        String status = action.getStatus() == null
                ? (action.getUndoneAt() == null ? ACTIVE : UNDONE)
                : action.getStatus();
        return new StaffActionDto.Response(action.getId(), action.getActor().getId(), action.getActor().getFullName(),
                action.getActionType(), action.getResourceType(), action.getResourceId(),
                parseJson(action.getBeforeState()), parseJson(action.getAfterState()), action.getDescription(),
                action.getCreatedAt(), status, action.isUndoable(),
                action.getUndoneBy() == null ? null : action.getUndoneBy().getFullName(), action.getUndoneAt());
    }

    private String buildDescription(String actionType, String actorName, Object beforeState, Object afterState,
            String fallback) {
        if (PATIENT_CREATED.equals(actionType) && afterState instanceof PatientResponse patient) {
            return actorName + " created patient " + patientName(patient);
        }
        if (PATIENT_BALANCE_UPDATED.equals(actionType)
                && beforeState instanceof PatientResponse before && afterState instanceof PatientResponse after) {
            return actorName + " updated " + patientName(after) + "'s balance\n"
                    + money(before.getUnpaidBalance()) + " DT → " + money(after.getUnpaidBalance()) + " DT";
        }
        if (PATIENT_UPDATED.equals(actionType) && afterState instanceof PatientResponse patient) {
            return actorName + " updated patient " + patientName(patient);
        }
        if (APPOINTMENT_CREATED.equals(actionType) && afterState instanceof AppointmentResponse appointment) {
            return actorName + " scheduled " + patientName(appointment) + "\n" + appointmentSlot(appointment);
        }
        if (APPOINTMENT_UPDATED.equals(actionType)
                && beforeState instanceof AppointmentResponse before && afterState instanceof AppointmentResponse after) {
            if (!Objects.equals(before.date(), after.date()) || !Objects.equals(before.heure(), after.heure())) {
                return actorName + " rescheduled " + patientName(after) + "\n"
                        + appointmentSlot(before) + " → " + appointmentSlot(after);
            }
            return actorName + " updated " + patientName(after) + "'s appointment";
        }
        if (APPOINTMENT_CANCELLED.equals(actionType) && afterState instanceof AppointmentResponse appointment) {
            return actorName + " cancelled " + patientName(appointment) + "'s appointment\n"
                    + appointmentSlot(appointment);
        }
        if (EXPENSE_CREATED.equals(actionType) && afterState instanceof ExpenseDto.Response expense) {
            return actorName + " created an expense of " + money(expense.amount()) + " DT";
        }
        if (EXPENSE_UPDATED.equals(actionType) && afterState instanceof ExpenseDto.Response expense) {
            return actorName + " updated an expense to " + money(expense.amount()) + " DT";
        }
        if (EXPENSE_DELETED.equals(actionType) && beforeState instanceof ExpenseDto.Response expense) {
            return actorName + " deleted an expense of " + money(expense.amount()) + " DT";
        }
        if (DOCTOR_AVAILABILITY_UPDATED.equals(actionType)
                && afterState instanceof DoctorWorkingHoursDto.Response hours) {
            return actorName + " updated " + hours.doctorName() + "'s working hours";
        }
        return fallback == null || fallback.isBlank() ? actorName + " performed " + actionType : fallback;
    }

    private boolean isUndoable(String actionType) {
        return !PATIENT_CREATED.equals(actionType) && !DOCTOR_AVAILABILITY_UPDATED.equals(actionType);
    }

    private boolean isAppointmentProviderReassignment(String resourceType, Object beforeState, Object afterState) {
        if (!APPOINTMENT.equals(resourceType)
                || !(beforeState instanceof AppointmentResponse before)
                || !(afterState instanceof AppointmentResponse after)) return false;
        return !Objects.equals(before.providerUserId(), after.providerUserId());
    }

    private boolean canViewAction(ClinicPrincipal doctor, StaffAction action) {
        if (!APPOINTMENT.equals(action.getResourceType())) return true;
        Long providerUserId = appointmentProviderId(
                parseJson(action.getAfterState()), parseJson(action.getBeforeState()));
        return doctor.userId().equals(providerUserId);
    }

    private Long appointmentProviderId(JsonNode primary, JsonNode fallback) {
        Long providerUserId = providerUserId(primary);
        return providerUserId == null ? providerUserId(fallback) : providerUserId;
    }

    private Long providerUserId(JsonNode state) {
        if (state == null || !state.isObject()) return null;
        JsonNode provider = state.get("providerUserId");
        return provider == null || provider.isNull() || !provider.isNumber() ? null : provider.asLong();
    }

    private String patientName(PatientResponse patient) {
        return (patient.getFirstName() + " " + patient.getLastName()).trim();
    }

    private String patientName(AppointmentResponse appointment) {
        return (appointment.patientFirstName() + " " + appointment.patientLastName()).trim();
    }

    private String appointmentSlot(AppointmentResponse appointment) {
        return appointment.date() + " " + appointment.effectiveStartTime().format(APPOINTMENT_TIME);
    }

    private String money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString();
    }

    private LoginUser requireUser(Long userId) {
        return userRepository.findById(userId).filter(user -> Boolean.TRUE.equals(user.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private void requireDoctor(ClinicPrincipal principal) {
        if (!principal.hasRole("doctor")) {
            throw new MessagingAccessDeniedException("Only a doctor can review or undo secretary actions.");
        }
    }

    private String toJson(Object value) {
        return value == null ? null : objectMapper.writeValueAsString(value);
    }

    private JsonNode parseJson(String value) {
        return value == null ? null : objectMapper.readTree(value);
    }

    private JsonNode comparable(JsonNode value) {
        if (value instanceof ObjectNode object) {
            ObjectNode copy = object.deepCopy();
            copy.remove("lastModified");
            return copy;
        }
        return value;
    }

    private <T> T read(String value, Class<T> type) {
        if (value == null) throw new BusinessRuleException("The previous state is unavailable.");
        return objectMapper.readValue(value, type);
    }

    private void sendAfterCommit(StaffActionDto.Response action) {
        Long cabinetId = CabinetContext.current();
        Runnable notification = () -> {
            String payload = objectMapper.writeValueAsString(action);
            if (APPOINTMENT.equals(action.entityType())) {
                Long providerUserId = appointmentProviderId(action.newValue(), action.oldValue());
                if (providerUserId == null) return;
                userRepository.findById(providerUserId)
                        .filter(doctor -> Boolean.TRUE.equals(doctor.getActive())
                                && "doctor".equalsIgnoreCase(doctor.getRole())
                                && Objects.equals(cabinetId, doctor.getCabinetId()))
                        .ifPresent(doctor -> messagingTemplate.convertAndSendToUser(
                                doctor.getUsername(), "/queue/activity", payload));
                return;
            }
            userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "doctor").forEach(doctor ->
                    messagingTemplate.convertAndSendToUser(doctor.getUsername(), "/queue/activity", payload));
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                notification.run();
            }
        });
    }
}

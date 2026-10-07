package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.entity.Treatment;
import com.example.demo.repository.LoginUserRepository;

import com.example.demo.tenant.CabinetContext;
import tools.jackson.databind.ObjectMapper;

@Service
public class TreatmentRealtimeNotifier {
    public record TreatmentEvent(String eventType, Long treatmentId, Long patientId,
            Long doctorUserId, Long catalogTreatmentId, LocalDateTime timestamp) {}

    private final SimpMessagingTemplate messagingTemplate;
    private final LoginUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public TreatmentRealtimeNotifier(SimpMessagingTemplate messagingTemplate,
            LoginUserRepository userRepository, ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public void clinicalChange(String eventType, Treatment treatment) {
        Long doctorId = treatment.getDoctor() == null ? null : treatment.getDoctor().getId();
        sendAfterCommit(new TreatmentEvent(eventType, treatment.getId(), treatment.getPatient().getId(),
                doctorId, null, LocalDateTime.now()), doctorId, false);
    }

    public void catalogChange(String eventType, Long catalogTreatmentId) {
        sendAfterCommit(new TreatmentEvent(eventType, null, null, null,
                catalogTreatmentId, LocalDateTime.now()), null, true);
    }

    private void sendAfterCommit(TreatmentEvent event, Long doctorId, boolean allDoctors) {
        Long cabinetId = CabinetContext.current();
        Runnable notification = () -> {
            if (cabinetId == null) return;
            String payload = objectMapper.writeValueAsString(event);
            Set<String> recipients = new LinkedHashSet<>();
            if (allDoctors) {
                userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "doctor")
                        .forEach(user -> recipients.add(user.getUsername()));
            } else if (doctorId != null) {
                userRepository.findById(doctorId).filter(user -> Boolean.TRUE.equals(user.getActive()))
                        .filter(user -> cabinetId.equals(user.getCabinetId()))
                        .ifPresent(user -> recipients.add(user.getUsername()));
            }
            userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "secretaire")
                    .forEach(user -> recipients.add(user.getUsername()));
            recipients.forEach(username -> messagingTemplate.convertAndSendToUser(
                    username, "/queue/treatments", payload));
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            notification.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() { notification.run(); }
        });
    }
}

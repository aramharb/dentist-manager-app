package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.example.demo.dto.AppointmentResponse;
import com.example.demo.dto.DoctorWorkingHoursDto;
import com.example.demo.repository.LoginUserRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class ScheduleRealtimeNotifier {
    public record ScheduleEvent(
            String eventType,
            Long appointmentId,
            Long doctorUserId,
            LocalDate date,
            LocalDateTime timestamp) {}

    private final SimpMessagingTemplate messagingTemplate;
    private final LoginUserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ScheduleRealtimeNotifier(SimpMessagingTemplate messagingTemplate, LoginUserRepository userRepository,
            ObjectMapper objectMapper) {
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public void appointmentChanged(String eventType, AppointmentResponse appointment) {
        appointmentChanged(eventType, appointment.id(), appointment.providerUserId(), appointment.date());
    }

    public void appointmentChanged(String eventType, Long appointmentId, Long doctorUserId, LocalDate date) {
        sendAfterCommit(new ScheduleEvent(eventType, appointmentId, doctorUserId, date, LocalDateTime.now()));
    }

    public void workingHoursChanged(DoctorWorkingHoursDto.Response hours) {
        sendAfterCommit(new ScheduleEvent(
                "WORKING_HOURS_UPDATED", null, hours.doctorUserId(), null, LocalDateTime.now()));
    }

    private void sendAfterCommit(ScheduleEvent event) {
        Runnable notification = () -> {
            String payload = objectMapper.writeValueAsString(event);
            Set<String> recipients = new LinkedHashSet<>();
            if (event.doctorUserId() != null) {
                userRepository.findById(event.doctorUserId())
                        .filter(user -> Boolean.TRUE.equals(user.getActive()))
                        .ifPresent(user -> recipients.add(user.getUsername()));
            }
            userRepository.findByRoleIgnoreCaseAndActiveTrue("secretaire")
                    .forEach(secretary -> recipients.add(secretary.getUsername()));
            recipients.forEach(username -> messagingTemplate.convertAndSendToUser(
                    username, "/queue/schedule", payload));
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

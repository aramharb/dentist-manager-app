package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.DashboardDto;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.TreatmentStatus;
import com.example.demo.entity.appointment;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.MessageNotificationRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.security.AuthenticationException;
import com.example.demo.security.ClinicPrincipal;

@Service
public class DashboardService {
    private final LoginUserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TreatmentRepository treatmentRepository;
    private final MessageNotificationRepository notificationRepository;
    private final AuthService authService;

    public DashboardService(LoginUserRepository userRepository, AppointmentRepository appointmentRepository,
            PatientRepository patientRepository, TreatmentRepository treatmentRepository,
            MessageNotificationRepository notificationRepository, AuthService authService) {
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.treatmentRepository = treatmentRepository;
        this.notificationRepository = notificationRepository;
        this.authService = authService;
    }

    @Transactional(readOnly = true)
    public DashboardDto.Response current(ClinicPrincipal principal) {
        LoginUser user = userRepository.findById(principal.userId())
                .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                .orElseThrow(() -> new AuthenticationException("Authenticated user is no longer active."));
        LocalDate today = LocalDate.now();
        boolean doctor = "doctor".equalsIgnoreCase(user.getRole());
        Long providerUserId = doctor ? user.getId() : null;
        List<appointment> appointments = appointmentRepository.dashboardAppointments(today, today.plusDays(7), providerUserId);
        long todayAppointments = appointments.stream().filter(item -> today.equals(item.getDate())).count();
        long waitingPatients = appointments.stream()
                .filter(item -> today.equals(item.getDate()))
                .filter(item -> item.getStatus() == AppointmentStatus.SCHEDULED
                        || item.getStatus() == AppointmentStatus.CONFIRMED)
                .count();
        List<DashboardDto.AppointmentItem> appointmentItems = appointments.stream().limit(12)
                .map(this::toAppointmentItem)
                .toList();

        return new DashboardDto.Response(
                authService.toSessionUser(user),
                today,
                todayAppointments,
                appointments.size(),
                waitingPatients,
                doctor ? patientRepository.countByAssignedDoctorId(user.getId()) : patientRepository.count(),
                doctor ? treatmentRepository.countByDoctorIdAndStatus(user.getId(), TreatmentStatus.IN_PROGRESS) : 0,
                notificationRepository.countByRecipientIdAndReadAtIsNull(user.getId()),
                appointmentItems);
    }

    private DashboardDto.AppointmentItem toAppointmentItem(appointment item) {
        String patientName = (item.getPatient().getFirstName() + " " + item.getPatient().getLastName()).trim();
        return new DashboardDto.AppointmentItem(item.getId(), item.getPatient().getId(), patientName,
                item.getDate(), item.getHeure(), item.getProviderName(), item.getStatus().name());
    }
}

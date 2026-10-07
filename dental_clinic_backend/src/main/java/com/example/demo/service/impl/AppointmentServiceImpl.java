package com.example.demo.service.impl;

import com.example.demo.tenant.CabinetContext;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.entity.AppointmentPriority;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.Patient;
import com.example.demo.entity.Treatment;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.appointment;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.AppointmentRepository;
import com.example.demo.repository.DoctorWorkingHoursRepository;
import com.example.demo.repository.PatientRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.service.AppointmentService;
import com.example.demo.service.BusinessRuleException;
import com.example.demo.service.ScheduleRealtimeNotifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AppointmentServiceImpl implements AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final TreatmentRepository treatmentRepository;
    private final LoginUserRepository userRepository;
    private final DoctorWorkingHoursRepository hoursRepository;
    private final ScheduleRealtimeNotifier scheduleNotifier;

    @Autowired
    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository,
                                  TreatmentRepository treatmentRepository, LoginUserRepository userRepository,
                                  DoctorWorkingHoursRepository hoursRepository,
                                  ScheduleRealtimeNotifier scheduleNotifier) {
        this.appointmentRepository = appointmentRepository;
        this.patientRepository = patientRepository;
        this.treatmentRepository = treatmentRepository;
        this.userRepository = userRepository;
        this.hoursRepository = hoursRepository;
        this.scheduleNotifier = scheduleNotifier;
    }

    public AppointmentServiceImpl(AppointmentRepository appointmentRepository, PatientRepository patientRepository,
                                  TreatmentRepository treatmentRepository, LoginUserRepository userRepository,
                                  DoctorWorkingHoursRepository hoursRepository) {
        this(appointmentRepository, patientRepository, treatmentRepository, userRepository, hoursRepository, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppointmentResponse> search(String query, LocalDate date, AppointmentStatus status,
            Long providerUserId) {
        String normalizedQuery = blankToNull(query);
        List<appointment> appointments = normalizedQuery == null
                ? appointmentRepository.search(date, status, providerUserId)
                : appointmentRepository.searchWithQuery(normalizedQuery, date, status, providerUserId);
        return appointments.stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentResponse findById(Long id) {
        return toResponse(getAppointment(id));
    }

    @Override
    @Transactional
    public AppointmentResponse findByIdForUpdate(Long id) {
        return toResponse(appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id)));
    }

    @Override
    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        appointment appointment = new appointment();
        apply(appointment, request, null);
        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));
        notifyAppointmentChanged("APPOINTMENT_CREATED", response);
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse update(Long id, AppointmentRequest request) {
        appointment appointment = appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        LocalDate previousDate = appointment.getDate();
        LocalTime previousStart = appointment.getHeure();
        LocalTime previousEnd = appointment.getEndTime();
        Long previousProviderId = appointment.getProviderUser() == null ? null : appointment.getProviderUser().getId();
        apply(appointment, request, id);
        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));
        boolean rescheduled = !java.util.Objects.equals(previousDate, response.date())
                || !java.util.Objects.equals(previousStart, response.effectiveStartTime())
                || !java.util.Objects.equals(previousEnd, response.effectiveEndTime())
                || !java.util.Objects.equals(previousProviderId, response.providerUserId());
        notifyAppointmentChanged(
                rescheduled ? "APPOINTMENT_RESCHEDULED" : "APPOINTMENT_UPDATED", response);
        if (rescheduled && previousProviderId != null && !previousProviderId.equals(response.providerUserId())) {
            notifyAppointmentChanged("APPOINTMENT_RESCHEDULED", id, previousProviderId, previousDate);
        }
        return response;
    }

    @Override
    @Transactional
    public AppointmentResponse cancel(Long id) {
        appointment appointment = appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) return toResponse(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        AppointmentResponse response = toResponse(appointmentRepository.save(appointment));
        notifyAppointmentChanged("APPOINTMENT_CANCELLED", response);
        return response;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        appointment appointment = getAppointment(id);
        AppointmentResponse deleted = toResponse(appointment);
        appointmentRepository.delete(appointment);
        notifyAppointmentChanged("APPOINTMENT_DELETED", deleted);
    }

    @Override
    @Transactional
    public void deleteCancelled(Long id) {
        appointment appointment = appointmentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        if (appointment.getStatus() != AppointmentStatus.CANCELLED) {
            throw new BusinessRuleException("Only cancelled appointments can be deleted.");
        }
        AppointmentResponse deleted = toResponse(appointment);
        appointmentRepository.delete(appointment);
        notifyAppointmentChanged("APPOINTMENT_DELETED", deleted);
    }

    @Override
    @Transactional
    public AppointmentResponse restore(AppointmentResponse snapshot) {
        return update(snapshot.id(), new AppointmentRequest(
                snapshot.patientId(), snapshot.treatmentId(), snapshot.date(), snapshot.effectiveStartTime(),
                snapshot.effectiveEndTime(), snapshot.heure(), snapshot.durationMinutes(), snapshot.providerUserId(),
                snapshot.providerName(), snapshot.priority(), snapshot.status(), snapshot.notes()));
    }

    private appointment getAppointment(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    private void apply(appointment appointment, AppointmentRequest request, Long excludedAppointmentId) {
        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.patientId()));
        Treatment treatment = null;
        if (request.treatmentId() != null) {
            treatment = treatmentRepository.findById(request.treatmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Treatment", request.treatmentId()));
            if (!treatment.getPatient().getId().equals(patient.getId())) {
                throw new IllegalArgumentException("Treatment does not belong to selected patient");
            }
        }

        LoginUser provider = resolveProvider(request);
        LocalTime startTime = request.effectiveStartTime();
        LocalTime endTime = request.effectiveEndTime();
        validateSchedule(request.date(), startTime, endTime, provider, excludedAppointmentId);

        appointment.setPatient(patient);
        appointment.setTreatment(treatment);
        appointment.setDate(request.date());
        appointment.setHeure(startTime);
        appointment.setEndTime(endTime);
        appointment.setDurationMinutes(Math.toIntExact(ChronoUnit.MINUTES.between(startTime, endTime)));
        appointment.setProviderName(provider.getFullName());
        appointment.setProviderUser(provider);
        appointment.setPriority(request.priority() == null ? AppointmentPriority.NORMAL : request.priority());
        appointment.setStatus(request.status() == null ? AppointmentStatus.SCHEDULED : request.status());
        appointment.setNotes(trim(request.notes()));
    }

    private LoginUser resolveProvider(AppointmentRequest request) {
        Long providerId = request.providerUserId();
        if (providerId == null) {
            throw new IllegalArgumentException("An active doctor must be selected.");
        }
        return userRepository.findByIdForUpdate(providerId)
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && "doctor".equalsIgnoreCase(user.getRole())
                        && CabinetContext.isCurrent(user.getCabinetId()))
                .orElseThrow(() -> new IllegalArgumentException("An active doctor must be selected."));
    }

    private void validateSchedule(LocalDate date, LocalTime startTime, LocalTime endTime, LoginUser provider,
            Long excludedAppointmentId) {
        if (date == null || startTime == null || endTime == null || !startTime.isBefore(endTime)) {
            throw new IllegalArgumentException("Appointment start time must be before end time on the same day.");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Appointments cannot be scheduled in the past.");
        }
        var workingHours = hoursRepository.findByDoctorIdAndDayOfWeek(provider.getId(), date.getDayOfWeek().getValue())
                .orElseThrow(() -> new BusinessRuleException("The selected doctor is not available on this day."));
        if (!workingHours.isWorking() || workingHours.getStartTime() == null || workingHours.getEndTime() == null
                || startTime.isBefore(workingHours.getStartTime()) || endTime.isAfter(workingHours.getEndTime())) {
            throw new BusinessRuleException("The appointment must fit inside the doctor's working hours.");
        }

        LocalTime bufferedStart = startTime.toSecondOfDay() < 30 * 60
                ? LocalTime.MIN : startTime.minusMinutes(30);
        LocalTime bufferedEnd = endTime.toSecondOfDay() > (23 * 60 + 29) * 60
                ? LocalTime.MAX : endTime.plusMinutes(30);
        if (appointmentRepository.existsSchedulingConflict(provider.getId(), date, bufferedStart, bufferedEnd,
                excludedAppointmentId)) {
            throw new BusinessRuleException(
                    "This appointment overlaps with another appointment or is less than 30 minutes away.");
        }
    }

    private AppointmentResponse toResponse(appointment appointment) {
        Patient patient = appointment.getPatient();
        Treatment treatment = appointment.getTreatment();
        return new AppointmentResponse(
                appointment.getId(),
                patient.getId(),
                patient.getFirstName(),
                patient.getLastName(),
                treatment == null ? null : treatment.getId(),
                treatment == null ? null : treatment.getObjective(),
                appointment.getDate(),
                appointment.getHeure(),
                appointment.getEndTime() == null
                        ? appointment.getHeure().plusMinutes(appointment.getDurationMinutes())
                        : appointment.getEndTime(),
                appointment.getHeure(),
                appointment.getDurationMinutes(),
                appointment.getProviderName(),
                appointment.getProviderUser() == null ? null : appointment.getProviderUser().getId(),
                appointment.getPriority(),
                appointment.getStatus(),
                appointment.getNotes()
        );
    }

    private void notifyAppointmentChanged(String eventType, AppointmentResponse response) {
        if (scheduleNotifier != null) scheduleNotifier.appointmentChanged(eventType, response);
    }

    private void notifyAppointmentChanged(String eventType, Long appointmentId, Long doctorUserId, LocalDate date) {
        if (scheduleNotifier != null) {
            scheduleNotifier.appointmentChanged(eventType, appointmentId, doctorUserId, date);
        }
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

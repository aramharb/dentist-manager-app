package com.example.demo.service;

import com.example.demo.tenant.CabinetContext;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.HashSet;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.dto.DoctorWorkingHoursDto;
import com.example.demo.entity.DoctorWorkingHours;
import com.example.demo.entity.LoginUser;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.DoctorWorkingHoursRepository;
import com.example.demo.repository.LoginUserRepository;

@Service
public class DoctorWorkingHoursService {
    private final DoctorWorkingHoursRepository hoursRepository;
    private final LoginUserRepository userRepository;
    private final ScheduleRealtimeNotifier scheduleNotifier;

    @Autowired
    public DoctorWorkingHoursService(DoctorWorkingHoursRepository hoursRepository, LoginUserRepository userRepository,
            ScheduleRealtimeNotifier scheduleNotifier) {
        this.hoursRepository = hoursRepository;
        this.userRepository = userRepository;
        this.scheduleNotifier = scheduleNotifier;
    }

    public DoctorWorkingHoursService(DoctorWorkingHoursRepository hoursRepository,
            LoginUserRepository userRepository) {
        this(hoursRepository, userRepository, null);
    }

    @Transactional(readOnly = true)
    public DoctorWorkingHoursDto.Response findByDoctor(Long doctorId) {
        LoginUser doctor = requireDoctor(doctorId, false);
        return response(doctor);
    }

    @Transactional
    public DoctorWorkingHoursDto.Response update(Long doctorId, DoctorWorkingHoursDto.UpdateRequest request) {
        LoginUser doctor = requireDoctor(doctorId, true);
        validateDays(request);
        var existing = hoursRepository.findByDoctorIdOrderByDayOfWeek(doctorId).stream()
                .collect(java.util.stream.Collectors.toMap(DoctorWorkingHours::getDayOfWeek, item -> item));
        for (DoctorWorkingHoursDto.Day day : request.days()) {
            DoctorWorkingHours hours = existing.getOrDefault(day.dayOfWeek(), new DoctorWorkingHours());
            hours.setDoctor(doctor);
            hours.setDayOfWeek(day.dayOfWeek());
            hours.setWorking(day.working());
            hours.setStartTime(day.working() ? day.startTime() : null);
            hours.setEndTime(day.working() ? day.endTime() : null);
            hoursRepository.save(hours);
        }
        hoursRepository.flush();
        DoctorWorkingHoursDto.Response response = response(doctor);
        if (scheduleNotifier != null) scheduleNotifier.workingHoursChanged(response);
        return response;
    }

    private void validateDays(DoctorWorkingHoursDto.UpdateRequest request) {
        HashSet<Integer> days = new HashSet<>();
        for (DoctorWorkingHoursDto.Day day : request.days()) {
            if (!days.add(day.dayOfWeek())) {
                throw new IllegalArgumentException("Each weekday must appear exactly once.");
            }
            if (day.working() && (day.startTime() == null || day.endTime() == null
                    || !day.startTime().isBefore(day.endTime()))) {
                throw new IllegalArgumentException("Working days require a valid start and end time.");
            }
        }
        if (days.size() != 7) throw new IllegalArgumentException("All seven weekdays are required.");
    }

    private LoginUser requireDoctor(Long doctorId, boolean lock) {
        LoginUser doctor = (lock ? userRepository.findByIdForUpdate(doctorId) : userRepository.findById(doctorId))
                .filter(user -> Boolean.TRUE.equals(user.getActive()) && "doctor".equalsIgnoreCase(user.getRole())
                        && CabinetContext.isCurrent(user.getCabinetId()))
                .orElseThrow(() -> new ResourceNotFoundException("Active doctor", doctorId));
        return doctor;
    }

    private DoctorWorkingHoursDto.Response response(LoginUser doctor) {
        var days = hoursRepository.findByDoctorIdOrderByDayOfWeek(doctor.getId()).stream()
                .sorted(Comparator.comparing(DoctorWorkingHours::getDayOfWeek))
                .map(item -> new DoctorWorkingHoursDto.Day(item.getDayOfWeek(), item.isWorking(),
                        item.getStartTime(), item.getEndTime()))
                .toList();
        return new DoctorWorkingHoursDto.Response(doctor.getId(), doctor.getFullName(), days);
    }
}

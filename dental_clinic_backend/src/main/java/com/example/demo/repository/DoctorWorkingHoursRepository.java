package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.DoctorWorkingHours;

public interface DoctorWorkingHoursRepository extends JpaRepository<DoctorWorkingHours, Long> {
    List<DoctorWorkingHours> findByDoctorIdOrderByDayOfWeek(Long doctorId);
    Optional<DoctorWorkingHours> findByDoctorIdAndDayOfWeek(Long doctorId, Integer dayOfWeek);
}

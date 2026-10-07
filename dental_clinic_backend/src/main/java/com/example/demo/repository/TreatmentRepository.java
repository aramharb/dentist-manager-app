package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Treatment;
import com.example.demo.entity.TreatmentStatus;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    List<Treatment> findByPatientIdOrderByUpdatedAtDesc(Long patientId);
    List<Treatment> findByPatientIdAndDoctorIdOrderByUpdatedAtDesc(Long patientId, Long doctorUserId);
    List<Treatment> findByStatusOrderByUpdatedAtDesc(TreatmentStatus status);
    long countByStatus(TreatmentStatus status);
    long countByDoctorIdAndStatus(Long doctorUserId, TreatmentStatus status);
}

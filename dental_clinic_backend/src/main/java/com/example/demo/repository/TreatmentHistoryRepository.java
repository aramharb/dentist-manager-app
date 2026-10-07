package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TreatmentHistory;

public interface TreatmentHistoryRepository extends JpaRepository<TreatmentHistory, Long> {
    List<TreatmentHistory> findByTreatmentIdOrderByEventAtDesc(Long treatmentId);
}

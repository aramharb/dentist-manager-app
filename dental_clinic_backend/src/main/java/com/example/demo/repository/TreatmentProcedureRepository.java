package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TreatmentProcedure;

public interface TreatmentProcedureRepository extends JpaRepository<TreatmentProcedure, Long> {
    List<TreatmentProcedure> findByTreatmentIdOrderByToothNumberAscNameAsc(Long treatmentId);
}

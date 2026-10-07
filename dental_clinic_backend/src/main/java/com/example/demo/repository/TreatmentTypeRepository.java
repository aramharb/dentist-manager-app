package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TreatmentType;

public interface TreatmentTypeRepository extends JpaRepository<TreatmentType, Long> {
}

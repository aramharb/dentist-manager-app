package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.TreatmentPhoto;

public interface TreatmentPhotoRepository extends JpaRepository<TreatmentPhoto, Long> {
    List<TreatmentPhoto> findByTreatmentIdOrderByUploadedAtDesc(Long treatmentId);
}

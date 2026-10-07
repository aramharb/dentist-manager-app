package com.example.demo.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.example.demo.entity.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {
    List<Patient> findByAssignedDoctorIdOrderByLastNameAscFirstNameAsc(Long doctorUserId);
    long countByAssignedDoctorId(Long doctorUserId);

    boolean existsByPatientNumberIgnoreCase(String patientNumber);

    boolean existsByPatientNumberIgnoreCaseAndIdNot(String patientNumber, Long id);

    Optional<Patient> findByPatientNumberIgnoreCase(String patientNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Patient p where p.id = :id")
    Optional<Patient> findByIdForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "delete from patient where id = :id", nativeQuery = true)
    int deleteByIdDirectly(@Param("id") Long id);
}

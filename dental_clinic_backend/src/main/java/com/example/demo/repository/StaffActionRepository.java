package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import com.example.demo.entity.StaffAction;

public interface StaffActionRepository extends JpaRepository<StaffAction, Long> {
    List<StaffAction> findTop100ByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from StaffAction a where a.id = :id")
    Optional<StaffAction> findByIdForUpdate(@Param("id") Long id);
}

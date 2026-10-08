package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.example.demo.entity.LoginUser;

public interface LoginUserRepository extends JpaRepository<LoginUser, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from LoginUser u where u.id = :id")
    Optional<LoginUser> lockMessagingUser(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from LoginUser u where u.id = :id")
    Optional<LoginUser> findByIdForUpdate(@Param("id") Long id);
    Optional<LoginUser> findByUsernameIgnoreCaseAndActiveTrue(String username);
    List<LoginUser> findByCabinetIdAndActiveTrueOrderByRoleAscFullNameAsc(Long cabinetId);
    List<LoginUser> findAllByOrderByRoleAscFullNameAsc();
    List<LoginUser> findByCabinetIdOrderByRoleAscFullNameAsc(Long cabinetId);
    boolean existsByUsernameIgnoreCase(String username);
    long countByRoleIgnoreCaseAndActiveTrue(String role);
    List<LoginUser> findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(Long cabinetId, String role);
    long countByCabinetId(Long cabinetId);
    boolean existsByCabinetIdAndRoleIgnoreCaseAndFullNameIgnoreCaseAndActiveTrue(Long cabinetId, String role, String fullName);
    boolean existsByCabinetIdAndRoleIgnoreCaseAndFullNameIgnoreCaseAndActiveTrueAndIdNot(Long cabinetId, String role, String fullName, Long id);
    long countByCabinetIdAndRoleIgnoreCaseAndActiveTrue(Long cabinetId, String role);

    @Query("select count(c) > 0 from Cabinet c where c.id = :cabinetId and c.active = true")
    boolean isCabinetActive(@Param("cabinetId") Long cabinetId);
}

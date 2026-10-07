package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.Cabinet;

public interface CabinetRepository extends JpaRepository<Cabinet, Long> {
    List<Cabinet> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}

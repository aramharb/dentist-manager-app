package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entity.ProcedureCatalog;

public interface ProcedureCatalogRepository extends JpaRepository<ProcedureCatalog, Long> {
    List<ProcedureCatalog> findByActiveTrueAndNameContainingIgnoreCaseOrderByNameAsc(String query);
    List<ProcedureCatalog> findByActiveTrueOrderByNameAsc();
    List<ProcedureCatalog> findByNameContainingIgnoreCaseOrderByNameAsc(String query);
    List<ProcedureCatalog> findAllByOrderByNameAsc();
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);
}

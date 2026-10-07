package com.example.demo.entity;

import org.hibernate.annotations.TenantId;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * Base class of every entity that belongs to one cabinet. Hibernate fills {@code cabinetId}
 * on insert and adds it to every query and lookup, so a cabinet can only ever touch its
 * own rows.
 */
@MappedSuperclass
public abstract class CabinetOwned {
    @TenantId
    @Column(name = "cabinet_id", nullable = false, updatable = false)
    private Long cabinetId;

    public Long getCabinetId() {
        return cabinetId;
    }
}

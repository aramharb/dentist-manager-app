package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "procedure_catalog")
public class ProcedureCatalog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Column(nullable = false, unique = true, length = 40)
    private String code;
    @NotBlank @Column(nullable = false, length = 140)
    private String name;
    @NotBlank @Column(nullable = false, length = 80)
    private String category;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal defaultCost = BigDecimal.ZERO;
    @Positive @Column(nullable = false)
    private Integer defaultDurationMinutes = 30;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private Boolean active = true;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getDefaultCost() { return defaultCost; }
    public void setDefaultCost(BigDecimal defaultCost) { this.defaultCost = defaultCost; }
    public Integer getDefaultDurationMinutes() { return defaultDurationMinutes; }
    public void setDefaultDurationMinutes(Integer defaultDurationMinutes) { this.defaultDurationMinutes = defaultDurationMinutes; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

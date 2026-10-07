package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "treatment_procedure")
public class TreatmentProcedure extends CabinetOwned {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "procedure_catalog_id")
    private ProcedureCatalog procedureCatalog;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "tooth_id")
    private Tooth tooth;
    @Min(11) @Max(48)
    private Integer toothNumber;
    @Column(nullable = false)
    private Boolean allTeeth = false;
    @Column(columnDefinition = "text")
    private String toothDescription;
    @NotBlank @Column(nullable = false, length = 140)
    private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ProcedureStatus status = ProcedureStatus.PLANNED;
    @Column(length = 120)
    private String practitioner;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;
    @Positive @Column(nullable = false)
    private Integer durationMinutes = 30;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "completed_by_user_id")
    private LoginUser completedBy;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Treatment getTreatment() { return treatment; }
    public void setTreatment(Treatment treatment) { this.treatment = treatment; }
    public ProcedureCatalog getProcedureCatalog() { return procedureCatalog; }
    public void setProcedureCatalog(ProcedureCatalog procedureCatalog) { this.procedureCatalog = procedureCatalog; }
    public Tooth getTooth() { return tooth; }
    public void setTooth(Tooth tooth) { this.tooth = tooth; }
    public Integer getToothNumber() { return toothNumber; }
    public void setToothNumber(Integer toothNumber) { this.toothNumber = toothNumber; }
    public Boolean getAllTeeth() { return allTeeth; }
    public void setAllTeeth(Boolean allTeeth) { this.allTeeth = allTeeth; }
    public String getToothDescription() { return toothDescription; }
    public void setToothDescription(String toothDescription) { this.toothDescription = toothDescription; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ProcedureStatus getStatus() { return status; }
    public void setStatus(ProcedureStatus status) { this.status = status; }
    public String getPractitioner() { return practitioner; }
    public void setPractitioner(String practitioner) { this.practitioner = practitioner; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LoginUser getCompletedBy() { return completedBy; }
    public void setCompletedBy(LoginUser completedBy) { this.completedBy = completedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

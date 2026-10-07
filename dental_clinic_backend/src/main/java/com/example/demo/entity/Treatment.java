package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "treatment")
public class Treatment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "doctor_user_id")
    private LoginUser doctor;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "treatment_type_id")
    private TreatmentType treatmentType;
    @NotBlank @Column(nullable = false, length = 220)
    private String objective;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private TreatmentStatus status = TreatmentStatus.PLANNED;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private TreatmentPriority priority = TreatmentPriority.NORMAL;
    @Min(0) @Max(100) @Column(nullable = false)
    private Integer progressPercent = 0;
    @PositiveOrZero @Column(nullable = false)
    private Integer estimatedDurationMinutes = 0;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedBill = BigDecimal.ZERO;
    @PositiveOrZero @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private LocalDateTime upcomingAppointment;
    private LocalDateTime lastVisit;
    @Column(columnDefinition = "text")
    private String doctorNotes;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @OneToMany(mappedBy = "treatment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TreatmentProcedure> procedures = new ArrayList<>();
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public LoginUser getDoctor() { return doctor; }
    public void setDoctor(LoginUser doctor) { this.doctor = doctor; }
    public TreatmentType getTreatmentType() { return treatmentType; }
    public void setTreatmentType(TreatmentType treatmentType) { this.treatmentType = treatmentType; }
    public String getObjective() { return objective; }
    public void setObjective(String objective) { this.objective = objective; }
    public TreatmentStatus getStatus() { return status; }
    public void setStatus(TreatmentStatus status) { this.status = status; }
    public TreatmentPriority getPriority() { return priority; }
    public void setPriority(TreatmentPriority priority) { this.priority = priority; }
    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }
    public Integer getEstimatedDurationMinutes() { return estimatedDurationMinutes; }
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) { this.estimatedDurationMinutes = estimatedDurationMinutes; }
    public BigDecimal getEstimatedBill() { return estimatedBill; }
    public void setEstimatedBill(BigDecimal estimatedBill) { this.estimatedBill = estimatedBill; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }
    public LocalDateTime getUpcomingAppointment() { return upcomingAppointment; }
    public void setUpcomingAppointment(LocalDateTime upcomingAppointment) { this.upcomingAppointment = upcomingAppointment; }
    public LocalDateTime getLastVisit() { return lastVisit; }
    public void setLastVisit(LocalDateTime lastVisit) { this.lastVisit = lastVisit; }
    public String getDoctorNotes() { return doctorNotes; }
    public void setDoctorNotes(String doctorNotes) { this.doctorNotes = doctorNotes; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}

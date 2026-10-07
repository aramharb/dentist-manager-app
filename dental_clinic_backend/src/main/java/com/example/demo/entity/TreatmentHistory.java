package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "treatment_history")
public class TreatmentHistory extends CabinetOwned {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private HistoryEventType eventType = HistoryEventType.NOTE;
    @NotBlank @Column(nullable = false, length = 160)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private LocalDateTime eventAt = LocalDateTime.now();
    @Column(length = 120)
    private String createdBy;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; if (eventAt == null) eventAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Treatment getTreatment() { return treatment; }
    public void setTreatment(Treatment treatment) { this.treatment = treatment; }
    public HistoryEventType getEventType() { return eventType; }
    public void setEventType(HistoryEventType eventType) { this.eventType = eventType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getEventAt() { return eventAt; }
    public void setEventAt(LocalDateTime eventAt) { this.eventAt = eventAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}

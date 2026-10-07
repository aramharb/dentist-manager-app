package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "treatment_photo")
public class TreatmentPhoto extends CabinetOwned {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private PhotoType photoType = PhotoType.OTHER;
    @NotBlank @Column(nullable = false, length = 220)
    private String fileName;
    @NotBlank @Column(nullable = false, length = 120)
    private String contentType;
    @NotBlank @Column(nullable = false, columnDefinition = "text")
    private String url;
    @Column(columnDefinition = "text")
    private String description;
    @Column(length = 120)
    private String uploadedBy;
    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    @PrePersist void prePersist() { createdAt = LocalDateTime.now(); updatedAt = createdAt; if (uploadedAt == null) uploadedAt = createdAt; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
    public Long getId() { return id; }
    public Treatment getTreatment() { return treatment; }
    public void setTreatment(Treatment treatment) { this.treatment = treatment; }
    public PhotoType getPhotoType() { return photoType; }
    public void setPhotoType(PhotoType photoType) { this.photoType = photoType; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}

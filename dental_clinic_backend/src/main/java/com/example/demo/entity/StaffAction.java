package com.example.demo.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "staff_action")
public class StaffAction extends CabinetOwned {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_user_id", nullable = false)
    private LoginUser actor;

    @Column(nullable = false, length = 64)
    private String actionType;

    @Column(nullable = false, length = 30)
    private String resourceType;

    @Column(nullable = false)
    private Long resourceId;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(columnDefinition = "text")
    private String beforeState;

    @Column(columnDefinition = "text")
    private String afterState;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(nullable = false)
    private boolean undoable = true;

    private LocalDateTime undoneAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "undone_by_user_id")
    private LoginUser undoneBy;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LoginUser getActor() { return actor; }
    public void setActor(LoginUser actor) { this.actor = actor; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }
    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBeforeState() { return beforeState; }
    public void setBeforeState(String beforeState) { this.beforeState = beforeState; }
    public String getAfterState() { return afterState; }
    public void setAfterState(String afterState) { this.afterState = afterState; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public boolean isUndoable() { return undoable; }
    public void setUndoable(boolean undoable) { this.undoable = undoable; }
    public LocalDateTime getUndoneAt() { return undoneAt; }
    public void setUndoneAt(LocalDateTime undoneAt) { this.undoneAt = undoneAt; }
    public LoginUser getUndoneBy() { return undoneBy; }
    public void setUndoneBy(LoginUser undoneBy) { this.undoneBy = undoneBy; }
}

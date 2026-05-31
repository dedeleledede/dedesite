package com.zavan.dedesite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "stars")
public class Star {
    public enum Status {
        NEBULA, READY, SCHEDULED, IN_PROGRESS, DONE, BLOCKED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private java.util.UUID publicId = java.util.UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star_system_id")
    private StarSystem starSystem;

    @NotBlank
    @Column(name = "encrypted_title", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String title;

    @Column(name = "encrypted_description", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.NEBULA;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    private StarSystem.EnergyType energyType = StarSystem.EnergyType.LIGHT_ADMIN;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dueDate;

    private Integer estimatedMinutes;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledStart;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime scheduledEnd;

    private LocalDateTime completedAt;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Integer encryptionKeyVersion = 1;

    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public boolean isCompleted() {
        return status == Status.DONE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.util.UUID getPublicId() { return publicId; }
    public void setPublicId(java.util.UUID publicId) { this.publicId = publicId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public StarSystem getStarSystem() { return starSystem; }
    public void setStarSystem(StarSystem starSystem) { this.starSystem = starSystem; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public StarSystem.EnergyType getEnergyType() { return energyType; }
    public void setEnergyType(StarSystem.EnergyType energyType) { this.energyType = energyType; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public LocalDateTime getScheduledStart() { return scheduledStart; }
    public void setScheduledStart(LocalDateTime scheduledStart) { this.scheduledStart = scheduledStart; }
    public LocalDateTime getScheduledEnd() { return scheduledEnd; }
    public void setScheduledEnd(LocalDateTime scheduledEnd) { this.scheduledEnd = scheduledEnd; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getEncryptionKeyVersion() { return encryptionKeyVersion; }
    public void setEncryptionKeyVersion(Integer encryptionKeyVersion) { this.encryptionKeyVersion = encryptionKeyVersion; }
}

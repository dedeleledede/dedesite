package com.zavan.dedesite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "star_systems")
public class StarSystem {
    public enum Status {
        ACTIVE, PAUSED, ARCHIVED, COMPLETED
    }

    public enum Priority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum EnergyType {
        DEEP_FOCUS, LIGHT_ADMIN, CREATIVE, STUDY, CODING, WRITING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private java.util.UUID publicId = java.util.UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    @Column(name = "encrypted_name", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String name;

    @Column(name = "encrypted_description", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.MEDIUM;

    private Double estimatedWeeklyHours;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private EnergyType energyType = EnergyType.DEEP_FOCUS;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private Integer encryptionKeyVersion = 1;

    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public java.util.UUID getPublicId() { return publicId; }
    public void setPublicId(java.util.UUID publicId) { this.publicId = publicId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public Double getEstimatedWeeklyHours() { return estimatedWeeklyHours; }
    public void setEstimatedWeeklyHours(Double estimatedWeeklyHours) { this.estimatedWeeklyHours = estimatedWeeklyHours; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public EnergyType getEnergyType() { return energyType; }
    public void setEnergyType(EnergyType energyType) { this.energyType = energyType; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getEncryptionKeyVersion() { return encryptionKeyVersion; }
    public void setEncryptionKeyVersion(Integer encryptionKeyVersion) { this.encryptionKeyVersion = encryptionKeyVersion; }
}

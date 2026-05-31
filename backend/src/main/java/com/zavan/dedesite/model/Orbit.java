package com.zavan.dedesite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "orbits")
public class Orbit {
    public enum Category {
        WORK, UNIVERSITY, FOOD, SLEEP, COMMUTE, HEALTH, CHORES, OTHER
    }

    public enum Kind {
        LOCKED, PULSAR, REST, MAINTENANCE, ECLIPSE
    }

    public enum Flexibility {
        FIXED, FLEXIBLE
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
    @Column(name = "encrypted_title", nullable = false)
    @Convert(converter = EncryptedStringConverter.class)
    private String title;

    @Column(name = "encrypted_description", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String description;

    @Enumerated(EnumType.STRING)
    private Kind kind = Kind.LOCKED;

    @Enumerated(EnumType.STRING)
    private Flexibility flexibility = Flexibility.FIXED;

    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek = DayOfWeek.MONDAY;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private Category category = Category.OTHER;

    private Integer targetMinutesPerWeek;
    private Integer minimumSessionMinutes;
    private Integer maximumSessionMinutes;

    @Enumerated(EnumType.STRING)
    private StarSystem.EnergyType energyType = StarSystem.EnergyType.LIGHT_ADMIN;

    @Enumerated(EnumType.STRING)
    private Star.Priority priority = Star.Priority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "star_system_id")
    private StarSystem starSystem;

    private String colorKey = "violet";
    private boolean active = true;
    private boolean autoSchedule;
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
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Kind getKind() { return kind == null ? Kind.LOCKED : kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public Flexibility getFlexibility() { return flexibility == null ? Flexibility.FIXED : flexibility; }
    public void setFlexibility(Flexibility flexibility) { this.flexibility = flexibility; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(DayOfWeek dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public Integer getTargetMinutesPerWeek() { return targetMinutesPerWeek; }
    public void setTargetMinutesPerWeek(Integer targetMinutesPerWeek) { this.targetMinutesPerWeek = targetMinutesPerWeek; }
    public Integer getMinimumSessionMinutes() { return minimumSessionMinutes; }
    public void setMinimumSessionMinutes(Integer minimumSessionMinutes) { this.minimumSessionMinutes = minimumSessionMinutes; }
    public Integer getMaximumSessionMinutes() { return maximumSessionMinutes; }
    public void setMaximumSessionMinutes(Integer maximumSessionMinutes) { this.maximumSessionMinutes = maximumSessionMinutes; }
    public StarSystem.EnergyType getEnergyType() { return energyType; }
    public void setEnergyType(StarSystem.EnergyType energyType) { this.energyType = energyType; }
    public Star.Priority getPriority() { return priority; }
    public void setPriority(Star.Priority priority) { this.priority = priority; }
    public StarSystem getStarSystem() { return starSystem; }
    public void setStarSystem(StarSystem starSystem) { this.starSystem = starSystem; }
    public String getColorKey() { return colorKey; }
    public void setColorKey(String colorKey) { this.colorKey = colorKey; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public boolean isAutoSchedule() { return autoSchedule; }
    public void setAutoSchedule(boolean autoSchedule) { this.autoSchedule = autoSchedule; }
    public boolean isPulsar() { return getKind() == Kind.PULSAR; }
    public boolean isFixedBlock() { return getKind() != Kind.PULSAR && getFlexibility() == Flexibility.FIXED; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Integer getEncryptionKeyVersion() { return encryptionKeyVersion; }
    public void setEncryptionKeyVersion(Integer encryptionKeyVersion) { this.encryptionKeyVersion = encryptionKeyVersion; }
}

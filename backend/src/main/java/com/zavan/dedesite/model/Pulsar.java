package com.zavan.dedesite.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Entity
public class Pulsar {
    public enum Frequency {
        DAILY, WEEKLY, TWICE_A_WEEK, THREE_TIMES_A_WEEK, WEEKENDS, CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotBlank
    private String title;

    private String subject;

    @Enumerated(EnumType.STRING)
    private Frequency frequency = Frequency.WEEKLY;

    private Integer targetMinutesPerWeek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_exam_id")
    private Comet relatedExam;

    private boolean active = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public Integer getTargetMinutesPerWeek() { return targetMinutesPerWeek; }
    public void setTargetMinutesPerWeek(Integer targetMinutesPerWeek) { this.targetMinutesPerWeek = targetMinutesPerWeek; }
    public Comet getRelatedExam() { return relatedExam; }
    public void setRelatedExam(Comet relatedExam) { this.relatedExam = relatedExam; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}

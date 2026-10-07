package com.example.demo.entity;

import java.time.LocalTime;

import jakarta.persistence.*;

@Entity
@Table(name = "doctor_working_hours",
        uniqueConstraints = @UniqueConstraint(name = "uk_doctor_working_hours", columnNames = {"doctor_user_id", "day_of_week"}))
public class DoctorWorkingHours {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_user_id", nullable = false)
    private LoginUser doctor;

    @Column(name = "day_of_week", nullable = false)
    private Integer dayOfWeek;

    @Column(nullable = false)
    private boolean working;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    public Long getId() { return id; }
    public LoginUser getDoctor() { return doctor; }
    public void setDoctor(LoginUser doctor) { this.doctor = doctor; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public boolean isWorking() { return working; }
    public void setWorking(boolean working) { this.working = working; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}

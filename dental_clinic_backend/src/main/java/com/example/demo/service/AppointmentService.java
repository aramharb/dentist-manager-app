package com.example.demo.service;

import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.entity.AppointmentStatus;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentService {
    List<AppointmentResponse> search(String query, LocalDate date, AppointmentStatus status, Long providerUserId);
    AppointmentResponse findById(Long id);
    AppointmentResponse findByIdForUpdate(Long id);
    AppointmentResponse create(AppointmentRequest request);
    AppointmentResponse update(Long id, AppointmentRequest request);
    AppointmentResponse cancel(Long id);
    void delete(Long id);
    void deleteCancelled(Long id);
    AppointmentResponse restore(AppointmentResponse snapshot);
}

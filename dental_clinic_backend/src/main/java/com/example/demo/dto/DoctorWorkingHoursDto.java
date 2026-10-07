package com.example.demo.dto;

import java.time.LocalTime;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class DoctorWorkingHoursDto {
    public record Day(
            @NotNull @Min(1) @Max(7) Integer dayOfWeek,
            boolean working,
            LocalTime startTime,
            LocalTime endTime) {}

    public record UpdateRequest(
            @NotNull @Size(min = 7, max = 7) List<@Valid Day> days) {}

    public record Response(
            Long doctorUserId,
            String doctorName,
            List<Day> days) {}
}

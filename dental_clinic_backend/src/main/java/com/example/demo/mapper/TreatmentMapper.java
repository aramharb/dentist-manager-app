package com.example.demo.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.demo.dto.*;
import com.example.demo.entity.*;

@Component
public class TreatmentMapper {
    public TreatmentDto.Response toTreatment(Treatment t, List<TreatmentProcedure> procedures) {
        BigDecimal remaining = t.getEstimatedBill().subtract(t.getPaidAmount());
        List<TreatmentProcedure> active = procedures.stream()
                .filter(p -> p.getStatus() != ProcedureStatus.CANCELLED).toList();
        long completedCount = active.stream().filter(p -> p.getStatus() == ProcedureStatus.COMPLETED).count();
        BigDecimal plannedValue = active.stream().map(TreatmentProcedure::getCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal completedValue = active.stream().filter(p -> p.getStatus() == ProcedureStatus.COMPLETED)
                .map(TreatmentProcedure::getCost).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new TreatmentDto.Response(
                t.getId(), t.getPatient().getId(),
                t.getDoctor() == null ? null : t.getDoctor().getId(),
                t.getDoctor() == null ? null : t.getDoctor().getFullName(),
                t.getTreatmentType() == null ? null : t.getTreatmentType().getId(),
                t.getTreatmentType() == null ? null : t.getTreatmentType().getName(),
                t.getObjective(), t.getStatus(), t.getPriority(), t.getProgressPercent(),
                t.getEstimatedDurationMinutes(), t.getEstimatedBill(), t.getPaidAmount(),
                remaining.max(BigDecimal.ZERO), t.getUpcomingAppointment(), t.getLastVisit(),
                t.getDoctorNotes(),
                procedures.stream().map(TreatmentProcedure::getToothNumber).filter(v -> v != null).distinct().sorted().toList(),
                procedures.stream().anyMatch(p -> Boolean.TRUE.equals(p.getAllTeeth())),
                procedures.size(), Math.toIntExact(completedCount), active.size() - Math.toIntExact(completedCount),
                plannedValue, completedValue, plannedValue.subtract(completedValue).max(BigDecimal.ZERO),
                t.getCreatedAt(), t.getUpdatedAt());
    }

    public ProcedureDto.Response toProcedure(TreatmentProcedure p) {
        return new ProcedureDto.Response(p.getId(), p.getTreatment().getId(),
                p.getProcedureCatalog() == null ? null : p.getProcedureCatalog().getId(),
                p.getToothNumber(), p.getAllTeeth(), p.getToothDescription(), p.getName(), p.getStatus(), p.getPractitioner(), p.getCost(),
                p.getDurationMinutes(), p.getStartedAt(), p.getCompletedAt(),
                p.getCompletedBy() == null ? null : p.getCompletedBy().getId(),
                p.getCompletedBy() == null ? null : p.getCompletedBy().getFullName(),
                p.getNotes(), p.getCreatedAt(), p.getUpdatedAt());
    }

    public HistoryDto.Response toHistory(TreatmentHistory h) {
        return new HistoryDto.Response(h.getId(), h.getTreatment().getId(), h.getEventType(), h.getTitle(), h.getDescription(), h.getEventAt(), h.getCreatedBy());
    }

    public PhotoDto.Response toPhoto(TreatmentPhoto p) {
        return new PhotoDto.Response(p.getId(), p.getTreatment().getId(), p.getPhotoType(), p.getFileName(), p.getContentType(), p.getUrl(), p.getDescription(), p.getUploadedBy(), p.getUploadedAt());
    }

    public PrescriptionDto.Response toPrescription(Prescription p) {
        return new PrescriptionDto.Response(p.getId(), p.getTreatment().getId(), p.getMedicineName(), p.getDosage(), p.getDuration(), p.getInstructions(), p.getIssuedAt(), p.getPdfUrl());
    }

    public CatalogDto.ProcedureCatalogResponse toCatalog(ProcedureCatalog c) {
        return new CatalogDto.ProcedureCatalogResponse(c.getId(), c.getCode(), c.getName(), c.getCategory(),
                c.getDefaultCost(), c.getDefaultDurationMinutes(), c.getDescription(), c.getActive(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}

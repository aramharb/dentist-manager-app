package com.example.demo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ExpenseDto;
import com.example.demo.dto.MaterialDto;
import com.example.demo.entity.ExpenseCategory;
import com.example.demo.entity.ExpenseStatus;
import com.example.demo.entity.Material;
import com.example.demo.entity.MaterialStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.MaterialRepository;
import com.example.demo.security.ClinicPrincipal;

@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final ExpenseService expenseService;

    public MaterialService(MaterialRepository materialRepository, ExpenseService expenseService) {
        this.materialRepository = materialRepository;
        this.expenseService = expenseService;
    }

    @Transactional(readOnly = true)
    public List<MaterialDto.Response> findAll() {
        return materialRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public MaterialDto.Response create(MaterialDto.Request request, ClinicPrincipal actor) {
        Material material = new Material();
        apply(material, request);
        material = materialRepository.save(material);
        linkPurchaseExpense(material, actor);
        return toResponse(materialRepository.save(material));
    }

    @Transactional
    public MaterialDto.Response update(Long id, MaterialDto.Request request, ClinicPrincipal actor) {
        Material material = materialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Material", id));
        apply(material, request);
        if (material.getPurchaseExpenseId() == null) linkPurchaseExpense(material, actor);
        return toResponse(materialRepository.save(material));
    }

    private void apply(Material material, MaterialDto.Request request) {
        material.setName(request.name().trim());
        material.setCategory(defaultText(request.category(), "Uncategorized"));
        material.setQuantity(request.quantity());
        material.setUnit(defaultText(request.unit(), "units"));
        material.setMinimumStock(request.minimumStock());
        material.setExpirationDate(request.expirationDate());
        material.setSupplier(trim(request.supplier()));
        material.setStatus(request.status() == null ? inferredStatus(request) : request.status());
        material.setBatchNumber(trim(request.batchNumber()));
        material.setMonthlyConsumption(request.monthlyConsumption() == null ? 0 : request.monthlyConsumption());
        material.setPurchaseCost(request.purchaseCost());
    }

    private String purchaseDescription(Material material) {
        return material.getQuantity() + " " + material.getUnit() + " of " + material.getName()
                + " (" + material.getCategory() + ")";
    }

    private void linkPurchaseExpense(Material material, ClinicPrincipal actor) {
        ExpenseDto.Response expense = expenseService.create(new ExpenseDto.Request(
                LocalDate.now(), LocalDate.now(), null, ExpenseCategory.DENTAL_MATERIALS,
                "Material purchase: " + material.getName(), purchaseDescription(material),
                material.getSupplier(), material.getBatchNumber(), material.getPurchaseCost(),
                ExpenseStatus.PENDING, "Inventory",
                actor.hasRole("doctor") ? "DOCTOR" : "SECRETARY", actor.getName(), false, null,
                null, true, false, null, 1));
        material.setPurchaseExpenseId(expense.id());
    }

    private MaterialDto.Response toResponse(Material material) {
        return new MaterialDto.Response(material.getId(), material.getName(), material.getCategory(),
                material.getQuantity(), material.getUnit(), material.getMinimumStock(), material.getExpirationDate(),
                material.getSupplier(), material.getStatus(), material.getBatchNumber(),
                material.getMonthlyConsumption(), material.getPurchaseCost(), material.getPurchaseExpenseId(),
                material.getCreatedAt(), material.getUpdatedAt());
    }

    private String trim(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String defaultText(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }

    private MaterialStatus inferredStatus(MaterialDto.Request request) {
        if (request.quantity() == 0) return MaterialStatus.OUT_OF_STOCK;
        if (request.quantity() <= request.minimumStock()) return MaterialStatus.LOW_STOCK;
        return MaterialStatus.AVAILABLE;
    }
}

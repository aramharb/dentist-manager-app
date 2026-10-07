package com.example.demo.service.impl;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.DoctorInsightDto;
import com.example.demo.dto.ExpenseDto;
import com.example.demo.entity.*;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.mapper.ExpenseMapper;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.repository.TreatmentRepository;
import com.example.demo.service.ExpenseService;

@Service
public class ExpenseServiceImpl implements ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final TreatmentRepository treatmentRepository;
    private final ExpenseMapper mapper;

    public ExpenseServiceImpl(ExpenseRepository expenseRepository, TreatmentRepository treatmentRepository, ExpenseMapper mapper) {
        this.expenseRepository = expenseRepository;
        this.treatmentRepository = treatmentRepository;
        this.mapper = mapper;
    }

    @Override @Transactional(readOnly = true)
    public List<ExpenseDto.Response> search(String query, ExpenseStatus status) {
        return expenseRepository.search(blankToNull(query), status).stream().map(mapper::toResponse).toList();
    }

    @Override @Transactional(readOnly = true)
    public ExpenseDto.Response findById(Long id) {
        return mapper.toResponse(expenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id)));
    }

    @Override @Transactional
    public ExpenseDto.Response findByIdForUpdate(Long id) {
        return mapper.toResponse(expenseRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Expense", id)));
    }

    @Override @Transactional(readOnly = true)
    public boolean exists(Long id) {
        return expenseRepository.existsById(id);
    }

    @Override @Transactional
    public ExpenseDto.Response create(ExpenseDto.Request request) {
        Expense expense = new Expense();
        apply(expense, request);
        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Override @Transactional
    public ExpenseDto.Response update(Long id, ExpenseDto.Request request) {
        Expense expense = expenseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id));
        apply(expense, request);
        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Override @Transactional
    public void delete(Long id) {
        expenseRepository.delete(expenseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Expense", id)));
    }

    @Override @Transactional
    public ExpenseDto.Response restore(ExpenseDto.Response snapshot) {
        Expense expense = expenseRepository.findById(snapshot.id())
                .orElseThrow(() -> new ResourceNotFoundException("Expense", snapshot.id()));
        applySnapshot(expense, snapshot);
        return mapper.toResponse(expenseRepository.save(expense));
    }

    @Override @Transactional
    public ExpenseDto.Response restoreDeleted(ExpenseDto.Response snapshot) {
        expenseRepository.restoreDeleted(snapshot.id(), snapshot.recordedAt(), snapshot.updatedAt(),
                snapshot.expenseDate(), snapshot.dueDate(), snapshot.paidAt(), snapshot.category().name(),
                snapshot.billingPeriodMonths(), snapshot.amount(), snapshot.status().name(), snapshot.label(),
                snapshot.description(), snapshot.supplier(), snapshot.invoiceNumber(), snapshot.owner(),
                snapshot.sourceRole(), snapshot.enteredBy(), snapshot.unexpected(), snapshot.unexpectedNote(),
                snapshot.paymentMethod(), snapshot.taxDeductible(), snapshot.recurring(), snapshot.attachmentUrl());
        return findById(snapshot.id());
    }

    @Override @Transactional(readOnly = true)
    public DoctorInsightDto.FinancialInsight doctorFinancialInsights() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime end = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);
        List<Expense> expenses = expenseRepository.search(null, null);
        BigDecimal grossRevenue = treatmentRepository.findAll().stream().map(Treatment::getEstimatedBill).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal collectedRevenue = treatmentRepository.findAll().stream().map(Treatment::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal monthlyExpenses = expenseRepository.sumBetween(start, end);
        BigDecimal pendingExpenses = expenseRepository.sumByStatusBetween(ExpenseStatus.PENDING, start, end);
        BigDecimal netIncome = collectedRevenue.subtract(monthlyExpenses);
        long unexpectedCount = expenseRepository.countUnexpectedMaintenanceBetween(start, end);
        List<DoctorInsightDto.CategoryTotal> byCategory = expenses.stream()
                .filter(e -> !e.getRecordedAt().isBefore(start) && !e.getRecordedAt().isAfter(end))
                .collect(Collectors.groupingBy(e -> e.getCategory().name(), Collectors.reducing(BigDecimal.ZERO, Expense::getAmount, BigDecimal::add)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .map(e -> new DoctorInsightDto.CategoryTotal(e.getKey(), e.getValue()))
                .toList();
        return new DoctorInsightDto.FinancialInsight(grossRevenue, collectedRevenue, monthlyExpenses, pendingExpenses,
                netIncome, expenses.size(), unexpectedCount,
                List.of(
                        new DoctorInsightDto.StatCard("Revenu brut", "$" + shortMoney(grossRevenue), "Traitements planifies"),
                        new DoctorInsightDto.StatCard("Encaisse", "$" + shortMoney(collectedRevenue), "Paiements recus"),
                        new DoctorInsightDto.StatCard("Depenses", "$" + shortMoney(monthlyExpenses), "Mois courant"),
                        new DoctorInsightDto.StatCard("Revenu net", "$" + shortMoney(netIncome), "Encaisse moins depenses"),
                        new DoctorInsightDto.StatCard("A payer", "$" + shortMoney(pendingExpenses), "En attente"),
                        new DoctorInsightDto.StatCard("Imprevus", String.valueOf(unexpectedCount), "Depenses exceptionnelles")),
                byCategory);
    }

    private void apply(Expense e, ExpenseDto.Request r) {
        e.setExpenseDate(r.expenseDate());
        e.setDueDate(r.dueDate());
        e.setPaidAt(r.status() == ExpenseStatus.PAID
                ? (r.paidAt() == null ? LocalDateTime.now() : r.paidAt())
                : null);
        e.setCategory(r.category());
        e.setLabel(trim(r.label()));
        e.setDescription(trim(r.description()));
        e.setSupplier(trim(r.supplier()));
        e.setInvoiceNumber(trim(r.invoiceNumber()));
        e.setBillingPeriodMonths(r.billingPeriodMonths() == null ? 1 : r.billingPeriodMonths());
        e.setAmount(r.amount());
        e.setStatus(r.status() == null ? ExpenseStatus.PENDING : r.status());
        e.setOwner(trim(r.owner()));
        e.setSourceRole(r.sourceRole() == null || r.sourceRole().isBlank() ? "SECRETARY" : r.sourceRole().trim());
        e.setEnteredBy(trim(r.enteredBy()));
        e.setUnexpected(Boolean.TRUE.equals(r.unexpected()));
        e.setUnexpectedNote(trim(r.unexpectedNote()));
        e.setPaymentMethod(trim(r.paymentMethod()));
        e.setTaxDeductible(Boolean.TRUE.equals(r.taxDeductible()));
        e.setRecurring(Boolean.TRUE.equals(r.recurring()));
        e.setAttachmentUrl(trim(r.attachmentUrl()));
    }

    private void applySnapshot(Expense expense, ExpenseDto.Response snapshot) {
        expense.setRecordedAt(snapshot.recordedAt());
        expense.setUpdatedAt(snapshot.updatedAt());
        expense.setExpenseDate(snapshot.expenseDate());
        expense.setDueDate(snapshot.dueDate());
        expense.setPaidAt(snapshot.paidAt());
        expense.setCategory(snapshot.category());
        expense.setLabel(snapshot.label());
        expense.setDescription(snapshot.description());
        expense.setSupplier(snapshot.supplier());
        expense.setInvoiceNumber(snapshot.invoiceNumber());
        expense.setBillingPeriodMonths(snapshot.billingPeriodMonths());
        expense.setAmount(snapshot.amount());
        expense.setStatus(snapshot.status());
        expense.setOwner(snapshot.owner());
        expense.setSourceRole(snapshot.sourceRole());
        expense.setEnteredBy(snapshot.enteredBy());
        expense.setUnexpected(snapshot.unexpected());
        expense.setUnexpectedNote(snapshot.unexpectedNote());
        expense.setPaymentMethod(snapshot.paymentMethod());
        expense.setTaxDeductible(snapshot.taxDeductible());
        expense.setRecurring(snapshot.recurring());
        expense.setAttachmentUrl(snapshot.attachmentUrl());
    }

    private String shortMoney(BigDecimal value) {
        double amount = value.doubleValue();
        return amount >= 1000 ? new DecimalFormat("0.#k").format(amount / 1000) : new DecimalFormat("0").format(amount);
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

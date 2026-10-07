package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;

@Entity
@Table(name = "expense")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 40)
private ExpenseCategory category;

    @Column(name = "billing_period_months", nullable = false)
    private Integer billingPeriodMonths = 1;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExpenseStatus status = ExpenseStatus.PENDING;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String supplier;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column(length = 150)
    private String owner;

    @Column(name = "source_role", nullable = false, length = 20)
    private String sourceRole = "SECRETARY";

    @Column(name = "entered_by", length = 150)
    private String enteredBy;

    @Column(nullable = false)
    private boolean unexpected;

    @Column(name = "unexpected_note", length = 2000)
    private String unexpectedNote;

    @Column(name = "payment_method", length = 100)
    private String paymentMethod;

    @Column(name = "tax_deductible", nullable = false)
    private boolean taxDeductible;

    @Column(nullable = false)
    private boolean recurring;

    @Column(name = "attachment_url", length = 1000)
    private String attachmentUrl;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (recordedAt == null) recordedAt = now;
        if (updatedAt == null) updatedAt = now;
        if (expenseDate == null) expenseDate = recordedAt.toLocalDate();
        if (billingPeriodMonths == null) billingPeriodMonths = 1;
        if (label == null || label.isBlank()) label = category == null ? "Expense" : category.name();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public void setExpenseDate(LocalDate expenseDate) { this.expenseDate = expenseDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public ExpenseCategory getCategory() {
        return category;
    }

    public void setCategory(ExpenseCategory category) {
        this.category = category;
    }

    public Integer getBillingPeriodMonths() {
        return billingPeriodMonths;
    }

    public void setBillingPeriodMonths(Integer billingPeriodMonths) {
        this.billingPeriodMonths = billingPeriodMonths;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public ExpenseStatus getStatus() {
        return status;
    }

    public void setStatus(ExpenseStatus status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getSupplier() { return supplier; }
    public void setSupplier(String supplier) { this.supplier = supplier; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getSourceRole() { return sourceRole; }
    public void setSourceRole(String sourceRole) { this.sourceRole = sourceRole; }
    public String getEnteredBy() { return enteredBy; }
    public void setEnteredBy(String enteredBy) { this.enteredBy = enteredBy; }
    public boolean isUnexpected() { return unexpected; }
    public void setUnexpected(boolean unexpected) { this.unexpected = unexpected; }
    public String getUnexpectedNote() { return unexpectedNote; }
    public void setUnexpectedNote(String unexpectedNote) { this.unexpectedNote = unexpectedNote; }
    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
    public boolean isTaxDeductible() { return taxDeductible; }
    public void setTaxDeductible(boolean taxDeductible) { this.taxDeductible = taxDeductible; }
    public boolean isRecurring() { return recurring; }
    public void setRecurring(boolean recurring) { this.recurring = recurring; }
    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }
}

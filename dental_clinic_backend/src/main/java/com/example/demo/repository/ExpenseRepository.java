package com.example.demo.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import com.example.demo.entity.Expense;
import com.example.demo.entity.ExpenseStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Expense e where e.id = :id")
    Optional<Expense> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query(value = """
            INSERT INTO expense (
                id, recorded_at, updated_at, expense_date, due_date, paid_at, category,
                billing_period_months, amount, status, label, description, supplier,
                invoice_number, owner, source_role, entered_by, unexpected, unexpected_note,
                payment_method, tax_deductible, recurring, attachment_url
            ) VALUES (
                :id, :recordedAt, :updatedAt, :expenseDate, :dueDate, :paidAt, :category,
                :billingPeriodMonths, :amount, :status, :label, :description, :supplier,
                :invoiceNumber, :owner, :sourceRole, :enteredBy, :unexpected, :unexpectedNote,
                :paymentMethod, :taxDeductible, :recurring, :attachmentUrl
            )
            """, nativeQuery = true)
    void restoreDeleted(
            @Param("id") Long id,
            @Param("recordedAt") LocalDateTime recordedAt,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("expenseDate") LocalDate expenseDate,
            @Param("dueDate") LocalDate dueDate,
            @Param("paidAt") LocalDateTime paidAt,
            @Param("category") String category,
            @Param("billingPeriodMonths") Integer billingPeriodMonths,
            @Param("amount") BigDecimal amount,
            @Param("status") String status,
            @Param("label") String label,
            @Param("description") String description,
            @Param("supplier") String supplier,
            @Param("invoiceNumber") String invoiceNumber,
            @Param("owner") String owner,
            @Param("sourceRole") String sourceRole,
            @Param("enteredBy") String enteredBy,
            @Param("unexpected") boolean unexpected,
            @Param("unexpectedNote") String unexpectedNote,
            @Param("paymentMethod") String paymentMethod,
            @Param("taxDeductible") boolean taxDeductible,
            @Param("recurring") boolean recurring,
            @Param("attachmentUrl") String attachmentUrl);

    // Get all expenses, newest first
    List<Expense> findAllByOrderByRecordedAtDesc();

    // Get expenses by status
    List<Expense> findByStatusOrderByRecordedAtDesc(ExpenseStatus status);

    // Search by category or description
    @Query("""
            SELECT e FROM Expense e
            WHERE (
                :query IS NULL
                OR LOWER(CONCAT(
                    e.category,
                    ' ',
                    COALESCE(e.description, '')
                )) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            AND (:status IS NULL OR e.status = :status)
            ORDER BY e.recordedAt DESC
            """)
    List<Expense> search(
            @Param("query") String query,
            @Param("status") ExpenseStatus status
    );

    // Total expenses between two dates
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.recordedAt BETWEEN :start AND :end
            """)
    BigDecimal sumBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Total expenses by status between two dates
    @Query("""
            SELECT COALESCE(SUM(e.amount), 0)
            FROM Expense e
            WHERE e.status = :status
            AND e.recordedAt BETWEEN :start AND :end
            """)
    BigDecimal sumByStatusBetween(
            @Param("status") ExpenseStatus status,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Count unexpected maintenance expenses
    @Query("""
            SELECT COUNT(e)
            FROM Expense e
            WHERE e.category = 'UNEXPECTED_MAINTENANCE'
            AND e.recordedAt BETWEEN :start AND :end
            """)
    long countUnexpectedMaintenanceBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

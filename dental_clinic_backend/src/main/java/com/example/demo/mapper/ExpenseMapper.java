package com.example.demo.mapper;

import org.springframework.stereotype.Component;

import com.example.demo.dto.ExpenseDto;
import com.example.demo.entity.Expense;

@Component
public class ExpenseMapper {

    public ExpenseDto.Response toResponse(Expense e) {
        return new ExpenseDto.Response(
                e.getId(),
                e.getRecordedAt(),
                e.getExpenseDate(),
                e.getDueDate(),
                e.getPaidAt(),
                e.getCategory(),
                e.getLabel(),
                e.getDescription(),
                e.getSupplier(),
                e.getInvoiceNumber(),
                e.getBillingPeriodMonths(),
                e.getAmount(),
                e.getStatus(),
                e.getOwner(),
                e.getSourceRole(),
                e.getEnteredBy(),
                e.isUnexpected(),
                e.getUnexpectedNote(),
                e.getPaymentMethod(),
                e.isTaxDeductible(),
                e.isRecurring(),
                e.getAttachmentUrl(),
                e.getRecordedAt(),
                e.getUpdatedAt()
        );
    }
}

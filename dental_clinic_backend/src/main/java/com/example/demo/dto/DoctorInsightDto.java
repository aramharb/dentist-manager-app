package com.example.demo.dto;

import java.math.BigDecimal;
import java.util.List;

public class DoctorInsightDto {
    public record StatCard(String label, String value, String hint) {}
    public record CategoryTotal(String category, BigDecimal amount) {}
    public record FinancialInsight(
            BigDecimal grossRevenue,
            BigDecimal collectedRevenue,
            BigDecimal monthlyExpenses,
            BigDecimal pendingExpenses,
            BigDecimal netIncome,
            long expenseCount,
            long unexpectedExpenseCount,
            List<StatCard> stats,
            List<CategoryTotal> expensesByCategory) {}
}

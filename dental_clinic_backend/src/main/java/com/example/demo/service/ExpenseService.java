package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.DoctorInsightDto;
import com.example.demo.dto.ExpenseDto;
import com.example.demo.entity.ExpenseStatus;

public interface ExpenseService {
    List<ExpenseDto.Response> search(String query, ExpenseStatus status);
    ExpenseDto.Response findById(Long id);
    ExpenseDto.Response findByIdForUpdate(Long id);
    boolean exists(Long id);
    ExpenseDto.Response create(ExpenseDto.Request request);
    ExpenseDto.Response update(Long id, ExpenseDto.Request request);
    void delete(Long id);
    ExpenseDto.Response restore(ExpenseDto.Response snapshot);
    ExpenseDto.Response restoreDeleted(ExpenseDto.Response snapshot);
    DoctorInsightDto.FinancialInsight doctorFinancialInsights();
}

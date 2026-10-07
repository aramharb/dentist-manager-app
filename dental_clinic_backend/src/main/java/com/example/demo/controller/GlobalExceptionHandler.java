package com.example.demo.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.service.DuplicatePatientNumberException;
import com.example.demo.service.PatientNotFoundException;
import com.example.demo.service.MessagingAccessDeniedException;
import com.example.demo.service.BusinessRuleException;
import com.example.demo.exception.ResourceNotFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map((error) -> error.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", messages);
    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(PatientNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Not found", List.of(exception.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTreatmentNotFound(ResourceNotFoundException exception) {
        return build(HttpStatus.NOT_FOUND, "Not found", List.of(exception.getMessage()));
    }

    @ExceptionHandler(DuplicatePatientNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicatePatientNumberException exception) {
        return build(HttpStatus.CONFLICT, "Duplicate patient number", List.of(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException exception) {
        return build(HttpStatus.BAD_REQUEST, "Bad request", List.of(exception.getMessage()));
    }

    @ExceptionHandler(MessagingAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleMessagingAccessDenied(MessagingAccessDeniedException exception) {
        return build(HttpStatus.FORBIDDEN, "Forbidden", List.of(exception.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiErrorResponse> handleBusinessRule(BusinessRuleException exception) {
        return build(HttpStatus.CONFLICT, "Operation not allowed", List.of(exception.getMessage()));
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String error, List<String> messages) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), error, messages));
    }
}

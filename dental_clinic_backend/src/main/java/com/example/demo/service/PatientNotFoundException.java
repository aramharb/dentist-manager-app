package com.example.demo.service;

public class PatientNotFoundException extends RuntimeException {
    public PatientNotFoundException(Long id) {
        super("Patient with id " + id + " was not found.");
    }
}

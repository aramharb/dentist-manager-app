package com.example.demo.service;

public class DuplicatePatientNumberException extends RuntimeException {
    public DuplicatePatientNumberException(String patientNumber) {
        super("Patient number " + patientNumber + " already exists.");
    }
}

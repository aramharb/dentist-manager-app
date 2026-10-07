package com.example.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.demo.entity.TreatmentPriority;
import com.example.demo.entity.TreatmentStatus;

public class PatientResponse {
    private Long id;
    private String patientNumber;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String phoneNumber;
    private String email;
    private String bloodType;
    private String allergies;
    private String currentTreatment;
    private Boolean cnamCovered;
    private String cnamNumber;
    private LocalDate firstVisit;
    private LocalDate lastVisit;
    private LocalDate nextAppointment;
    private Long selectedTreatmentId;
    private BigDecimal expectedAmount;
    private BigDecimal paidAmount;
    private BigDecimal unpaidBalance;
    private Long registrationTreatmentId;
    private Long assignedDoctorUserId;
    private String assignedDoctorName;
    private TreatmentStatus treatmentStatus;
    private TreatmentPriority treatmentPriority;
    private Integer treatmentProgressPercent;
    private String notes;
    private LocalDateTime lastModified;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPatientNumber() {
        return patientNumber;
    }

    public void setPatientNumber(String patientNumber) {
        this.patientNumber = patientNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getCurrentTreatment() {
        return currentTreatment;
    }

    public void setCurrentTreatment(String currentTreatment) {
        this.currentTreatment = currentTreatment;
    }

    public Boolean getCnamCovered() {
        return cnamCovered;
    }

    public void setCnamCovered(Boolean cnamCovered) {
        this.cnamCovered = cnamCovered;
    }

    public String getCnamNumber() {
        return cnamNumber;
    }

    public void setCnamNumber(String cnamNumber) {
        this.cnamNumber = cnamNumber;
    }

    public LocalDate getFirstVisit() {
        return firstVisit;
    }

    public void setFirstVisit(LocalDate firstVisit) {
        this.firstVisit = firstVisit;
    }

    public LocalDate getLastVisit() {
        return lastVisit;
    }

    public void setLastVisit(LocalDate lastVisit) {
        this.lastVisit = lastVisit;
    }

    public LocalDate getNextAppointment() {
        return nextAppointment;
    }

    public void setNextAppointment(LocalDate nextAppointment) {
        this.nextAppointment = nextAppointment;
    }

    public Long getSelectedTreatmentId() {
        return selectedTreatmentId;
    }

    public void setSelectedTreatmentId(Long selectedTreatmentId) {
        this.selectedTreatmentId = selectedTreatmentId;
    }

    public BigDecimal getExpectedAmount() {
        return expectedAmount;
    }

    public void setExpectedAmount(BigDecimal expectedAmount) {
        this.expectedAmount = expectedAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getUnpaidBalance() {
        return unpaidBalance;
    }

    public void setUnpaidBalance(BigDecimal unpaidBalance) {
        this.unpaidBalance = unpaidBalance;
    }

    public Long getRegistrationTreatmentId() {
        return registrationTreatmentId;
    }

    public void setRegistrationTreatmentId(Long registrationTreatmentId) {
        this.registrationTreatmentId = registrationTreatmentId;
    }

    public Long getAssignedDoctorUserId() { return assignedDoctorUserId; }
    public void setAssignedDoctorUserId(Long assignedDoctorUserId) { this.assignedDoctorUserId = assignedDoctorUserId; }
    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public TreatmentStatus getTreatmentStatus() {
        return treatmentStatus;
    }

    public void setTreatmentStatus(TreatmentStatus treatmentStatus) {
        this.treatmentStatus = treatmentStatus;
    }

    public TreatmentPriority getTreatmentPriority() {
        return treatmentPriority;
    }

    public void setTreatmentPriority(TreatmentPriority treatmentPriority) {
        this.treatmentPriority = treatmentPriority;
    }

    public Integer getTreatmentProgressPercent() {
        return treatmentProgressPercent;
    }

    public void setTreatmentProgressPercent(Integer treatmentProgressPercent) {
        this.treatmentProgressPercent = treatmentProgressPercent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}

package com.example.demo.controller;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class PatientRequest {

    @Size(max = 20, message = "Patient number must be 20 characters or fewer.")
    private String patientNumber;

    @NotBlank(message = "First name is required.")
    @Size(max = 100, message = "First name must be 100 characters or fewer.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    @Size(max = 100, message = "Last name must be 100 characters or fewer.")
    private String lastName;

    @NotBlank(message = "Gender is required.")
    @Pattern(regexp = "Male|Female", message = "Gender must be Male or Female.")
    private String gender;

    private LocalDate birthDate;
    private String address;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 20, message = "Phone number must be 20 characters or fewer.")
    private String phoneNumber;

    @Email(message = "Email must be valid.")
    @Size(max = 150, message = "Email must be 150 characters or fewer.")
    private String email;

    @Pattern(regexp = "A\\+|A-|B\\+|B-|AB\\+|AB-|O\\+|O-", message = "Blood type is invalid.")
    private String bloodType;

    private String allergies;
    private String currentTreatment;
    private Boolean cnamCovered;

    @Size(max = 30, message = "CNAM number must be 30 characters or fewer.")
    private String cnamNumber;

    private LocalDate firstVisit;
    private LocalDate nextAppointment;

    private Long selectedTreatmentId;
    private Long assignedDoctorUserId;

    @DecimalMin(value = "0.00", message = "Expected amount cannot be negative.")
    private BigDecimal expectedAmount;

    @DecimalMin(value = "0.00", message = "Paid amount cannot be negative.")
    private BigDecimal paidAmount;

    @DecimalMin(value = "0.00", message = "Unpaid balance cannot be negative.")
    private BigDecimal unpaidBalance;

    private String notes;

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

    public Long getAssignedDoctorUserId() {
        return assignedDoctorUserId;
    }

    public void setAssignedDoctorUserId(Long assignedDoctorUserId) {
        this.assignedDoctorUserId = assignedDoctorUserId;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

package com.example.demo.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "patient")
public class Patient extends CabinetOwned {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_number", nullable = false, unique = true, length = 20)
    private String patientNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "address", columnDefinition = "text")
    private String address;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "blood_type", length = 5)
    private String bloodType;

    @Column(name = "allergies", columnDefinition = "text")
    private String allergies;

    @Column(name = "current_treatment", columnDefinition = "text")
    private String currentTreatment;

    @Column(name = "cnam_covered")
    private Boolean cnamCovered;

    @Column(name = "cnam_number", length = 30)
    private String cnamNumber;

    @Column(name = "first_visit")
    private LocalDate firstVisit;

    @Column(name = "last_visit")
    private LocalDate lastVisit;

    @Column(name = "next_appointment")
    private LocalDate nextAppointment;

    @ManyToOne
    @JoinColumn(name = "selected_treatment_id")
    private ProcedureCatalog selectedTreatment;

    @Column(name = "expected_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal expectedAmount;

    @Column(name = "paid_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @ManyToOne
    @JoinColumn(name = "registration_treatment_id")
    private Treatment registrationTreatment;

    @ManyToOne
    @JoinColumn(name = "assigned_doctor_user_id")
    private LoginUser assignedDoctor;

    @Column(name = "unpaid_balance", precision = 12, scale = 2)
    private BigDecimal unpaidBalance;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    @Column(name = "last_modified", nullable = false)
    private LocalDateTime lastModified;

    @PrePersist
    @PreUpdate
    void updateLastModified() {
        lastModified = LocalDateTime.now();
    }

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

    public ProcedureCatalog getSelectedTreatment() {
        return selectedTreatment;
    }

    public void setSelectedTreatment(ProcedureCatalog selectedTreatment) {
        this.selectedTreatment = selectedTreatment;
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

    public Treatment getRegistrationTreatment() {
        return registrationTreatment;
    }

    public void setRegistrationTreatment(Treatment registrationTreatment) {
        this.registrationTreatment = registrationTreatment;
    }

    public LoginUser getAssignedDoctor() {
        return assignedDoctor;
    }

    public void setAssignedDoctor(LoginUser assignedDoctor) {
        this.assignedDoctor = assignedDoctor;
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

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
}

package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.controller.PatientRequest;
import com.example.demo.controller.PatientResponse;
import com.example.demo.dto.AppointmentRequest;
import com.example.demo.dto.AppointmentResponse;
import com.example.demo.dto.BookingDto;
import com.example.demo.dto.ClientDto;
import com.example.demo.entity.AppointmentPriority;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.security.ClinicPrincipal;
import com.example.demo.security.PhoneNumbers;

/**
 * Secretary side of the client space: confirming a request creates the real appointment and, when
 * needed, links the client to an existing file or creates a new one. Everything is limited to the
 * secretary's own cabinet.
 */
@Service
public class BookingRequestService {
    private final JdbcTemplate jdbc;
    private final PatientService patientService;
    private final AppointmentService appointmentService;
    private final StaffActionService staffActionService;

    public BookingRequestService(JdbcTemplate jdbc, PatientService patientService,
            AppointmentService appointmentService, StaffActionService staffActionService) {
        this.jdbc = jdbc;
        this.patientService = patientService;
        this.appointmentService = appointmentService;
        this.staffActionService = staffActionService;
    }

    @Transactional(readOnly = true)
    public List<BookingDto.Pending> pending(ClinicPrincipal actor) {
        Long cabinetId = actor.requireCabinetId();
        return jdbc.query("""
                select r.id, a.full_name, a.phone, r.doctor_user_id, l.full_name, r.date, r.start_time, r.end_time,
                       r.note, r.created_at, m.status, m.patient_id,
                       m.first_name, m.last_name, m.gender, m.birth_date, m.address, m.email, m.blood_type,
                       m.allergies, m.cnam_covered, m.cnam_number
                from appointment_request r
                join client_membership m on m.id = r.membership_id
                join client_account a on a.id = m.account_id
                join login l on l.id = r.doctor_user_id
                where r.cabinet_id = ? and r.status = 'PENDING'
                order by r.date, r.start_time
                """, (rs, row) -> {
            Long linked = (Long) rs.getObject(12);
            String phone = rs.getString(3);
            return new BookingDto.Pending(rs.getLong(1), rs.getString(2), phone, rs.getLong(4), rs.getString(5),
                    rs.getObject(6, LocalDate.class), rs.getObject(7, LocalTime.class), rs.getObject(8, LocalTime.class),
                    rs.getString(9), rs.getObject(10, LocalDateTime.class), rs.getString(11), linked,
                    new ClientDto.JoinRequest(rs.getString(13), rs.getString(14), rs.getString(15),
                            rs.getObject(16, LocalDate.class), rs.getString(17), rs.getString(18), rs.getString(19),
                            rs.getString(20), rs.getBoolean(21), rs.getString(22)),
                    linked == null ? matches(cabinetId, phone) : List.of());
        }, cabinetId);
    }

    @Transactional(readOnly = true)
    public long pendingCount(ClinicPrincipal actor) {
        Long count = jdbc.queryForObject(
                "select count(*) from appointment_request where cabinet_id = ? and status = 'PENDING'",
                Long.class, actor.requireCabinetId());
        return count == null ? 0 : count;
    }

    @Transactional
    public BookingDto.Confirmed confirm(Long requestId, BookingDto.ConfirmRequest body, ClinicPrincipal actor) {
        Long cabinetId = actor.requireCabinetId();
        var request = jdbc.query("""
                select membership_id, doctor_user_id, date, start_time, end_time, note, status
                from appointment_request where id = ? and cabinet_id = ? for update
                """, (rs, row) -> new Object[] { rs.getLong(1), rs.getLong(2), rs.getObject(3, LocalDate.class),
                rs.getObject(4, LocalTime.class), rs.getObject(5, LocalTime.class), rs.getString(6), rs.getString(7) },
                requestId, cabinetId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Request", requestId));
        if (!"PENDING".equals(request[6])) {
            throw new BusinessRuleException("This request was already handled.");
        }
        Long membershipId = (Long) request[0];
        Long doctorId = (Long) request[1];

        var membership = jdbc.query("""
                select m.status, m.patient_id, a.phone, m.first_name, m.last_name, m.gender, m.birth_date, m.address,
                       m.email, m.blood_type, m.allergies, m.cnam_covered, m.cnam_number
                from client_membership m join client_account a on a.id = m.account_id
                where m.id = ? and m.cabinet_id = ? for update of m
                """, (rs, row) -> new Object[] { rs.getString(1), rs.getObject(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getObject(7, LocalDate.class), rs.getString(8), rs.getString(9),
                rs.getString(10), rs.getString(11), rs.getBoolean(12), rs.getString(13) },
                membershipId, cabinetId).stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Client", membershipId));

        Long patientId;
        boolean created = false;
        if ("LINKED".equals(membership[0]) && membership[1] != null) {
            patientId = ((Number) membership[1]).longValue();
        } else if (body != null && body.patientId() != null) {
            Long exists = jdbc.queryForObject("select count(*) from patient where id = ? and cabinet_id = ?",
                    Long.class, body.patientId(), cabinetId);
            if (exists == null || exists == 0) throw new ResourceNotFoundException("Patient", body.patientId());
            patientId = body.patientId();
        } else {
            PatientRequest newPatient = new PatientRequest();
            newPatient.setFirstName((String) membership[3]);
            newPatient.setLastName((String) membership[4]);
            newPatient.setGender((String) membership[5]);
            newPatient.setBirthDate((LocalDate) membership[6]);
            newPatient.setAddress((String) membership[7]);
            newPatient.setPhoneNumber((String) membership[2]);
            newPatient.setEmail((String) membership[8]);
            newPatient.setBloodType((String) membership[9]);
            newPatient.setAllergies((String) membership[10]);
            newPatient.setCnamCovered((Boolean) membership[11]);
            newPatient.setCnamNumber((String) membership[12]);
            newPatient.setAssignedDoctorUserId(doctorId);
            PatientResponse patient = patientService.create(newPatient, actor);
            staffActionService.recordAction(actor, StaffActionService.PATIENT_CREATED, StaffActionService.PATIENT,
                    patient.getId(), null, null, patient);
            patientId = patient.getId();
            created = true;
        }
        jdbc.update("update client_membership set status = 'LINKED', patient_id = ?, updated_at = now() where id = ?",
                patientId, membershipId);

        // The real appointment goes through the normal rules (working hours, 30-minute spacing...); if the
        // time was taken meanwhile this throws and nothing above is kept.
        AppointmentResponse appointment = appointmentService.create(new AppointmentRequest(patientId, null,
                (LocalDate) request[2], (LocalTime) request[3], (LocalTime) request[4], null, null, doctorId, null,
                AppointmentPriority.NORMAL, AppointmentStatus.CONFIRMED, (String) request[5]));
        staffActionService.recordAction(actor, StaffActionService.APPOINTMENT_CREATED, StaffActionService.APPOINTMENT,
                appointment.id(), null, null, appointment);
        jdbc.update("""
                update appointment_request set status = 'CONFIRMED', appointment_id = ?, decided_at = now(), decided_by = ?
                where id = ?
                """, appointment.id(), actor.userId(), requestId);
        return new BookingDto.Confirmed(requestId, appointment.id(), patientId, created);
    }

    @Transactional
    public void reject(Long requestId, BookingDto.RejectRequest body, ClinicPrincipal actor) {
        String reason = body == null || body.reason() == null || body.reason().isBlank() ? null : body.reason().trim();
        int updated = jdbc.update("""
                update appointment_request set status = 'REJECTED', reject_reason = ?, decided_at = now(), decided_by = ?
                where id = ? and cabinet_id = ? and status = 'PENDING'
                """, reason, actor.userId(), requestId, actor.requireCabinetId());
        if (updated != 1) throw new BusinessRuleException("This request was already handled or does not exist.");
    }

    /** Existing client files of this cabinet whose phone number ends with the same 8 digits. */
    private List<BookingDto.PatientMatch> matches(Long cabinetId, String phone) {
        return jdbc.query("""
                select id, patient_number, first_name, last_name, phone_number, birth_date
                from patient
                where cabinet_id = ?
                  and right(regexp_replace(coalesce(phone_number, ''), '[^0-9]', '', 'g'), 8) = ?
                order by last_name, first_name limit 5
                """, (rs, row) -> new BookingDto.PatientMatch(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getObject(6, LocalDate.class)),
                cabinetId, PhoneNumbers.last8(phone));
    }
}

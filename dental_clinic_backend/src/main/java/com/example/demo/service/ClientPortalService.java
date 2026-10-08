package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.BrandingDto;
import com.example.demo.dto.ClientDto;
import com.example.demo.entity.Cabinet;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CabinetRepository;

/**
 * What a client can do: browse cabinets, send their details to one, see the free times of its doctors
 * and ask for an appointment. Cabinet data is read with explicit {@code cabinet_id} conditions (a client
 * belongs to no single cabinet, so the automatic cabinet filter does not apply), and the agenda is only
 * ever exposed as a list of free start times.
 */
@Service
public class ClientPortalService {
    static final int SLOT_MINUTES = 30;
    /** Same rule as the secretary's agenda: two appointments of a doctor are at least 30 minutes apart. */
    static final int GAP_MINUTES = 30;
    static final int MAX_DAYS_AHEAD = 60;
    static final int MIN_LEAD_MINUTES = 60;
    static final int MAX_PENDING_REQUESTS = 3;

    private final JdbcTemplate jdbc;
    private final CabinetRepository cabinetRepository;
    private final CabinetBrandingService brandingService;

    public ClientPortalService(JdbcTemplate jdbc, CabinetRepository cabinetRepository,
            CabinetBrandingService brandingService) {
        this.jdbc = jdbc;
        this.cabinetRepository = cabinetRepository;
        this.brandingService = brandingService;
    }

    // ------------------------------------------------------------------ cabinets

    @Transactional(readOnly = true)
    public List<BrandingDto.Response> activeCabinets() {
        return cabinetRepository.findAllByOrderByNameAsc().stream()
                .filter(cabinet -> Boolean.TRUE.equals(cabinet.getActive()))
                .map(brandingService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public BrandingDto.Response activeCabinet(Long cabinetId) {
        return brandingService.toResponse(requireActiveCabinet(cabinetId));
    }

    @Transactional(readOnly = true)
    public List<ClientDto.Membership> memberships(Long accountId) {
        List<Long> cabinetIds = jdbc.queryForList(
                "select cabinet_id from client_membership where account_id = ? order by created_at desc",
                Long.class, accountId);
        List<ClientDto.Membership> result = new ArrayList<>();
        for (Long cabinetId : cabinetIds) {
            Cabinet cabinet = cabinetRepository.findById(cabinetId).orElse(null);
            if (cabinet == null || !Boolean.TRUE.equals(cabinet.getActive())) continue;
            result.add(membership(accountId, cabinet));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public ClientDto.Membership membership(Long accountId, Long cabinetId) {
        Cabinet cabinet = requireActiveCabinet(cabinetId);
        ClientDto.Membership membership = membership(accountId, cabinet);
        if (membership == null) throw new ResourceNotFoundException("Membership", cabinetId);
        return membership;
    }

    private ClientDto.Membership membership(Long accountId, Cabinet cabinet) {
        return jdbc.query("""
                select status, first_name, last_name, gender, birth_date, address, email, blood_type, allergies,
                       cnam_covered, cnam_number
                from client_membership where account_id = ? and cabinet_id = ?
                """, (rs, row) -> new ClientDto.Membership(brandingService.toResponse(cabinet), rs.getString(1),
                new ClientDto.JoinRequest(rs.getString(2), rs.getString(3), rs.getString(4),
                        rs.getObject(5, LocalDate.class), rs.getString(6), rs.getString(7), rs.getString(8),
                        rs.getString(9), rs.getBoolean(10), rs.getString(11))),
                accountId, cabinet.getId()).stream().findFirst().orElse(null);
    }

    /** Sends (or updates) the client's details to a cabinet; a secretary links them to a client file later. */
    @Transactional
    public ClientDto.Membership join(Long accountId, Long cabinetId, ClientDto.JoinRequest request) {
        Cabinet cabinet = requireActiveCabinet(cabinetId);
        String status = jdbc.query("select status from client_membership where account_id = ? and cabinet_id = ?",
                (rs, row) -> rs.getString(1), accountId, cabinetId).stream().findFirst().orElse(null);
        if ("LINKED".equals(status)) {
            throw new BusinessRuleException("Your file is already linked at this cabinet. Ask the cabinet to change it.");
        }
        Object[] values = { clean(request.firstName()), clean(request.lastName()), request.gender(),
                request.birthDate(), clean(request.address()), clean(request.email()), request.bloodType(),
                clean(request.allergies()), Boolean.TRUE.equals(request.cnamCovered()), clean(request.cnamNumber()) };
        if (status == null) {
            jdbc.update("""
                    insert into client_membership (account_id, cabinet_id, first_name, last_name, gender, birth_date,
                        address, email, blood_type, allergies, cnam_covered, cnam_number)
                    values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, concat(new Object[] { accountId, cabinetId }, values));
        } else {
            jdbc.update("""
                    update client_membership set first_name = ?, last_name = ?, gender = ?, birth_date = ?, address = ?,
                        email = ?, blood_type = ?, allergies = ?, cnam_covered = ?, cnam_number = ?,
                        status = 'PENDING', updated_at = now()
                    where account_id = ? and cabinet_id = ?
                    """, concat(values, new Object[] { accountId, cabinetId }));
        }
        return membership(accountId, cabinet);
    }

    // ------------------------------------------------------------------ doctors and free times

    @Transactional(readOnly = true)
    public List<ClientDto.Doctor> doctors(Long accountId, Long cabinetId) {
        requireOpenMembership(accountId, cabinetId);
        return jdbc.query("""
                select id, full_name from login
                where cabinet_id = ? and role = 'doctor' and active = true order by full_name
                """, (rs, row) -> new ClientDto.Doctor(rs.getLong(1), rs.getString(2)), cabinetId);
    }

    @Transactional(readOnly = true)
    public List<ClientDto.SlotDay> freeTimes(Long accountId, Long cabinetId, Long doctorId, LocalDate from, int days) {
        requireOpenMembership(accountId, cabinetId);
        requireDoctor(cabinetId, doctorId, false);
        LocalDate start = from == null || from.isBefore(LocalDate.now()) ? LocalDate.now() : from;
        LocalDate last = start.plusDays(Math.max(1, Math.min(days, 14)) - 1L);
        LocalDate limit = LocalDate.now().plusDays(MAX_DAYS_AHEAD);
        if (last.isAfter(limit)) last = limit;
        List<ClientDto.SlotDay> result = new ArrayList<>();
        Map<LocalDate, List<LocalTime>> free = freeTimesByDay(cabinetId, doctorId, start, last);
        for (LocalDate day = start; !day.isAfter(last); day = day.plusDays(1)) {
            List<LocalTime> times = free.getOrDefault(day, List.of());
            if (!times.isEmpty()) result.add(new ClientDto.SlotDay(day, times));
        }
        return result;
    }

    // ------------------------------------------------------------------ requests

    @Transactional
    public ClientDto.MyRequest request(Long accountId, Long cabinetId, ClientDto.BookingBody body) {
        if (body == null || body.doctorId() == null || body.date() == null || body.time() == null) {
            throw new IllegalArgumentException("Choose a doctor, a day and a time.");
        }
        Long membershipId = requireOpenMembership(accountId, cabinetId);
        // Locking the doctor's row serialises concurrent requests (and secretary bookings) for that doctor.
        requireDoctor(cabinetId, body.doctorId(), true);
        if (body.date().isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) {
            throw new BusinessRuleException("You can book up to " + MAX_DAYS_AHEAD + " days ahead.");
        }
        List<LocalTime> free = freeTimesByDay(cabinetId, body.doctorId(), body.date(), body.date())
                .getOrDefault(body.date(), List.of());
        if (!free.contains(body.time())) {
            throw new BusinessRuleException("This time is no longer available. Please choose another one.");
        }
        Long pending = jdbc.queryForObject(
                "select count(*) from appointment_request where membership_id = ? and status = 'PENDING'",
                Long.class, membershipId);
        if (pending != null && pending >= MAX_PENDING_REQUESTS) {
            throw new BusinessRuleException("You already have " + MAX_PENDING_REQUESTS
                    + " requests waiting for confirmation. Wait for the cabinet's answer first.");
        }
        Long id = jdbc.queryForObject("""
                insert into appointment_request (cabinet_id, membership_id, doctor_user_id, date, start_time, end_time, note)
                values (?, ?, ?, ?, ?, ?, ?) returning id
                """, Long.class, cabinetId, membershipId, body.doctorId(), body.date(), body.time(),
                body.time().plusMinutes(SLOT_MINUTES), clean(body.note()));
        return myRequests(accountId).stream().filter(request -> request.id().equals(id)).findFirst().orElseThrow();
    }

    @Transactional(readOnly = true)
    public List<ClientDto.MyRequest> myRequests(Long accountId) {
        return jdbc.query("""
                select r.id, r.cabinet_id, c.name, r.doctor_user_id, l.full_name, r.date, r.start_time, r.end_time,
                       r.status, r.note, r.reject_reason, a.status
                from appointment_request r
                join client_membership m on m.id = r.membership_id
                join cabinet c on c.id = r.cabinet_id
                join login l on l.id = r.doctor_user_id
                left join appointment a on a.id = r.appointment_id
                where m.account_id = ?
                order by r.date desc, r.start_time desc
                limit 100
                """, (rs, row) -> {
            String status = rs.getString(9);
            String appointmentStatus = rs.getString(12);
            boolean droppedByCabinet = "CONFIRMED".equals(status) && (appointmentStatus == null
                    || "CANCELLED".equals(appointmentStatus));
            return new ClientDto.MyRequest(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getLong(4),
                    rs.getString(5), rs.getObject(6, LocalDate.class), rs.getObject(7, LocalTime.class),
                    rs.getObject(8, LocalTime.class), status, droppedByCabinet ? "CANCELLED_BY_CABINET" : status,
                    rs.getString(10), rs.getString(11));
        }, accountId);
    }

    @Transactional
    public void cancel(Long accountId, Long requestId) {
        int updated = jdbc.update("""
                update appointment_request set status = 'CANCELLED', decided_at = now()
                where id = ? and status = 'PENDING'
                  and membership_id in (select id from client_membership where account_id = ?)
                """, requestId, accountId);
        if (updated != 1) {
            throw new BusinessRuleException(
                    "Only a request that is still waiting can be cancelled. Call the cabinet for anything else.");
        }
    }

    // ------------------------------------------------------------------ free time computation

    /**
     * Free start times per day for one doctor: inside the doctor's working hours, not in the past, and not
     * conflicting (30 minutes apart) with an appointment or a pending request. The single source of truth for
     * both the list shown to clients and the check made when a request is sent.
     */
    private Map<LocalDate, List<LocalTime>> freeTimesByDay(Long cabinetId, Long doctorId, LocalDate from, LocalDate to) {
        Map<Integer, LocalTime[]> hours = new HashMap<>();
        jdbc.query("""
                select day_of_week, start_time, end_time from doctor_working_hours
                where doctor_user_id = ? and working = true and start_time is not null and end_time is not null
                """, rs -> {
            hours.put(rs.getInt(1), new LocalTime[] { rs.getObject(2, LocalTime.class), rs.getObject(3, LocalTime.class) });
        }, doctorId);

        Map<LocalDate, List<LocalTime[]>> busy = new HashMap<>();
        String busySql = """
                select date, heure, end_time from appointment
                where cabinet_id = ? and provider_user_id = ? and status <> 'CANCELLED' and date between ? and ?
                union all
                select date, start_time, end_time from appointment_request
                where cabinet_id = ? and doctor_user_id = ? and status = 'PENDING' and date between ? and ?
                """;
        jdbc.query(busySql, rs -> {
            busy.computeIfAbsent(rs.getObject(1, LocalDate.class), ignored -> new ArrayList<>())
                    .add(new LocalTime[] { rs.getObject(2, LocalTime.class), rs.getObject(3, LocalTime.class) });
        }, cabinetId, doctorId, from, to, cabinetId, doctorId, from, to);

        LocalDateTime earliest = LocalDateTime.now().plusMinutes(MIN_LEAD_MINUTES);
        Map<LocalDate, List<LocalTime>> result = new HashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            LocalTime[] window = hours.get(day.getDayOfWeek().getValue());
            if (window == null) continue;
            List<LocalTime> times = new ArrayList<>();
            List<LocalTime[]> dayBusy = busy.getOrDefault(day, List.of());
            for (LocalTime t = window[0]; !t.plusMinutes(SLOT_MINUTES).isAfter(window[1]); t = t.plusMinutes(SLOT_MINUTES)) {
                if (day.atTime(t).isBefore(earliest)) continue;
                LocalTime slotStart = t;
                LocalTime end = t.plusMinutes(SLOT_MINUTES);
                if (dayBusy.stream().noneMatch(other -> other[0].isBefore(end.plusMinutes(GAP_MINUTES))
                        && other[1].isAfter(slotStart.minusMinutes(GAP_MINUTES)))) {
                    times.add(t);
                }
                // A time that would wrap past midnight cannot happen: working hours end the same day.
            }
            if (!times.isEmpty()) result.put(day, times);
        }
        return result;
    }

    // ------------------------------------------------------------------ guards

    private Cabinet requireActiveCabinet(Long cabinetId) {
        return cabinetRepository.findById(cabinetId)
                .filter(cabinet -> Boolean.TRUE.equals(cabinet.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Cabinet", cabinetId));
    }

    /** The client must have sent their details to this cabinet (and not been turned down). */
    private Long requireOpenMembership(Long accountId, Long cabinetId) {
        requireActiveCabinet(cabinetId);
        return jdbc.query("""
                select id from client_membership where account_id = ? and cabinet_id = ? and status <> 'REJECTED'
                """, (rs, row) -> rs.getLong(1), accountId, cabinetId).stream().findFirst()
                .orElseThrow(() -> new BusinessRuleException("Send your details to this cabinet first."));
    }

    private void requireDoctor(Long cabinetId, Long doctorId, boolean lock) {
        String sql = "select id from login where id = ? and cabinet_id = ? and role = 'doctor' and active = true"
                + (lock ? " for update" : "");
        if (jdbc.queryForList(sql, Long.class, doctorId, cabinetId).isEmpty()) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Object[] concat(Object[] first, Object[] second) {
        Object[] all = new Object[first.length + second.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(second, 0, all, first.length, second.length);
        return all;
    }
}

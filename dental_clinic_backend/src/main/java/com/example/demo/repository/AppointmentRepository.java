package com.example.demo.repository;

import com.example.demo.entity.appointment;
import com.example.demo.entity.AppointmentStatus;
import com.example.demo.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<appointment, Long> {

    /**
     * Get all appointments of a patient.
     */
    List<appointment> findByPatient(Patient patient);

    /**
     * Get all appointments by patient id.
     */
    List<appointment> findByPatientId(Long patientId);

    /**
     * Get all appointments for a specific date.
     */
    List<appointment> findByDate(LocalDate date);

    /**
     * Search appointments by patient first name.
     */
    List<appointment> findByPatientFirstNameContainingIgnoreCase(String firstName);

    /**
     * Search appointments by patient last name.
     */
    List<appointment> findByPatientLastNameContainingIgnoreCase(String lastName);

    /**
     * Search appointments by patient's full name.
     */
    List<appointment> findByPatientFirstNameContainingIgnoreCaseOrPatientLastNameContainingIgnoreCase(
            String firstName,
            String lastName
    );

    @Query("""
            select a from appointment a
            join fetch a.patient p
            left join fetch a.treatment t
            where (:date is null or a.date = :date)
              and (:status is null or a.status = :status)
              and (:providerUserId is null or a.providerUser.id = :providerUserId)
            order by a.date asc, a.heure asc
            """)
    List<appointment> search(@Param("date") LocalDate date,
            @Param("status") AppointmentStatus status,
            @Param("providerUserId") Long providerUserId);

    @Query("""
            select a from appointment a
            join fetch a.patient p
            left join fetch a.treatment t
            where (:date is null or a.date = :date)
              and (:status is null or a.status = :status)
              and (:providerUserId is null or a.providerUser.id = :providerUserId)
              and (lower(p.firstName) like lower(concat('%', :query, '%'))
                or lower(p.lastName) like lower(concat('%', :query, '%'))
                or lower(a.providerName) like lower(concat('%', :query, '%')))
            order by a.date asc, a.heure asc
            """)
    List<appointment> searchWithQuery(@Param("query") String query,
            @Param("date") LocalDate date,
            @Param("status") AppointmentStatus status,
            @Param("providerUserId") Long providerUserId);

    @Query("""
            select a from appointment a
            join fetch a.patient p
            left join fetch a.treatment t
            where a.date between :from and :to
              and (:providerUserId is null or a.providerUser.id = :providerUserId)
            order by a.date asc, a.heure asc
            """)
    List<appointment> dashboardAppointments(@Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("providerUserId") Long providerUserId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from appointment a join fetch a.patient left join fetch a.treatment where a.id = :id")
    Optional<appointment> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select (count(a) > 0) from appointment a
            where a.providerUser.id = :providerUserId
              and a.date = :date
              and a.status <> com.example.demo.entity.AppointmentStatus.CANCELLED
              and (:excludedId is null or a.id <> :excludedId)
              and a.heure < :bufferedEnd
              and a.endTime > :bufferedStart
            """)
    boolean existsSchedulingConflict(@Param("providerUserId") Long providerUserId,
            @Param("date") LocalDate date,
            @Param("bufferedStart") LocalTime bufferedStart,
            @Param("bufferedEnd") LocalTime bufferedEnd,
            @Param("excludedId") Long excludedId);

    @Query("""
            select min(a.date) from appointment a
            where a.patient.id = :patientId
              and a.date >= current_date
              and a.status in (
                com.example.demo.entity.AppointmentStatus.SCHEDULED,
                com.example.demo.entity.AppointmentStatus.CONFIRMED,
                com.example.demo.entity.AppointmentStatus.IN_PROGRESS
              )
            """)
    LocalDate findNextDateByPatientId(@Param("patientId") Long patientId);
}

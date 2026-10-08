package com.example.demo.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.dto.AccessLogDto;
import com.example.demo.security.ClinicPrincipal;

/** Journal of who viewed or changed client files and clinical records, kept per cabinet. */
@Service
public class RecordAccessLogService {
    private static final Logger log = LoggerFactory.getLogger(RecordAccessLogService.class);
    private final JdbcTemplate jdbc;

    public RecordAccessLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Never fails the request it is attached to. */
    public void record(ClinicPrincipal actor, String action, String resource, Long resourceId, String address) {
        try {
            if (actor.cabinetId() == null) return;
            jdbc.update("""
                    insert into record_access_log (cabinet_id, user_id, user_name, user_role, action, resource, resource_id, client_address)
                    select ?, l.id, l.full_name, l.role, ?, ?, ?, ? from login l where l.id = ?
                    """, actor.cabinetId(), action, resource, resourceId, address, actor.userId());
        } catch (RuntimeException exception) {
            log.warn("Could not write the access log entry: {}", exception.getMessage());
        }
    }

    public List<AccessLogDto.Entry> recent(Long cabinetId, int limit, int offset) {
        return jdbc.query("""
                select id, user_name, user_role, action, resource, resource_id, client_address, occurred_at
                from record_access_log where cabinet_id = ?
                order by occurred_at desc, id desc limit ? offset ?
                """, (rs, row) -> new AccessLogDto.Entry(rs.getLong(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), (Long) rs.getObject(6), rs.getString(7),
                rs.getObject(8, java.time.LocalDateTime.class)),
                cabinetId, Math.max(1, Math.min(limit, 500)), Math.max(0, offset));
    }
}

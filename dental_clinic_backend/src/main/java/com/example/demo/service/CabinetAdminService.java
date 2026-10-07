package com.example.demo.service;

import java.util.List;
import java.util.Locale;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CabinetDto;
import com.example.demo.entity.Cabinet;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.CabinetRepository;

/**
 * Platform-admin operations on cabinets. Statistics are read with plain SQL on purpose:
 * the admin belongs to no cabinet, so tenant-filtered entity queries would see nothing.
 */
@Service
public class CabinetAdminService {
    private final CabinetRepository cabinetRepository;
    private final JdbcTemplate jdbc;

    public CabinetAdminService(CabinetRepository cabinetRepository, JdbcTemplate jdbc) {
        this.cabinetRepository = cabinetRepository;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CabinetDto.Response> list() {
        return cabinetRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CabinetDto.Response get(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CabinetDto.Response create(CabinetDto.Request request) {
        String code = code(request.code());
        String name = name(request.name());
        if (cabinetRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Cabinet code \"" + code + "\" is already used.");
        }
        if (cabinetRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("Cabinet \"" + name + "\" already exists.");
        }
        Cabinet cabinet = new Cabinet();
        apply(cabinet, request, code, name);
        cabinet.setActive(request.active() == null || request.active());
        Cabinet saved = cabinetRepository.saveAndFlush(cabinet);
        if (request.copyCatalogFromCabinetId() != null) {
            copyCatalog(find(request.copyCatalogFromCabinetId()).getId(), saved.getId());
        }
        return toResponse(saved);
    }

    @Transactional
    public CabinetDto.Response update(Long id, CabinetDto.Request request) {
        Cabinet cabinet = find(id);
        String code = code(request.code());
        String name = name(request.name());
        if (cabinetRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessRuleException("Cabinet code \"" + code + "\" is already used.");
        }
        if (cabinetRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException("Cabinet \"" + name + "\" already exists.");
        }
        apply(cabinet, request, code, name);
        if (request.active() != null) cabinet.setActive(request.active());
        return toResponse(cabinetRepository.save(cabinet));
    }

    /** Only an empty cabinet (no member, no data) can be removed; otherwise disable it. */
    @Transactional
    public void delete(Long id) {
        Cabinet cabinet = find(id);
        CabinetDto.Stats stats = stats(id);
        Long members = jdbc.queryForObject("select count(*) from login where cabinet_id = ?", Long.class, id);
        Long expenses = jdbc.queryForObject("select count(*) from expense where cabinet_id = ?", Long.class, id);
        Long materials = jdbc.queryForObject("select count(*) from material_inventory where cabinet_id = ?",
                Long.class, id);
        if ((members != null && members > 0) || stats.patients() > 0 || stats.appointments() > 0
                || stats.treatments() > 0 || (expenses != null && expenses > 0)
                || (materials != null && materials > 0)) {
            throw new BusinessRuleException(
                    "Only an empty cabinet can be deleted. Disable it instead to keep its data.");
        }
        jdbc.update("delete from procedure_catalog where cabinet_id = ?", id);
        jdbc.update("delete from conversation where cabinet_id = ?", id);
        jdbc.update("delete from staff_action where cabinet_id = ?", id);
        cabinetRepository.delete(cabinet);
    }

    private void copyCatalog(Long sourceId, Long targetId) {
        jdbc.update("""
                insert into procedure_catalog
                    (code, name, category, default_cost, default_duration_minutes, description, active, cabinet_id)
                select code, name, category, default_cost, default_duration_minutes, description, active, ?
                from procedure_catalog where cabinet_id = ? and active = true
                """, targetId, sourceId);
    }

    private CabinetDto.Stats stats(Long cabinetId) {
        return jdbc.queryForObject("""
                select
                  (select count(*) from login where cabinet_id = ? and role = 'doctor' and active = true),
                  (select count(*) from login where cabinet_id = ? and role = 'secretaire' and active = true),
                  (select count(*) from patient where cabinet_id = ?),
                  (select count(*) from appointment where cabinet_id = ?),
                  (select count(*) from treatment where cabinet_id = ?)
                """,
                (rs, row) -> new CabinetDto.Stats(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                        rs.getLong(5)),
                cabinetId, cabinetId, cabinetId, cabinetId, cabinetId);
    }

    private Cabinet find(Long id) {
        return cabinetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cabinet", id));
    }

    private void apply(Cabinet cabinet, CabinetDto.Request request, String code, String name) {
        cabinet.setCode(code);
        cabinet.setName(name);
        cabinet.setAddress(blankToNull(request.address()));
        cabinet.setPhoneNumber(blankToNull(request.phoneNumber()));
        cabinet.setEmail(blankToNull(request.email()));
    }

    private CabinetDto.Response toResponse(Cabinet cabinet) {
        return new CabinetDto.Response(cabinet.getId(), cabinet.getName(), cabinet.getCode(), cabinet.getAddress(),
                cabinet.getPhoneNumber(), cabinet.getEmail(), Boolean.TRUE.equals(cabinet.getActive()),
                cabinet.getCreatedAt(), cabinet.getUpdatedAt(), stats(cabinet.getId()));
    }

    private String code(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9_-]{2,40}")) {
            throw new IllegalArgumentException(
                    "Cabinet code must be 2-40 characters: letters, digits, dash or underscore.");
        }
        return code;
    }

    private String name(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty() || name.length() > 160) {
            throw new IllegalArgumentException("Cabinet name is required (max 160 characters).");
        }
        return name;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

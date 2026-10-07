package com.example.demo.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.AdminUserDto;
import com.example.demo.dto.InvitationDto;
import com.example.demo.entity.Cabinet;
import com.example.demo.entity.LoginUser;
import com.example.demo.repository.CabinetRepository;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.security.ClinicPrincipal;

@Service
public class UserAdminService {
    private static final Set<String> ROLES = Set.of("doctor", "secretaire", "manager", "admin");
    private static final Set<String> MEMBER_ROLES = Set.of("doctor", "secretaire");

    private final LoginUserRepository userRepository;
    private final CabinetRepository cabinetRepository;
    private final JdbcTemplate jdbc;
    private final InvitationService invitationService;

    public UserAdminService(LoginUserRepository userRepository, CabinetRepository cabinetRepository,
            JdbcTemplate jdbc, InvitationService invitationService) {
        this.invitationService = invitationService;
        this.userRepository = userRepository;
        this.cabinetRepository = cabinetRepository;
        this.jdbc = jdbc;
    }

    /** All accounts, or only the members of one cabinet when {@code cabinetId} is given. */
    @Transactional(readOnly = true)
    public List<AdminUserDto.Response> list(Long cabinetId) {
        List<LoginUser> users = cabinetId == null
                ? userRepository.findAllByOrderByRoleAscFullNameAsc()
                : userRepository.findByCabinetIdOrderByRoleAscFullNameAsc(requireCabinet(cabinetId).getId());
        Map<Long, String> names = cabinetNames();
        return users.stream().map(user -> toResponse(user, names)).toList();
    }

    @Transactional
    public AdminUserDto.Created create(AdminUserDto.CreateRequest request, ClinicPrincipal actor) {
        String role = role(request.role(), ROLES);
        LoginUser saved = createUser(request.username(), request.fullName(), role,
                cabinetFor(role, request.cabinetId()));
        return created(saved, actor);
    }

    @Transactional
    public AdminUserDto.Response update(Long id, AdminUserDto.UpdateRequest request, ClinicPrincipal actor) {
        LoginUser user = find(id);
        String username = username(request.username());
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("Username \"" + username + "\" is already taken.");
        }
        String role = role(request.role(), ROLES);
        boolean active = request.active() == null ? Boolean.TRUE.equals(user.getActive()) : request.active();

        boolean wasActiveAdmin = "admin".equals(user.getRole()) && Boolean.TRUE.equals(user.getActive());
        boolean staysActiveAdmin = "admin".equals(role) && active;
        if (user.getId().equals(actor.userId()) && !staysActiveAdmin) {
            throw new BusinessRuleException("You cannot deactivate your own account or remove your own admin role.");
        }
        if (wasActiveAdmin && !staysActiveAdmin && userRepository.countByRoleIgnoreCaseAndActiveTrue("admin") <= 1) {
            throw new BusinessRuleException("At least one active admin account is required.");
        }

        Long cabinetId = cabinetFor(role, request.cabinetId());
        if (!Objects.equals(cabinetId, user.getCabinetId()) || !role.equals(user.getRole())) {
            assertCanLeaveCabinet(user);
        }
        assertSingleManager(user, role, active, cabinetId);

        user.setUsername(username);
        user.setFullName(fullName(request.fullName()));
        user.setRole(role);
        user.setCabinetId(cabinetId);
        user.setActive(active);
        LoginUser saved = userRepository.saveAndFlush(user);
        seedDefaultWorkingHours(saved);
        return toResponse(saved);
    }

    /** A new link for an existing account (first password, or a forgotten one). */
    @Transactional
    public InvitationDto.Created invite(Long id, ClinicPrincipal actor) {
        return invitationService.create(find(id), actor.userId());
    }

    // ------------------------------------------------------------- cabinet manager

    /** Doctors and secretaries of one cabinet (the manager account itself is not listed). */
    @Transactional(readOnly = true)
    public List<AdminUserDto.Response> listMembers(Long cabinetId) {
        Map<Long, String> names = cabinetNames();
        return userRepository.findByCabinetIdOrderByRoleAscFullNameAsc(cabinetId).stream()
                .filter(user -> MEMBER_ROLES.contains(user.getRole()))
                .map(user -> toResponse(user, names))
                .toList();
    }

    @Transactional
    public AdminUserDto.Created createMember(Long cabinetId, AdminUserDto.MemberCreateRequest request,
            ClinicPrincipal actor) {
        String role = role(request.role(), MEMBER_ROLES);
        requireActiveCabinet(cabinetId);
        return created(createUser(request.username(), request.fullName(), role, cabinetId), actor);
    }

    @Transactional
    public AdminUserDto.Response updateMember(Long cabinetId, Long id, AdminUserDto.MemberUpdateRequest request) {
        LoginUser user = findMember(cabinetId, id);
        String username = username(request.username());
        if (!username.equalsIgnoreCase(user.getUsername()) && userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("Username \"" + username + "\" is already taken.");
        }
        String role = role(request.role(), MEMBER_ROLES);
        if (!role.equals(user.getRole())) assertCanLeaveCabinet(user);
        user.setUsername(username);
        user.setFullName(fullName(request.fullName()));
        user.setRole(role);
        user.setActive(request.active() == null ? Boolean.TRUE.equals(user.getActive()) : request.active());
        LoginUser saved = userRepository.saveAndFlush(user);
        seedDefaultWorkingHours(saved);
        return toResponse(saved);
    }

    @Transactional
    public InvitationDto.Created inviteMember(Long cabinetId, Long id, ClinicPrincipal actor) {
        return invitationService.create(findMember(cabinetId, id), actor.userId());
    }

    // ------------------------------------------------------------- used by the cabinet service

    /**
     * Makes {@code username} the single active manager of the cabinet; a previous manager is
     * deactivated. Returns the new account with its invitation link.
     */
    @Transactional
    public AdminUserDto.Created replaceManager(Long cabinetId, String username, String fullName,
            ClinicPrincipal actor) {
        requireCabinet(cabinetId);
        userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "manager").forEach(previous -> {
            previous.setActive(false);
            userRepository.save(previous);
        });
        userRepository.flush();
        return created(createUser(username, fullName, "manager", cabinetId), actor);
    }

    @Transactional(readOnly = true)
    public AdminUserDto.Response activeManagerOf(Long cabinetId) {
        return userRepository.findByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "manager").stream()
                .findFirst().map(this::toResponse).orElse(null);
    }

    private LoginUser createUser(String rawUsername, String rawFullName, String role, Long cabinetId) {
        String username = username(rawUsername);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessRuleException("Username \"" + username + "\" is already taken.");
        }
        LoginUser user = new LoginUser();
        user.setUsername(username);
        user.setFullName(fullName(rawFullName));
        user.setRole(role);
        user.setCabinetId(cabinetId);
        user.setActive(true);
        // The owner chooses the real password through the invitation link; until then nobody
        // (including the admin or manager who created the account) knows a working password.
        user.setPassword(UUID.randomUUID() + UUID.randomUUID().toString());
        assertSingleManager(user, role, true, cabinetId);
        LoginUser saved = userRepository.saveAndFlush(user);
        seedDefaultWorkingHours(saved);
        return saved;
    }

    private AdminUserDto.Created created(LoginUser user, ClinicPrincipal actor) {
        return new AdminUserDto.Created(toResponse(user), invitationService.create(user, actor.userId()));
    }

    private void assertSingleManager(LoginUser user, String role, boolean active, Long cabinetId) {
        if (!"manager".equals(role) || !active) return;
        boolean alreadyThisManager = user.getId() != null && "manager".equals(user.getRole()) && Boolean.TRUE.equals(user.getActive())
                && Objects.equals(cabinetId, user.getCabinetId());
        if (!alreadyThisManager
                && userRepository.countByCabinetIdAndRoleIgnoreCaseAndActiveTrue(cabinetId, "manager") > 0) {
            throw new BusinessRuleException(
                    "This cabinet already has an active manager. Replace it from the cabinet page.");
        }
    }

    private LoginUser findMember(Long cabinetId, Long id) {
        return userRepository.findById(id)
                .filter(user -> Objects.equals(cabinetId, user.getCabinetId()))
                .filter(user -> MEMBER_ROLES.contains(user.getRole()))
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private void requireActiveCabinet(Long cabinetId) {
        if (!Boolean.TRUE.equals(requireCabinet(cabinetId).getActive())) {
            throw new BusinessRuleException("This cabinet is disabled.");
        }
    }

    private Map<Long, String> cabinetNames() {
        Map<Long, String> names = new HashMap<>();
        cabinetRepository.findAll().forEach(cabinet -> names.put(cabinet.getId(), cabinet.getName()));
        return names;
    }

    private LoginUser find(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private AdminUserDto.Response toResponse(LoginUser user) {
        Map<Long, String> names = new HashMap<>();
        if (user.getCabinetId() != null) {
            cabinetRepository.findById(user.getCabinetId())
                    .ifPresent(cabinet -> names.put(cabinet.getId(), cabinet.getName()));
        }
        return toResponse(user, names);
    }

    private AdminUserDto.Response toResponse(LoginUser user, Map<Long, String> cabinetNames) {
        return new AdminUserDto.Response(user.getId(), user.getUsername(), user.getFullName(), user.getRole(),
                Boolean.TRUE.equals(user.getActive()), user.getCreatedAt(), user.getUpdatedAt(),
                user.getCabinetId(), user.getCabinetId() == null ? null : cabinetNames.get(user.getCabinetId()),
                invitationService.hasOpenInvitation(user.getId()));
    }

    /** A new doctor can receive appointments right away: Mon-Thu 08-17, Fri 08-15, weekend off. */
    private void seedDefaultWorkingHours(LoginUser user) {
        if (!"doctor".equals(user.getRole())) return;
        jdbc.update("""
                insert into doctor_working_hours (doctor_user_id, day_of_week, working, start_time, end_time)
                select ?, day.day_of_week, day.day_of_week between 1 and 5,
                       case when day.day_of_week between 1 and 5 then time '08:00' end,
                       case when day.day_of_week between 1 and 4 then time '17:00'
                            when day.day_of_week = 5 then time '15:00' end
                from generate_series(1, 7) as day(day_of_week)
                on conflict (doctor_user_id, day_of_week) do nothing
                """, user.getId());
    }

    private Cabinet requireCabinet(Long id) {
        return cabinetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cabinet", id));
    }

    /** Doctors and secretaries belong to exactly one cabinet; the admin belongs to none. */
    private Long cabinetFor(String role, Long cabinetId) {
        if ("admin".equals(role)) {
            if (cabinetId != null) throw new IllegalArgumentException("An admin does not belong to a cabinet.");
            return null;
        }
        if (cabinetId == null) throw new IllegalArgumentException("A cabinet is required for this role.");
        return requireCabinet(cabinetId).getId();
    }

    /**
     * A member that already owns cabinet data (patients, appointments, treatments, audit entries or
     * conversations) cannot be moved to another cabinet or role without orphaning that data.
     */
    private void assertCanLeaveCabinet(LoginUser user) {
        if (user.getCabinetId() == null) return;
        Long id = user.getId();
        Long linked = jdbc.queryForObject("""
                select (select count(*) from patient where assigned_doctor_user_id = ?)
                     + (select count(*) from treatment where doctor_user_id = ?)
                     + (select count(*) from appointment where provider_user_id = ?)
                     + (select count(*) from staff_action where actor_user_id = ? or undone_by_user_id = ?)
                     + (select count(*) from conversation_participant where user_id = ?)
                """, Long.class, id, id, id, id, id, id);
        if (linked != null && linked > 0) {
            throw new BusinessRuleException("This user already has patients, appointments, treatments or messages "
                    + "in the current cabinet. Disable the account and create a new one in the other cabinet.");
        }
    }

    private String username(String value) {
        String username = value == null ? "" : value.trim().toLowerCase();
        if (!username.matches("[a-z0-9._-]{3,80}")) {
            throw new IllegalArgumentException(
                    "Username must be 3-80 characters: letters, digits, dot, dash or underscore.");
        }
        return username;
    }

    private String fullName(String value) {
        String fullName = value == null ? "" : value.trim();
        if (fullName.isEmpty() || fullName.length() > 160) {
            throw new IllegalArgumentException("Full name is required (max 160 characters).");
        }
        return fullName;
    }

    private String role(String value, Set<String> allowed) {
        String role = value == null ? "" : value.trim().toLowerCase();
        if (!allowed.contains(role)) {
            throw new IllegalArgumentException("Role must be one of: " + String.join(", ", new java.util.TreeSet<>(allowed)) + ".");
        }
        return role;
    }
}

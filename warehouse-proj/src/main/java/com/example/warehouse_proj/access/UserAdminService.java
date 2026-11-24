package com.example.warehouse_proj.access;

import com.example.warehouse_proj.access.AccessDtos.UserRequest;
import com.example.warehouse_proj.access.AccessDtos.UserResponse;
import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class UserAdminService {

    private static final String ENTITY = "User";

    private final AppUserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService audit;

    public UserAdminService(
            AppUserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder, AuditLogService audit) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserResponse> list() {
        return users.findAllByActiveTrue().stream()
                .sorted(Comparator.comparing(AppUser::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(UserAdminService::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required for a new user");
        }
        AppUser user = new AppUser(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        user.setRole(findRole(request.roleId()));
        users.save(user);
        audit.record(AuditAction.CREATE, ENTITY, user.getId());
        return toResponse(user);
    }

    @Transactional
    public UserResponse update(UUID id, UserRequest request) {
        AppUser user = find(id);
        String email = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(email) && users.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }

        Role newRole = findRole(request.roleId());
        boolean roleChanged = user.getRole() == null || !user.getRole().getId().equals(newRole.getId());
        if (roleChanged) {
            if (id.equals(currentUserId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot change your own role");
            }
            requireAnotherAdminRemains(user);
        }

        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        user.setRole(newRole);
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
            // A new password signs the person out everywhere.
            user.setRefreshToken(null);
            user.setRefreshTokenExpiresAt(null);
        }
        users.saveAndFlush(user);
        audit.record(AuditAction.UPDATE, ENTITY, user.getId());
        return toResponse(user);
    }

    /**
     * Soft delete: the user can no longer sign in. Any other account can be deleted; admin accounts never can
     * (change the person's role first if they should go), so the app can never be left without an admin.
     */
    @Transactional
    public void delete(UUID id) {
        AppUser user = find(id);
        if (id.equals(currentUserId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot delete your own account");
        }
        if (user.getRole() != null && user.getRole().isAdmin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Admin accounts cannot be deleted");
        }
        user.setActive(false);
        user.setRefreshToken(null);
        user.setRefreshTokenExpiresAt(null);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /** Refuses to take the last active admin out of the admin role (or out of service). */
    private void requireAnotherAdminRemains(AppUser user) {
        Role role = user.getRole();
        if (role != null && role.isAdmin() && users.countByRoleIdAndActiveTrue(role.getId()) <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "There must be at least one active admin");
        }
    }

    private AppUser find(UUID id) {
        return users.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private Role findRole(UUID id) {
        return roles.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private static UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        try {
            return auth == null ? null : UUID.fromString(auth.getName());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static UserResponse toResponse(AppUser u) {
        Role role = u.getRole();
        return new UserResponse(
                u.getId(),
                u.getFirstName(),
                u.getLastName(),
                u.getEmail(),
                role == null ? null : role.getId(),
                role == null ? null : role.getName(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }
}

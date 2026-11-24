package com.example.warehouse_proj.access;

import com.example.warehouse_proj.access.AccessDtos.RoleRequest;
import com.example.warehouse_proj.access.AccessDtos.RoleResponse;
import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
public class RoleAdminService {

    private static final String ENTITY = "Role";

    private final RoleRepository roles;
    private final AppUserRepository users;
    private final AuditLogService audit;

    public RoleAdminService(RoleRepository roles, AppUserRepository users, AuditLogService audit) {
        this.roles = roles;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roles.findAllByActiveTrue().stream()
                .sorted(Comparator.comparing((Role r) -> !r.isSystem()).thenComparing(Role::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RoleResponse create(RoleRequest request) {
        String name = request.name().trim();
        if (roles.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role named \"" + name + "\" already exists");
        }
        Role role = new Role(name, blankToNull(request.description()), false);
        role.setPermissions(validPermissions(request.permissions()));
        role.setPages(validPages(request.pages()));
        roles.save(role);
        audit.record(AuditAction.CREATE, ENTITY, role.getId());
        return toResponse(role);
    }

    /** Edits the name, description and permissions. The admin role keeps its name and everything it has. */
    @Transactional
    public RoleResponse update(UUID id, RoleRequest request) {
        Role role = find(id);
        String name = request.name().trim();
        if (role.isSystem() && !role.getName().equalsIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A standard role cannot be renamed");
        }
        if (!role.getName().equalsIgnoreCase(name) && roles.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A role named \"" + name + "\" already exists");
        }
        role.setName(name);
        role.setDescription(blankToNull(request.description()));
        if (role.isAdmin()) {
            role.setPermissions(AccessCatalog.allPermissions());
        } else if (request.permissions() != null) {
            role.setPermissions(validPermissions(request.permissions()));
        }
        roles.saveAndFlush(role);
        audit.record(AuditAction.UPDATE, ENTITY, role.getId());
        return toResponse(role);
    }

    /** Page assignment: which pages the role may open. The admin role always has every page. */
    @Transactional
    public RoleResponse assignPages(UUID id, Set<String> pages) {
        Role role = find(id);
        if (role.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The admin role always has every page");
        }
        role.setPages(validPages(pages));
        roles.saveAndFlush(role);
        audit.record(AuditAction.UPDATE, ENTITY, role.getId());
        return toResponse(role);
    }

    /** Removes the role for good (its name would otherwise stay taken). Refused while anyone still has it. */
    @Transactional
    public void delete(UUID id) {
        Role role = find(id);
        if (role.isSystem()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A standard role cannot be deleted");
        }
        if (users.existsByRoleId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role is still assigned to users");
        }
        roles.delete(role);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Role find(UUID id) {
        return roles.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Role not found"));
    }

    private RoleResponse toResponse(Role role) {
        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isSystem(),
                List.copyOf(new TreeSet<>(role.getPermissions())),
                List.copyOf(new TreeSet<>(role.getPages())),
                users.countByRoleIdAndActiveTrue(role.getId()),
                role.getCreatedAt(),
                role.getUpdatedAt());
    }

    private static Set<String> validPermissions(Set<String> permissions) {
        Set<String> input = permissions == null ? Set.of() : permissions;
        for (String permission : input) {
            if (!AccessCatalog.isValidPermission(permission)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown permission: " + permission);
            }
        }
        return input;
    }

    private static Set<String> validPages(Set<String> pages) {
        Set<String> input = pages == null ? Set.of() : pages;
        for (String page : input) {
            if (!AccessCatalog.isValidPage(page)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown page: " + page);
            }
        }
        return input;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}

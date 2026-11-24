package com.example.warehouse_proj.access;

import com.example.warehouse_proj.access.AccessDtos.CatalogResponse;
import com.example.warehouse_proj.access.AccessDtos.PageInfo;
import com.example.warehouse_proj.access.AccessDtos.RolePagesRequest;
import com.example.warehouse_proj.access.AccessDtos.RoleRequest;
import com.example.warehouse_proj.access.AccessDtos.RoleResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Roles, their permissions and page assignment. Each call needs the matching permission. */
@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final RoleAdminService roles;

    public AccessController(RoleAdminService roles) {
        this.roles = roles;
    }

    /** All permissions and pages that can be granted. */
    @GetMapping("/catalog")
    public CatalogResponse catalog() {
        List<PageInfo> pages = AccessCatalog.PAGES.stream()
                .map(p -> new PageInfo(p.path(), p.label(), p.section()))
                .toList();
        return new CatalogResponse(AccessCatalog.RESOURCES, AccessCatalog.ACTIONS, pages);
    }

    /** Also used by the Users and Page Assignment screens, so any of the three permissions will do. */
    @GetMapping("/roles")
    @PreAuthorize("hasAnyAuthority('roles:view', 'users:view', 'pages:view')")
    public List<RoleResponse> list() {
        return roles.list();
    }

    @PostMapping("/roles")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('roles:create')")
    public RoleResponse create(@Valid @RequestBody RoleRequest request) {
        return roles.create(request);
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('roles:update')")
    public RoleResponse update(@PathVariable UUID id, @Valid @RequestBody RoleRequest request) {
        return roles.update(id, request);
    }

    /** Page assignment for one role. */
    @PutMapping("/roles/{id}/pages")
    @PreAuthorize("hasAuthority('pages:update')")
    public RoleResponse assignPages(@PathVariable UUID id, @Valid @RequestBody RolePagesRequest request) {
        return roles.assignPages(id, request.pages());
    }

    @DeleteMapping("/roles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('roles:delete')")
    public void delete(@PathVariable UUID id) {
        roles.delete(id);
    }
}

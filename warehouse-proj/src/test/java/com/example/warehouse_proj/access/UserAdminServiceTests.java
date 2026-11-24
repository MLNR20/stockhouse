package com.example.warehouse_proj.access;

import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Which accounts can be deleted: everyone except admins (and yourself). */
class UserAdminServiceTests {

    private AppUserRepository users;
    private UserAdminService service;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        service = new UserAdminService(
                users, mock(RoleRepository.class), mock(PasswordEncoder.class), mock(AuditLogService.class));
    }

    private AppUser userWithRole(String roleName, boolean system) {
        AppUser user = new AppUser("Test", "User", roleName + "@example.com", "hash");
        user.setRole(new Role(roleName, null, system));
        when(users.findByIdAndActiveTrue(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    void adminAccountsCannotBeDeleted() {
        AppUser admin = userWithRole(AccessCatalog.ADMIN, true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.delete(admin.getId()));

        assertEquals(HttpStatus.CONFLICT, e.getStatusCode());
        assertTrue(admin.isActive(), "the admin must still be active");
    }

    @Test
    void otherAccountsAreSoftDeleted() {
        AppUser viewer = userWithRole(AccessCatalog.VIEWER, true);

        service.delete(viewer.getId());

        assertFalse(viewer.isActive());
        assertEquals(null, viewer.getRefreshToken(), "a deleted user is signed out everywhere");
    }

    @Test
    void aCustomRoleNamedAdminIsNotTreatedAsTheAdminRole() {
        // Only the standard (system) admin role is protected; a role someone created is not.
        AppUser lookalike = userWithRole("admin", false);

        service.delete(lookalike.getId());

        assertFalse(lookalike.isActive());
    }
}

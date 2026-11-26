package com.example.warehouse_proj.access;

import com.example.warehouse_proj.access.AccessCatalog.RoleDefinition;
import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * Makes sure the standard roles exist and that every user has one. Runs on every start but only fills gaps:
 * it never changes a role that already exists, so edits made on the Roles and Page Assignment screens stay.
 *
 * <p>Users who existed before roles were introduced become admins (they were all-powerful until now).
 * Not tied to app.seed.enabled: without roles nobody could use the app.
 */
@Component
@Order(0)
public class AccessBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AccessBootstrap.class);

    private final TransactionTemplate tx;
    private final RoleRepository roles;
    private final AppUserRepository users;

    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AccessBootstrap(
            TransactionTemplate tx,
            RoleRepository roles,
            AppUserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String adminEmail,
            @Value("${app.admin.password:}") String adminPassword) {
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
        this.tx = tx;
        this.roles = roles;
        this.users = users;
    }

    /** On an empty database, creates the first admin from ADMIN_EMAIL / ADMIN_PASSWORD. */
    private void createFirstAdmin(Role admin) {
        if (users.count() > 0) {
            return;
        }
        if (adminEmail.isBlank() || adminPassword.length() < 8) {
            log.warn("There are no users yet. Set ADMIN_EMAIL and ADMIN_PASSWORD (8+ characters) and restart "
                    + "to create the first admin.");
            return;
        }
        AppUser first = new AppUser("Admin", "User", adminEmail.trim().toLowerCase(),
                passwordEncoder.encode(adminPassword));
        first.setRole(admin);
        users.save(first);
        log.info("Created the first admin account from ADMIN_EMAIL");
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            for (RoleDefinition def : AccessCatalog.standardRoles()) {
                if (roles.findByNameIgnoreCase(def.name()).isEmpty()) {
                    Role role = new Role(def.name(), def.description(), true);
                    role.setPermissions(def.permissions());
                    role.setPages(def.pages());
                    roles.save(role);
                    log.info("Created standard role \"{}\"", def.name());
                }
            }

            // The admin role always holds everything, including permissions and pages added in later versions.
            Role admin = roles.findByNameIgnoreCase(AccessCatalog.ADMIN).orElseThrow();
            if (!admin.getPermissions().containsAll(AccessCatalog.allPermissions())
                    || !admin.getPages().containsAll(AccessCatalog.allPagePaths())) {
                admin.getPermissions().addAll(AccessCatalog.allPermissions());
                admin.getPages().addAll(AccessCatalog.allPagePaths());
                roles.save(admin);
            }
            createFirstAdmin(admin);

            List<AppUser> withoutRole = users.findAll().stream().filter(u -> u.getRole() == null).toList();
            for (AppUser user : withoutRole) {
                user.setRole(admin);
                users.save(user);
            }
            if (!withoutRole.isEmpty()) {
                log.info("Gave the admin role to {} existing user(s) that had no role", withoutRole.size());
            }
        });
    }
}

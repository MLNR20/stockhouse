package com.example.warehouse_proj.access;

import com.example.warehouse_proj.access.AccessDtos.AccessSummary;
import com.example.warehouse_proj.identity.AppUser;
import com.example.warehouse_proj.identity.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/** Looks up what a user is allowed to do. Read on every request by the JWT filter. */
@Service
public class AccessService {

    private final AppUserRepository users;

    public AccessService(AppUserRepository users) {
        this.users = users;
    }

    /** Empty when the user no longer exists or has been deactivated. A user without a role gets nothing. */
    @Transactional(readOnly = true)
    public Optional<AccessSummary> summaryFor(UUID userId) {
        return users.findByIdAndActiveTrue(userId).map(AccessService::summaryOf);
    }

    public static AccessSummary summaryOf(AppUser user) {
        Role role = user.getRole();
        if (role == null) {
            return new AccessSummary(null, Set.of(), Set.of());
        }
        return new AccessSummary(role.getName(), new TreeSet<>(role.getPermissions()), new TreeSet<>(role.getPages()));
    }
}

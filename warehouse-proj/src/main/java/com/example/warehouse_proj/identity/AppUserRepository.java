package com.example.warehouse_proj.identity;

import com.example.warehouse_proj.common.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends BaseRepository<AppUser> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Any user, active or not: a role still referenced cannot be deleted. */
    boolean existsByRoleId(UUID roleId);

    long countByRoleIdAndActiveTrue(UUID roleId);
}

package com.example.warehouse_proj.access;

import com.example.warehouse_proj.common.BaseRepository;

import java.util.Optional;

public interface RoleRepository extends BaseRepository<Role> {

    Optional<Role> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}

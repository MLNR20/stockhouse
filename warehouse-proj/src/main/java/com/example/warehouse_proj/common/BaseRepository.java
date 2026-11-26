package com.example.warehouse_proj.common;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generic repository for every model. Extend it per entity:
 * {@code interface FooRepository extends BaseRepository<Foo> {}}
 */
@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, UUID>, JpaSpecificationExecutor<T> {

    List<T> findAllByActiveTrue();

    /** Server-side pagination over active rows; see PaginationService. */
    Page<T> findAllByActiveTrue(Pageable pageable);

    Optional<T> findByIdAndActiveTrue(UUID id);

    long countByActiveTrue();
}

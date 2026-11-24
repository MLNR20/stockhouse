package com.example.warehouse_proj.product;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.example.warehouse_proj.analytics.CategoryStat;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends BaseRepository<Product> {

    @Override
    @EntityGraph(attributePaths = "category")
    List<Product> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = "category")
    Page<Product> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "category")
    Optional<Product> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByCategoryIdAndActiveTrue(UUID categoryId);

    boolean existsByCategoryIdAndActiveTrue(UUID categoryId);

    /** Active products per category name; uncategorised products come back with a null category. */
    @Query("""
            select new com.example.warehouse_proj.analytics.CategoryStat(c.name, count(p))
            from Product p left join p.category c
            where p.active = true
            group by c.name
            order by count(p) desc""")
    List<CategoryStat> countByCategory();
}

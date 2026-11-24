package com.example.warehouse_proj.howsold;

import com.example.warehouse_proj.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HowSoldConversionRepository extends BaseRepository<HowSoldConversion> {

    @Override
    @EntityGraph(attributePaths = {"parentHowSold", "childHowSold", "product"})
    List<HowSoldConversion> findAllByActiveTrue();

    @Override
    @EntityGraph(attributePaths = {"parentHowSold", "childHowSold", "product"})
    Page<HowSoldConversion> findAllByActiveTrue(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"parentHowSold", "childHowSold", "product"})
    Optional<HowSoldConversion> findByIdAndActiveTrue(UUID id);

    @EntityGraph(attributePaths = {"parentHowSold", "childHowSold", "product"})
    List<HowSoldConversion> findAllByProductIdAndActiveTrue(UUID productId);
}

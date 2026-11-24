package com.example.warehouse_proj.howsold;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Walks unit conversions down to the base unit: with "1 Pallet = 40 Case" and "1 Case = 12 Each", a pallet
 * is 480 Each, and Each is the base unit because nothing converts it into anything smaller.
 *
 * <p>For a product, a conversion assigned to that product wins over a general one (product = null) for the
 * same larger unit. Built from a snapshot of all active conversions, so one instance can resolve many lines
 * without further queries.
 */
public final class UnitConversions {

    /** How many base units one of the given unit is, and which unit is the base. */
    public record Resolved(BigDecimal factor, HowSold baseUnit) {
    }

    private final List<HowSoldConversion> all;

    public UnitConversions(List<HowSoldConversion> allActive) {
        this.all = allActive;
    }

    public Resolved toBase(UUID productId, HowSold unit) {
        BigDecimal factor = BigDecimal.ONE;
        HowSold current = unit;
        Set<UUID> seen = new HashSet<>();
        while (seen.add(current.getId())) { // the set also stops a cycle (A -> B -> A) from looping forever
            HowSoldConversion edge = edgeFrom(productId, current.getId());
            if (edge == null) {
                return new Resolved(factor, current);
            }
            factor = factor.multiply(edge.getUnitConversion());
            current = edge.getChildHowSold();
        }
        return new Resolved(factor, current);
    }

    /** The conversion to follow from a larger unit: this product's own, else a general one. */
    private HowSoldConversion edgeFrom(UUID productId, UUID parentId) {
        HowSoldConversion general = null;
        for (HowSoldConversion c : all) {
            if (!c.getParentHowSold().getId().equals(parentId)) {
                continue;
            }
            if (c.getProduct() == null) {
                if (general == null) {
                    general = c;
                }
            } else if (c.getProduct().getId().equals(productId)) {
                return c;
            }
        }
        return general;
    }
}

package com.example.warehouse_proj.orderdetail;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.howsold.UnitConversions;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.ProductStock;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderDetailService {

    private static final String ENTITY = "OrderDetail";
    private static final Set<String> SORT_FIELDS = Set.of("quantity", "salesPrice");
    private static final List<String> SEARCH_FIELDS = List.of(
            "product.name");

    private final OrderDetailRepository details;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final InventoryRepository inventories;
    private final HowSoldRepository howSolds;
    private final HowSoldConversionRepository conversions;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public OrderDetailService(
            OrderDetailRepository details,
            OrderRepository orders,
            ProductRepository products,
            InventoryRepository inventories,
            HowSoldRepository howSolds,
            HowSoldConversionRepository conversions,
            AuditLogService audit,
            PaginationService pagination) {
        this.details = details;
        this.orders = orders;
        this.products = products;
        this.inventories = inventories;
        this.howSolds = howSolds;
        this.conversions = conversions;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<OrderDetailDtos.Response> list(UUID orderId) {
        findOrder(orderId); // 404 for an unknown order instead of an empty list
        return toResponses(details.findAllByCustomerOrderIdAndActiveTrue(orderId));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDetailDtos.Response> page(PageQuery query) {
        Page<OrderDetail> result = pagination.find(details, query, SORT_FIELDS, SEARCH_FIELDS);
        Resolver resolver = resolverFor(result.getContent());
        return PageResponse.of(result, resolver::response);
    }

    @Transactional(readOnly = true)
    public OrderDetailDtos.Response get(UUID id) {
        return toResponses(List.of(find(id))).getFirst();
    }

    @Transactional
    public OrderDetailDtos.Response create(UUID orderId, OrderDetailDtos.Request request) {
        CustomerOrder order = findOrder(orderId);
        requireEditable(order);
        Product product = findProduct(request.productId());
        if (details.existsByCustomerOrderIdAndProductIdAndActiveTrue(orderId, product.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is already on the order; edit its line instead");
        }
        OrderDetail detail = new OrderDetail(
                order,
                product,
                request.quantity(),
                request.salesPrice() != null ? request.salesPrice() : product.getBasePrice());
        detail.setHowSold(chooseHowSold(request.howSoldId(), product));
        details.save(detail);
        audit.record(AuditAction.CREATE, ENTITY, detail.getId());
        return toResponses(List.of(detail)).getFirst();
    }

    @Transactional
    public OrderDetailDtos.Response update(UUID id, OrderDetailDtos.Request request) {
        OrderDetail detail = find(id);
        requireEditable(detail.getCustomerOrder());

        if (!detail.getProduct().getId().equals(request.productId())) {
            Product product = findProduct(request.productId());
            if (details.existsByCustomerOrderIdAndProductIdAndActiveTrueAndIdNot(
                    detail.getCustomerOrder().getId(), product.getId(), id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This product is already on the order");
            }
            detail.setProduct(product);
        }
        detail.setQuantity(request.quantity());
        detail.setSalesPrice(request.salesPrice() != null ? request.salesPrice() : detail.getProduct().getBasePrice());
        // Keeping the unit it already has is always fine, even if that unit was deleted since.
        HowSold unit = detail.getHowSold();
        if (unit == null || !unit.getId().equals(request.howSoldId())) {
            detail.setHowSold(chooseHowSold(request.howSoldId(), detail.getProduct()));
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        details.saveAndFlush(detail);
        audit.record(AuditAction.UPDATE, ENTITY, detail.getId());
        return toResponses(List.of(detail)).getFirst();
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        OrderDetail detail = find(id);
        requireEditable(detail.getCustomerOrder());
        detail.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    /** Lines can only change while the order has not shipped (pending or confirmed). */
    private static void requireEditable(CustomerOrder order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The items of a " + order.getStatus().name().toLowerCase(Locale.ROOT) + " order can no longer be changed");
        }
    }

    /** The unit asked for, or when none was given the unit the product's inventory is sold in (may be null). */
    private HowSold chooseHowSold(UUID requestedId, Product product) {
        if (requestedId != null) {
            return howSolds.findByIdAndActiveTrue(requestedId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "How sold not found"));
        }
        return inventories.findAllByProductIdAndActiveTrue(product.getId()).stream()
                .map(Inventory::getHowSold)
                .filter(Objects::nonNull)
                .filter(HowSold::isActive) // a deleted unit is never given to a line by default
                .findFirst()
                .orElse(null);
    }

    private List<OrderDetailDtos.Response> toResponses(Collection<OrderDetail> list) {
        Resolver resolver = resolverFor(list);
        return list.stream().map(resolver::response).toList();
    }

    /** Looks up stock and unit conversions once for a batch of lines, not once per line. */
    private Resolver resolverFor(Collection<OrderDetail> list) {
        if (list.isEmpty()) {
            return new Resolver(Map.of(), new UnitConversions(List.of()));
        }
        List<UUID> productIds = list.stream().map(d -> d.getProduct().getId()).distinct().toList();
        Map<UUID, Long> stock = inventories.stockFor(productIds).stream()
                .collect(Collectors.toMap(ProductStock::productId, ProductStock::quantity, Long::sum));
        boolean anyUnit = list.stream().anyMatch(d -> d.getHowSold() != null);
        UnitConversions unitConversions =
                new UnitConversions(anyUnit ? conversions.findAllByActiveTrue() : List.of());
        return new Resolver(stock, unitConversions);
    }

    private record Resolver(Map<UUID, Long> stock, UnitConversions unitConversions) {

        OrderDetailDtos.Response response(OrderDetail d) {
            UnitConversions.Resolved base = d.getHowSold() == null
                    ? new UnitConversions.Resolved(BigDecimal.ONE, null)
                    : unitConversions.toBase(d.getProduct().getId(), d.getHowSold());
            return OrderDetailDtos.Response.from(d, stock.getOrDefault(d.getProduct().getId(), 0L), base);
        }
    }

    private OrderDetail find(UUID id) {
        return details.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order line not found"));
    }

    private CustomerOrder findOrder(UUID id) {
        return orders.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private Product findProduct(UUID id) {
        return products.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }
}

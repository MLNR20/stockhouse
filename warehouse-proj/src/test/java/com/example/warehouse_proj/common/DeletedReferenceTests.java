package com.example.warehouse_proj.common;

import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.customer.Customer;
import com.example.warehouse_proj.customer.CustomerRepository;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldConversion;
import com.example.warehouse_proj.howsold.HowSoldConversionDtos;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.howsold.HowSoldConversionService;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryDtos;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.InventoryService;
import com.example.warehouse_proj.inventory.InventoryStatus;
import com.example.warehouse_proj.notification.NotificationService;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderDtos;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderService;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.orderdetail.OrderDetail;
import com.example.warehouse_proj.orderdetail.OrderDetailDtos;
import com.example.warehouse_proj.orderdetail.OrderDetailRepository;
import com.example.warehouse_proj.orderdetail.OrderDetailService;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.reorder.Reorder;
import com.example.warehouse_proj.reorder.ReorderDtos;
import com.example.warehouse_proj.reorder.ReorderRepository;
import com.example.warehouse_proj.reorder.ReorderService;
import com.example.warehouse_proj.reorder.ReorderStatus;
import com.example.warehouse_proj.report.OrdersPdfReport;
import com.example.warehouse_proj.shipment.ShipmentRepository;
import com.example.warehouse_proj.supplier.Supplier;
import com.example.warehouse_proj.supplier.SupplierProduct;
import com.example.warehouse_proj.supplier.SupplierProductRepository;
import com.example.warehouse_proj.supplier.SupplierRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Records that point at something since deleted: they can still be saved while they keep it, and nothing new can be
 * pointed at it. A mocked repository answers "not found" for a deleted record, as findByIdAndActiveTrue does.
 */
class DeletedReferenceTests {

    private final AuditLogService audit = mock(AuditLogService.class);
    private final NotificationService notifications = mock(NotificationService.class);
    private final PaginationService pagination = new PaginationService();

    private final Product product = new Product("Wireless Mouse", BigDecimal.TEN);

    private static HowSold deletedUnit() {
        HowSold unit = new HowSold("Case");
        unit.setActive(false);
        return unit;
    }

    private static void assertNotFound(Executable action) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, action);
        assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
    }

    // ---- customers ---------------------------------------------------------------------

    private final OrderRepository orders = mock(OrderRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);

    private OrderService orderService() {
        return new OrderService(orders, customers, audit, pagination, notifications,
                mock(ShipmentRepository.class), mock(OrderDetailRepository.class));
    }

    private CustomerOrder orderOfADeletedCustomer() {
        Customer customer = new Customer("Ava", "Johnson", "ava@example.com", "555", "12 Maple Street");
        customer.setActive(false);
        CustomerOrder order = new CustomerOrder(customer, BigDecimal.TEN, LocalDate.now(), OrderStatus.PENDING);
        when(orders.findByIdAndActiveTrue(order.getId())).thenReturn(Optional.of(order));
        return order;
    }

    private static OrderDtos.Request orderRequest(UUID customerId, OrderStatus status) {
        return new OrderDtos.Request(customerId, BigDecimal.TEN, LocalDate.now(), status);
    }

    @Test
    void anOrderOfADeletedCustomerCanStillBeUpdated() {
        CustomerOrder order = orderOfADeletedCustomer();

        OrderDtos.Response saved = orderService()
                .update(order.getId(), orderRequest(order.getCustomer().getId(), OrderStatus.CONFIRMED));

        assertEquals(OrderStatus.CONFIRMED, saved.status());
        assertEquals("Ava Johnson", saved.customerName());
        assertFalse(saved.customerActive());
    }

    @Test
    void anOrderCannotBeMovedToADeletedCustomer() {
        CustomerOrder order = orderOfADeletedCustomer();

        assertNotFound(() -> orderService().update(order.getId(), orderRequest(UUID.randomUUID(), null)));
    }

    @Test
    void aNewOrderCannotBeMadeForADeletedCustomer() {
        assertNotFound(() -> orderService().create(orderRequest(UUID.randomUUID(), null)));
    }

    @Test
    void theOrdersReportNamesADeletedCustomerAndSaysSo() {
        CustomerOrder order = orderOfADeletedCustomer();
        when(orders.findAllByActiveTrue()).thenReturn(List.of(order));

        List<List<String>> rows = new OrdersPdfReport(orderService()).rows();

        assertEquals("Ava Johnson (deleted)", rows.getFirst().get(1));
    }

    // ---- units: inventory --------------------------------------------------------------

    private final InventoryRepository inventories = mock(InventoryRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final HowSoldRepository howSolds = mock(HowSoldRepository.class);

    private InventoryService inventoryService() {
        when(products.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));
        return new InventoryService(inventories, products, howSolds, audit, pagination, notifications);
    }

    private Inventory inventoryIn(HowSold unit) {
        Inventory inventory = new Inventory(product, InventoryStatus.IN_STOCK);
        inventory.setHowSold(unit);
        when(inventories.findByIdAndActiveTrue(inventory.getId())).thenReturn(Optional.of(inventory));
        return inventory;
    }

    private InventoryDtos.Request inventoryRequest(UUID unitId) {
        return new InventoryDtos.Request(product.getId(), InventoryStatus.IN_STOCK, 7, 2, unitId);
    }

    @Test
    void inventoryCanKeepItsDeletedUnit() {
        HowSold unit = deletedUnit();
        Inventory inventory = inventoryIn(unit);

        InventoryDtos.Response saved = inventoryService().update(inventory.getId(), inventoryRequest(unit.getId()));

        assertEquals(7, saved.baseQuantity());
        assertEquals("Case", saved.howSoldName());
        assertFalse(saved.howSoldActive());
    }

    @Test
    void inventoryCannotSwitchToADeletedUnit() {
        Inventory inventory = inventoryIn(new HowSold("Each"));

        assertNotFound(() -> inventoryService().update(inventory.getId(), inventoryRequest(deletedUnit().getId())));
    }

    // ---- units: reorders ---------------------------------------------------------------

    private final ReorderRepository reorders = mock(ReorderRepository.class);
    private final SupplierProductRepository supplierProducts = mock(SupplierProductRepository.class);
    private final Supplier supplier = new Supplier("Acme", "555", "sales@example.com");

    private ReorderService reorderService() {
        return new ReorderService(reorders, mock(SupplierRepository.class), products, supplierProducts, inventories,
                howSolds, audit, pagination, notifications);
    }

    @Test
    void aReorderWithADeletedUnitCanBeMarkedReceived() {
        HowSold unit = deletedUnit();
        Reorder reorder = new Reorder(supplier, product, 5, null, ReorderStatus.ORDERED);
        reorder.setHowSold(unit);
        when(reorders.findByIdAndActiveTrue(reorder.getId())).thenReturn(Optional.of(reorder));
        when(supplierProducts.findBySupplierIdAndProductId(supplier.getId(), product.getId()))
                .thenReturn(Optional.of(new SupplierProduct(supplier, product)));

        ReorderDtos.Response saved = reorderService().update(reorder.getId(), new ReorderDtos.Request(
                supplier.getId(), product.getId(), 5, unit.getId(), null, ReorderStatus.RECEIVED));

        assertEquals(ReorderStatus.RECEIVED, saved.status());
        assertEquals("Case", saved.howSoldName());
        assertFalse(saved.howSoldActive());
    }

    // ---- units: order items ------------------------------------------------------------

    private final OrderDetailRepository details = mock(OrderDetailRepository.class);

    private OrderDetailService orderDetailService() {
        return new OrderDetailService(details, orders, products, inventories, howSolds,
                mock(HowSoldConversionRepository.class), audit, pagination);
    }

    @Test
    void anOrderItemCanKeepItsDeletedUnit() {
        HowSold unit = deletedUnit();
        OrderDetail line = new OrderDetail(orderOfADeletedCustomer(), product, 1, BigDecimal.TEN);
        line.setHowSold(unit);
        when(details.findByIdAndActiveTrue(line.getId())).thenReturn(Optional.of(line));

        OrderDetailDtos.Response saved = orderDetailService().update(line.getId(),
                new OrderDetailDtos.Request(product.getId(), 3, BigDecimal.TEN, unit.getId()));

        assertEquals(3, saved.quantity());
        assertEquals("Case", saved.howSoldName());
        assertFalse(saved.howSoldActive());
    }

    @Test
    void aDeletedUnitIsNotGivenToANewItemByDefault() {
        CustomerOrder order = orderOfADeletedCustomer();
        Inventory stock = inventoryIn(deletedUnit());
        when(products.findByIdAndActiveTrue(product.getId())).thenReturn(Optional.of(product));
        when(inventories.findAllByProductIdAndActiveTrue(product.getId())).thenReturn(List.of(stock));

        OrderDetailDtos.Response saved = orderDetailService().create(order.getId(),
                new OrderDetailDtos.Request(product.getId(), 1, null, null));

        assertNull(saved.howSoldId());
    }

    // ---- units: conversions ------------------------------------------------------------

    @Test
    void aConversionCanKeepItsDeletedUnit() {
        HowSoldConversionRepository conversions = mock(HowSoldConversionRepository.class);
        HowSold parent = deletedUnit();
        HowSold child = new HowSold("Each");
        HowSoldConversion conversion = new HowSoldConversion(parent, child, BigDecimal.valueOf(12));
        when(conversions.findByIdAndActiveTrue(conversion.getId())).thenReturn(Optional.of(conversion));
        HowSoldConversionService service =
                new HowSoldConversionService(conversions, howSolds, products, audit, pagination);

        HowSoldConversionDtos.Response saved = service.update(conversion.getId(), new HowSoldConversionDtos.Request(
                parent.getId(), child.getId(), BigDecimal.valueOf(24), null));

        assertEquals(BigDecimal.valueOf(24), saved.unitConversion());
        assertEquals("Case", saved.parentHowSoldName());
        assertFalse(saved.parentHowSoldActive());
    }
}

package com.example.warehouse_proj.order;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import com.example.warehouse_proj.customer.Customer;
import com.example.warehouse_proj.notification.NotificationService;
import com.example.warehouse_proj.notification.NotificationType;
import com.example.warehouse_proj.orderdetail.OrderDetailRepository;
import com.example.warehouse_proj.shipment.ShipmentRepository;
import com.example.warehouse_proj.customer.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderService {

    private static final String ENTITY = "Order";
    private static final Set<String> SORT_FIELDS = Set.of("totalPrice", "targetDeliveryDate", "status");
    private static final List<String> SEARCH_FIELDS = List.of(
            "id", "customer.firstName", "customer.lastName", "status", "targetDeliveryDate");

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final AuditLogService audit;
    private final PaginationService pagination;
    private final NotificationService notifications;
    private final ShipmentRepository shipments;
    private final OrderDetailRepository orderDetails;

    public OrderService(
            OrderRepository orders,
            CustomerRepository customers,
            AuditLogService audit,
            PaginationService pagination,
            NotificationService notifications,
            ShipmentRepository shipments,
            OrderDetailRepository orderDetails) {
        this.orders = orders;
        this.customers = customers;
        this.audit = audit;
        this.pagination = pagination;
        this.notifications = notifications;
        this.shipments = shipments;
        this.orderDetails = orderDetails;
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.Response> list(UUID customerId) {
        List<CustomerOrder> result = customerId == null
                ? orders.findAllByActiveTrue()
                : orders.findAllByCustomerIdAndActiveTrue(customerId);
        return result.stream().map(OrderDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderDtos.Response> page(PageQuery query) {
        return pagination.page(orders, query, SORT_FIELDS, SEARCH_FIELDS, OrderDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public OrderDtos.Response get(UUID id) {
        return OrderDtos.Response.from(find(id));
    }

    @Transactional
    public OrderDtos.Response create(OrderDtos.Request request) {
        CustomerOrder order = new CustomerOrder(
                findCustomer(request.customerId()),
                request.totalPrice(),
                request.targetDeliveryDate(),
                request.status() != null ? request.status() : OrderStatus.PENDING);
        orders.save(order);
        audit.record(AuditAction.CREATE, ENTITY, order.getId());
        notifications.notifyAllUsers(
                NotificationType.ORDER_CREATED,
                "New order",
                "Order " + shortId(order) + " was created for " + customerName(order) + " ("
                        + order.getStatus() + ")",
                ENTITY,
                order.getId());
        return OrderDtos.Response.from(order);
    }

    @Transactional
    public OrderDtos.Response update(UUID id, OrderDtos.Request request) {
        CustomerOrder order = find(id);
        OrderStatus previousStatus = order.getStatus();
        // Keeping the customer it already has is always fine, even if that customer was deleted since.
        if (!order.getCustomer().getId().equals(request.customerId())) {
            order.setCustomer(findCustomer(request.customerId()));
        }
        order.setTotalPrice(request.totalPrice());
        order.setTargetDeliveryDate(request.targetDeliveryDate());
        if (request.status() != null) {
            order.setStatus(request.status());
        }
        // Flush so @PreUpdate has set updatedAt before the response is built.
        orders.saveAndFlush(order);
        audit.record(AuditAction.UPDATE, ENTITY, order.getId());
        if (order.getStatus() != previousStatus) {
            notifications.notifyAllUsers(
                    NotificationType.ORDER_STATUS_CHANGED,
                    "Order status changed",
                    "Order " + shortId(order) + " for " + customerName(order) + " went from "
                            + previousStatus + " to " + order.getStatus(),
                    ENTITY,
                    order.getId());
        }
        return OrderDtos.Response.from(order);
    }

    /** Soft delete; refused while the order still has active shipments. */
    @Transactional
    public void delete(UUID id) {
        CustomerOrder order = find(id);
        if (shipments.existsByCustomerOrderIdAndActiveTrue(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Order still has shipments");
        }
        // The order's lines have no meaning without it, so they are soft-deleted along with it.
        orderDetails.findAllByCustomerOrderIdAndActiveTrue(id).forEach(line -> line.setActive(false));
        order.setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private static String shortId(CustomerOrder order) {
        return order.getId().toString().substring(0, 8);
    }

    private static String customerName(CustomerOrder order) {
        return order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName();
    }

    private CustomerOrder find(UUID id) {
        return orders.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private Customer findCustomer(UUID id) {
        return customers.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}

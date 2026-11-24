package com.example.warehouse_proj.report;

import com.example.warehouse_proj.customer.CustomerDtos;
import com.example.warehouse_proj.customer.CustomerService;
import com.example.warehouse_proj.order.OrderService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class OrdersPdfReport implements PdfReport {

    private final OrderService orders;
    private final CustomerService customers;

    public OrdersPdfReport(OrderService orders, CustomerService customers) {
        this.orders = orders;
        this.customers = customers;
    }

    @Override
    public String type() {
        return "orders";
    }

    @Override
    public String title() {
        return "Orders";
    }

    @Override
    public List<String> headers() {
        return List.of("Order", "Customer", "Total price", "Target delivery", "Status");
    }

    @Override
    public List<List<String>> rows() {
        Map<UUID, String> customerNames = customers.list().stream()
                .collect(Collectors.toMap(
                        CustomerDtos.Response::id, c -> c.firstName() + " " + c.lastName(), (a, b) -> a));
        return orders.list(null).stream()
                .map(o -> List.of(
                        o.id().toString(),
                        customerNames.getOrDefault(o.customerId(), o.customerId().toString()),
                        o.totalPrice().toPlainString(),
                        o.targetDeliveryDate().toString(),
                        o.status().name()))
                .toList();
    }
}

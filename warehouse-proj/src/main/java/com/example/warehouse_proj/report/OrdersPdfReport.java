package com.example.warehouse_proj.report;

import com.example.warehouse_proj.order.OrderService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class OrdersPdfReport implements PdfReport {

    private final OrderService orders;

    public OrdersPdfReport(OrderService orders) {
        this.orders = orders;
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
        return List.of("Order", "Customer", "Total price (PHP)", "Target delivery", "Status");
    }

    @Override
    public List<List<String>> rows() {
        return orders.list(null).stream()
                .map(o -> List.of(
                        o.id().toString(),
                        o.customerActive() ? o.customerName() : o.customerName() + " (deleted)",
                        String.format(Locale.US, "%,.2f", o.totalPrice()),
                        o.targetDeliveryDate().toString(),
                        o.status().name()))
                .toList();
    }
}

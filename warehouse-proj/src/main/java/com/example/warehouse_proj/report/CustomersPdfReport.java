package com.example.warehouse_proj.report;

import com.example.warehouse_proj.customer.CustomerService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CustomersPdfReport implements PdfReport {

    private final CustomerService customers;

    public CustomersPdfReport(CustomerService customers) {
        this.customers = customers;
    }

    @Override
    public String type() {
        return "customers";
    }

    @Override
    public String title() {
        return "Customers";
    }

    @Override
    public List<String> headers() {
        return List.of("First name", "Last name", "Email", "Phone", "Address");
    }

    @Override
    public List<List<String>> rows() {
        return customers.list().stream()
                .map(c -> List.of(c.firstName(), c.lastName(), c.email(), c.phoneNumber(), c.address()))
                .toList();
    }
}

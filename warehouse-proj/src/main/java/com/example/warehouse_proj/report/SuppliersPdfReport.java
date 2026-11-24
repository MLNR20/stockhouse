package com.example.warehouse_proj.report;

import com.example.warehouse_proj.supplier.SupplierService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SuppliersPdfReport implements PdfReport {

    private final SupplierService suppliers;

    public SuppliersPdfReport(SupplierService suppliers) {
        this.suppliers = suppliers;
    }

    @Override
    public String type() {
        return "suppliers";
    }

    @Override
    public String title() {
        return "Suppliers";
    }

    @Override
    public List<String> headers() {
        return List.of("Supplier", "Phone", "Email", "Products");
    }

    @Override
    public List<List<String>> rows() {
        return suppliers.list().stream()
                .map(s -> List.of(s.supplierName(), s.phoneNumber(), s.email(), String.valueOf(s.productIds().size())))
                .toList();
    }
}

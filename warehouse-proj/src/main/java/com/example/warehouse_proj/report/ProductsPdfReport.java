package com.example.warehouse_proj.report;

import com.example.warehouse_proj.product.ProductService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductsPdfReport implements PdfReport {

    private final ProductService products;

    public ProductsPdfReport(ProductService products) {
        this.products = products;
    }

    @Override
    public String type() {
        return "products";
    }

    @Override
    public String title() {
        return "Products";
    }

    @Override
    public List<String> headers() {
        return List.of("Name", "Base price", "Suppliers");
    }

    @Override
    public List<List<String>> rows() {
        return products.list(null).stream()
                .map(p -> List.of(p.name(), p.basePrice().toPlainString(), String.valueOf(p.supplierIds().size())))
                .toList();
    }
}

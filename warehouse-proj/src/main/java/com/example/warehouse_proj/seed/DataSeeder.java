package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.aisle.Aisle;
import com.example.warehouse_proj.aisle.AisleRepository;
import com.example.warehouse_proj.category.Category;
import com.example.warehouse_proj.category.CategoryRepository;
import com.example.warehouse_proj.container.Container;
import com.example.warehouse_proj.container.ContainerProduct;
import com.example.warehouse_proj.container.ContainerProductRepository;
import com.example.warehouse_proj.container.ContainerRepository;
import com.example.warehouse_proj.customer.Customer;
import com.example.warehouse_proj.customer.CustomerRepository;
import com.example.warehouse_proj.howsold.HowSold;
import com.example.warehouse_proj.howsold.HowSoldConversion;
import com.example.warehouse_proj.howsold.HowSoldConversionRepository;
import com.example.warehouse_proj.howsold.HowSoldRepository;
import com.example.warehouse_proj.inventory.Inventory;
import com.example.warehouse_proj.inventory.InventoryRepository;
import com.example.warehouse_proj.inventory.InventoryStatus;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.product.Product;
import com.example.warehouse_proj.product.ProductRepository;
import com.example.warehouse_proj.shelf.Shelf;
import com.example.warehouse_proj.shelf.ShelfRepository;
import com.example.warehouse_proj.shelfrow.ShelfRow;
import com.example.warehouse_proj.shelfrow.ShelfRowRepository;
import com.example.warehouse_proj.supplier.Supplier;
import com.example.warehouse_proj.supplier.SupplierProduct;
import com.example.warehouse_proj.supplier.SupplierProductRepository;
import com.example.warehouse_proj.supplier.SupplierRepository;
import com.example.warehouse_proj.warehouse.Warehouse;
import com.example.warehouse_proj.warehouse.WarehouseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Fills an empty database with realistic, linked sample data on startup so every screen and
 * endpoint has something to show. It does nothing once any product exists, so it runs once.
 * Turn it off with {@code app.seed.enabled=false} (env SEED_DATA=false).
 *
 * <p>Writes through the repositories, so no audit rows or notifications are created, and it adds no
 * users: register through /api/auth/register.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final TransactionTemplate tx;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final SupplierProductRepository supplierProducts;
    private final HowSoldRepository howSolds;
    private final HowSoldConversionRepository conversions;
    private final WarehouseRepository warehouses;
    private final AisleRepository aisles;
    private final ShelfRepository shelves;
    private final ShelfRowRepository rows;
    private final ContainerRepository containers;
    private final ContainerProductRepository containerProducts;
    private final InventoryRepository inventories;
    private final CustomerRepository customers;
    private final OrderRepository orders;

    public DataSeeder(
            TransactionTemplate tx,
            CategoryRepository categories,
            ProductRepository products,
            SupplierRepository suppliers,
            SupplierProductRepository supplierProducts,
            HowSoldRepository howSolds,
            HowSoldConversionRepository conversions,
            WarehouseRepository warehouses,
            AisleRepository aisles,
            ShelfRepository shelves,
            ShelfRowRepository rows,
            ContainerRepository containers,
            ContainerProductRepository containerProducts,
            InventoryRepository inventories,
            CustomerRepository customers,
            OrderRepository orders) {
        this.tx = tx;
        this.categories = categories;
        this.products = products;
        this.suppliers = suppliers;
        this.supplierProducts = supplierProducts;
        this.howSolds = howSolds;
        this.conversions = conversions;
        this.warehouses = warehouses;
        this.aisles = aisles;
        this.shelves = shelves;
        this.rows = rows;
        this.containers = containers;
        this.containerProducts = containerProducts;
        this.inventories = inventories;
        this.customers = customers;
        this.orders = orders;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            if (products.count() > 0) {
                log.info("Seed skipped: the database already has products");
                return;
            }
            seed();
        });
    }

    private void seed() {
        Random random = new Random(42); // fixed seed: the same data every time

        // ---- categories and products -------------------------------------------------------
        List<Category> cats = categories.saveAll(List.of(
                new Category("Electronics", "Devices, cables and accessories"),
                new Category("Groceries", "Packaged food and drink"),
                new Category("Hardware", "Tools and fasteners"),
                new Category("Apparel", "Clothing and workwear"),
                new Category("Office Supplies", "Paper, pens and desk items")));

        String[][] productRows = {
                // name, price, category index
                {"USB-C Charging Cable 1m", "9.99", "0"},
                {"Wireless Mouse", "24.50", "0"},
                {"Bluetooth Speaker", "59.00", "0"},
                {"LED Desk Lamp", "32.75", "0"},
                {"Organic Pasta 500g", "2.49", "1"},
                {"Olive Oil 1L", "8.90", "1"},
                {"Sparkling Water 12-pack", "6.40", "1"},
                {"Cordless Drill 18V", "89.00", "2"},
                {"Hex Bolt Set (200 pcs)", "14.25", "2"},
                {"Claw Hammer", "16.80", "2"},
                {"Work Gloves (pair)", "7.95", "3"},
                {"Hi-Vis Vest", "11.30", "3"},
                {"A4 Copy Paper (500 sheets)", "5.60", "4"},
                {"Ballpoint Pens (box of 50)", "12.00", "4"},
        };
        List<Product> prods = new ArrayList<>();
        for (String[] r : productRows) {
            Product p = new Product(r[0], new BigDecimal(r[1]));
            p.setCategory(cats.get(Integer.parseInt(r[2])));
            prods.add(p);
        }
        prods = products.saveAll(prods);

        // ---- suppliers and which products they supply --------------------------------------
        List<Supplier> sups = suppliers.saveAll(List.of(
                new Supplier("Northwind Trading", "+1-555-0101", "orders@northwind.example"),
                new Supplier("Globex Wholesale", "+1-555-0102", "sales@globex.example"),
                new Supplier("Initech Supply Co.", "+1-555-0103", "supply@initech.example"),
                new Supplier("Umbrella Distribution", "+1-555-0104", "dispatch@umbrella.example")));
        List<SupplierProduct> links = new ArrayList<>();
        for (int i = 0; i < prods.size(); i++) {
            links.add(new SupplierProduct(sups.get(i % sups.size()), prods.get(i)));
            if (i % 3 == 0) { // some products have a second supplier
                links.add(new SupplierProduct(sups.get((i + 1) % sups.size()), prods.get(i)));
            }
        }
        supplierProducts.saveAll(links);

        // ---- units of sale and conversions: 1 pallet = 40 cases = 160 packs = 960 each ---------
        HowSold each = new HowSold("Each");
        HowSold pack = new HowSold("Pack");
        HowSold cas = new HowSold("Case");
        HowSold pallet = new HowSold("Pallet");
        howSolds.saveAll(List.of(each, pack, cas, pallet));
        conversions.saveAll(List.of(
                new HowSoldConversion(pack, each, new BigDecimal("6")),
                new HowSoldConversion(cas, pack, new BigDecimal("4")),
                new HowSoldConversion(pallet, cas, new BigDecimal("40"))));

        // ---- warehouse > aisle > shelf > row > container, with products inside ---------------
        String[] warehouseNames = {"Main Distribution Center", "East Coast Warehouse"};
        int[] aislesPerWarehouse = {3, 2};
        List<Container> allContainers = new ArrayList<>();
        for (int w = 0; w < warehouseNames.length; w++) {
            Warehouse warehouse = warehouses.save(new Warehouse(warehouseNames[w]));
            for (int a = 1; a <= aislesPerWarehouse[w]; a++) {
                Aisle aisle = aisles.save(new Aisle(warehouse, "Aisle " + a));
                for (int s = 1; s <= 2; s++) {
                    Shelf shelf = shelves.save(new Shelf(aisle, "Shelf " + s));
                    for (int r = 1; r <= 2; r++) {
                        ShelfRow row = rows.save(new ShelfRow(shelf, "Row " + r));
                        for (int c = 1; c <= 2; c++) {
                            String code = "W%d-A%d-S%d-R%d-C%d".formatted(w + 1, a, s, r, c);
                            allContainers.add(new Container(row, code));
                        }
                    }
                }
            }
        }
        allContainers = containers.saveAll(allContainers);

        List<ContainerProduct> contents = new ArrayList<>();
        for (Container container : allContainers) {
            int distinct = 2 + random.nextInt(2); // 2 or 3 different products per container
            List<Product> pool = new ArrayList<>(prods);
            for (int i = 0; i < distinct; i++) {
                Product p = pool.remove(random.nextInt(pool.size()));
                contents.add(new ContainerProduct(container, p, 5 + random.nextInt(196)));
            }
        }
        containerProducts.saveAll(contents);

        // ---- inventory status per product ----------------------------------------------------
        InventoryStatus[] pattern = {
                InventoryStatus.IN_STOCK, InventoryStatus.IN_STOCK, InventoryStatus.IN_STOCK,
                InventoryStatus.LOW_STOCK, InventoryStatus.IN_STOCK, InventoryStatus.OUT_OF_STOCK,
                InventoryStatus.IN_STOCK, InventoryStatus.LOW_STOCK};
        List<Inventory> stock = new ArrayList<>();
        for (int i = 0; i < prods.size(); i++) {
            stock.add(new Inventory(prods.get(i), pattern[i % pattern.length]));
        }
        stock.get(prods.size() - 1).setStatus(InventoryStatus.DISCONTINUED);
        inventories.saveAll(stock);

        // ---- customers and orders ------------------------------------------------------------
        String[][] customerRows = {
                {"Ava", "Johnson", "ava.johnson@example.com", "+1-555-0201", "12 Maple Street, Springfield"},
                {"Liam", "Garcia", "liam.garcia@example.com", "+1-555-0202", "48 Oak Avenue, Riverton"},
                {"Noah", "Patel", "noah.patel@example.com", "+1-555-0203", "7 Harbor Road, Lakeside"},
                {"Emma", "Nguyen", "emma.nguyen@example.com", "+1-555-0204", "301 Pine Court, Fairview"},
                {"Olivia", "Brown", "olivia.brown@example.com", "+1-555-0205", "95 Cedar Lane, Georgetown"},
                {"Lucas", "Martin", "lucas.martin@example.com", "+1-555-0206", "22 Birch Boulevard, Salem"},
        };
        List<Customer> custs = new ArrayList<>();
        for (String[] r : customerRows) {
            custs.add(new Customer(r[0], r[1], r[2], r[3], r[4]));
        }
        custs = customers.saveAll(custs);

        OrderStatus[] statuses = OrderStatus.values();
        List<CustomerOrder> orderList = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            BigDecimal total = BigDecimal.valueOf(50 + random.nextDouble() * 4950).setScale(2, RoundingMode.HALF_UP);
            OrderStatus status = statuses[i % statuses.length];
            // delivered orders are in the past, the rest are still ahead
            LocalDate delivery = status == OrderStatus.DELIVERED
                    ? LocalDate.now().minusDays(1 + random.nextInt(14))
                    : LocalDate.now().plusDays(1 + random.nextInt(21));
            orderList.add(new CustomerOrder(custs.get(i % custs.size()), total, delivery, status));
        }
        orders.saveAll(orderList);

        log.info("Seeded sample data: {} categories, {} products, {} suppliers, {} containers, {} customers, {} orders",
                cats.size(), prods.size(), sups.size(), allContainers.size(), custs.size(), orderList.size());
    }
}

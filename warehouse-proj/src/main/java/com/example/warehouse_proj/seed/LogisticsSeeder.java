package com.example.warehouse_proj.seed;

import com.example.warehouse_proj.employee.WarehouseEmployee;
import com.example.warehouse_proj.employee.WarehouseEmployeeRepository;
import com.example.warehouse_proj.order.CustomerOrder;
import com.example.warehouse_proj.order.OrderRepository;
import com.example.warehouse_proj.order.OrderStatus;
import com.example.warehouse_proj.shipment.Shipment;
import com.example.warehouse_proj.shipment.ShipmentRepository;
import com.example.warehouse_proj.shipment.ShipmentStatus;
import com.example.warehouse_proj.truck.Truck;
import com.example.warehouse_proj.truck.TruckRepository;
import com.example.warehouse_proj.truck.TruckStatus;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Sample trucks, warehouse employees and shipments. Runs after DataSeeder (it needs the warehouses
 * and orders that one creates) and only when there are no trucks yet, so it also fills a database
 * that was seeded before trucks existed. Turn off with app.seed.enabled=false.
 */
@Component
@Order(2)
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class LogisticsSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LogisticsSeeder.class);

    private final TransactionTemplate tx;
    private final TruckRepository trucks;
    private final WarehouseEmployeeRepository employees;
    private final ShipmentRepository shipments;
    private final WarehouseRepository warehouses;
    private final OrderRepository orders;

    public LogisticsSeeder(
            TransactionTemplate tx,
            TruckRepository trucks,
            WarehouseEmployeeRepository employees,
            ShipmentRepository shipments,
            WarehouseRepository warehouses,
            OrderRepository orders) {
        this.tx = tx;
        this.trucks = trucks;
        this.employees = employees;
        this.shipments = shipments;
        this.warehouses = warehouses;
        this.orders = orders;
    }

    @Override
    public void run(ApplicationArguments args) {
        tx.executeWithoutResult(status -> {
            if (trucks.count() > 0) {
                log.info("Logistics seed skipped: the database already has trucks");
                return;
            }
            List<Warehouse> warehouseList = warehouses.findAllByActiveTrue();
            List<CustomerOrder> orderList = orders.findAllByActiveTrue();
            if (warehouseList.isEmpty() || orderList.isEmpty()) {
                log.info("Logistics seed skipped: no warehouses or orders to attach to");
                return;
            }
            seed(warehouseList, orderList);
        });
    }

    private void seed(List<Warehouse> warehouseList, List<CustomerOrder> orderList) {
        // ---- trucks ------------------------------------------------------------------------
        List<Truck> truckList = trucks.saveAll(List.of(
                new Truck("TRK-1001", "Volvo FH16", new BigDecimal("18000.00"), TruckStatus.AVAILABLE),
                new Truck("TRK-1002", "Mercedes Actros", new BigDecimal("16500.00"), TruckStatus.IN_TRANSIT),
                new Truck("TRK-1003", "Isuzu NPR", new BigDecimal("5200.00"), TruckStatus.AVAILABLE),
                new Truck("TRK-1004", "Ford Transit", new BigDecimal("1400.00"), TruckStatus.MAINTENANCE)));
        // Trucks in maintenance cannot take shipments.
        List<Truck> drivable = truckList.stream().filter(t -> t.getStatus() != TruckStatus.MAINTENANCE).toList();

        // ---- employees, spread across the warehouses ---------------------------------------
        String[][] people = {
                {"Marcus", "Reyes", "marcus.reyes@warehouse.example", "+1-555-0301"},
                {"Priya", "Shah", "priya.shah@warehouse.example", "+1-555-0302"},
                {"Tomas", "Novak", "tomas.novak@warehouse.example", "+1-555-0303"},
                {"Grace", "Okafor", "grace.okafor@warehouse.example", "+1-555-0304"},
                {"Daniel", "Kim", "daniel.kim@warehouse.example", "+1-555-0305"},
                {"Sofia", "Rossi", "sofia.rossi@warehouse.example", "+1-555-0306"},
        };
        List<WarehouseEmployee> staff = new ArrayList<>();
        for (int i = 0; i < people.length; i++) {
            String[] p = people[i];
            staff.add(new WarehouseEmployee(warehouseList.get(i % warehouseList.size()), p[0], p[1], p[2], p[3]));
        }
        staff = employees.saveAll(staff);

        // ---- shipments for orders that are confirmed, shipped or delivered ------------------
        List<Shipment> shipmentList = new ArrayList<>();
        int next = 0;
        boolean splitDone = false;
        for (CustomerOrder order : orderList) {
            ShipmentStatus shipmentStatus = switch (order.getStatus()) {
                case CONFIRMED -> ShipmentStatus.SCHEDULED;
                case SHIPPED -> ShipmentStatus.IN_TRANSIT;
                case DELIVERED -> ShipmentStatus.DELIVERED;
                case PENDING, CANCELLED -> null;
            };
            if (shipmentStatus == null) {
                continue;
            }
            Truck truck = drivable.get(next % drivable.size());
            WarehouseEmployee handler = staff.get(next % staff.size());
            next++;
            shipmentList.add(new Shipment(order, truck, handler, order.getTargetDeliveryDate().minusDays(1), shipmentStatus));

            // One order shipped in two parts, to show an order with many shipments.
            if (!splitDone && order.getStatus() == OrderStatus.SHIPPED) {
                splitDone = true;
                shipmentList.add(new Shipment(
                        order,
                        drivable.get(next % drivable.size()),
                        null, // not assigned to anyone yet
                        order.getTargetDeliveryDate(),
                        ShipmentStatus.SCHEDULED));
            }
        }
        shipments.saveAll(shipmentList);

        log.info("Seeded logistics data: {} trucks, {} employees, {} shipments",
                truckList.size(), staff.size(), shipmentList.size());
    }
}

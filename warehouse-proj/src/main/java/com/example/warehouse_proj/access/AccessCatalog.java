package com.example.warehouse_proj.access;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything that can be granted: the permissions (resource + action) and the pages. The admin screens
 * read this list from {@code GET /api/access/catalog}, and role definitions are validated against it.
 */
public final class AccessCatalog {

    public static final List<String> ACTIONS = List.of("view", "create", "update", "delete");

    /** Resources that a permission is about, in display order. */
    public static final List<String> RESOURCES = List.of(
            "dashboard", "inventory", "units", "orders", "suppliers", "products", "aisles", "trucks",
            "shipments", "employees", "reorders", "history", "categories", "warehouses", "shelves", "customers", "users", "roles",
            "pages");

    public record Page(String path, String label, String section) {
    }

    /** Every page in the app, in sidebar order. */
    public static final List<Page> PAGES = List.of(
            new Page("/", "Dashboard", "Overview"),
            new Page("/history", "History", "Overview"),
            new Page("/analytics", "Analytics", "Overview"),
            new Page("/reports", "Reports", "Overview"),
            new Page("/products", "Products", "Catalog"),
            new Page("/categories", "Categories", "Catalog"),
            new Page("/units", "Units of Measure (How Sold)", "Catalog"),
            new Page("/conversions", "Unit Conversions", "Catalog"),
            new Page("/inventory", "Inventory", "Warehouse"),
            new Page("/reorders", "Reorders", "Warehouse"),
            new Page("/employees", "Employees", "Warehouse"),
            new Page("/warehouses", "Warehouses", "Storage Layout"),
            new Page("/aisles", "Aisles", "Storage Layout"),
            new Page("/shelves", "Shelves", "Storage Layout"),
            new Page("/rows", "Shelf Rows", "Storage Layout"),
            new Page("/containers", "Containers", "Storage Layout"),
            new Page("/orders", "Orders", "Sales & Supply"),
            new Page("/customers", "Customers", "Sales & Supply"),
            new Page("/shipments", "Shipments", "Sales & Supply"),
            new Page("/trucks", "Trucks", "Sales & Supply"),
            new Page("/suppliers", "Suppliers", "Sales & Supply"),
            new Page("/access/users", "Users", "Access Control"),
            new Page("/access/roles", "Roles & Permissions", "Access Control"),
            new Page("/access/pages", "Page Assignment", "Access Control"));

    public static final String ADMIN = "admin";
    public static final String MANAGER = "manager";
    public static final String STAFF = "staff";
    public static final String VIEWER = "viewer";

    /** Role given to people who sign themselves up. */
    public static final String DEFAULT_ROLE = VIEWER;

    private AccessCatalog() {
    }

    public static boolean isValidPermission(String permission) {
        int colon = permission.indexOf(':');
        return colon > 0
                && RESOURCES.contains(permission.substring(0, colon))
                && ACTIONS.contains(permission.substring(colon + 1));
    }

    public static boolean isValidPage(String path) {
        return PAGES.stream().anyMatch(p -> p.path().equals(path));
    }

    public static Set<String> allPermissions() {
        return grant(RESOURCES, ACTIONS);
    }

    public static Set<String> allPagePaths() {
        Set<String> paths = new LinkedHashSet<>();
        PAGES.forEach(p -> paths.add(p.path()));
        return paths;
    }

    private static Set<String> grant(List<String> resources, List<String> actions) {
        Set<String> out = new LinkedHashSet<>();
        for (String r : resources) {
            for (String a : actions) {
                out.add(r + ":" + a);
            }
        }
        return out;
    }

    /** Definition of one of the standard roles. */
    public record RoleDefinition(String name, String description, Set<String> permissions, Set<String> pages) {
    }

    private static final List<String> OPERATIONS = List.of(
            "dashboard", "inventory", "units", "orders", "suppliers", "products", "aisles", "trucks",
            "shipments", "employees", "reorders", "history", "categories", "warehouses", "shelves", "customers");

    /** The standard roles, created on first start. */
    public static List<RoleDefinition> standardRoles() {
        Map<String, RoleDefinition> roles = new LinkedHashMap<>();

        roles.put(ADMIN, new RoleDefinition(ADMIN, "Full access, including users, roles and page assignment",
                allPermissions(), allPagePaths()));

        Set<String> manager = new LinkedHashSet<>(grant(OPERATIONS, List.of("view")));
        manager.addAll(grant(
                List.of("inventory", "units", "orders", "suppliers", "products", "categories", "warehouses", "aisles",
                        "shelves", "trucks", "shipments", "employees", "reorders", "customers"),
                List.of("create", "update")));
        manager.addAll(grant(List.of("inventory", "orders"), List.of("delete")));
        roles.put(MANAGER, new RoleDefinition(MANAGER, "Runs day-to-day operations", manager,
                pagesWithout("/access/")));

        // Staff and viewers cannot see the employee list or the user history.
        List<String> floor = List.of("dashboard", "inventory", "units", "orders", "products", "categories", "warehouses",
                "aisles", "shelves", "trucks", "shipments", "reorders", "customers");
        Set<String> staff = new LinkedHashSet<>(grant(floor, List.of("view")));
        staff.addAll(grant(List.of("inventory", "orders", "shipments", "reorders", "customers"), List.of("create", "update")));
        roles.put(STAFF, new RoleDefinition(STAFF, "Handles stock, orders and shipments", staff,
                pagesWithout("/access/", "/employees", "/history", "/suppliers", "/reports")));

        roles.put(VIEWER, new RoleDefinition(VIEWER, "Read-only access", grant(floor, List.of("view")),
                pagesWithout("/access/", "/employees", "/history", "/suppliers", "/reports")));

        return new ArrayList<>(roles.values());
    }

    private static Set<String> pagesWithout(String... excludedPrefixes) {
        Set<String> out = new LinkedHashSet<>();
        for (Page page : PAGES) {
            boolean excluded = false;
            for (String prefix : excludedPrefixes) {
                if (page.path().startsWith(prefix)) {
                    excluded = true;
                    break;
                }
            }
            if (!excluded) {
                out.add(page.path());
            }
        }
        return out;
    }
}

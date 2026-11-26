package com.example.warehouse_proj.auth;

import com.example.warehouse_proj.access.AccessCatalog;
import com.example.warehouse_proj.access.AccessCatalog.RoleDefinition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockServletContext;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

/**
 * The real permission table (ApiPermissions) in front of a stand-in controller that answers 200 to everything,
 * checked with the permissions the standard roles actually hold. No database needed.
 */
class ApiPermissionsTests {

    @Configuration
    @EnableWebSecurity
    @EnableWebMvc
    static class TestApp {

        @Bean
        SecurityFilterChain chain(HttpSecurity http) throws Exception {
            return http.csrf(c -> c.disable())
                    .authorizeHttpRequests(ApiPermissions::apply)
                    .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                    .build();
        }

        @Bean
        Anything anything() {
            return new Anything();
        }
    }

    @RestController
    static class Anything {

        @RequestMapping("/**")
        String ok() {
            return "ok";
        }
    }

    private static final String ID = UUID.randomUUID().toString();

    /** Every resource collection with a normal create/update/delete. */
    private static final List<String> COLLECTIONS = List.of(
            "aisles", "categories", "containers", "customers", "how-sold", "how-sold-conversions", "inventory",
            "orders", "products", "reorders", "rows", "shelves", "shipments", "suppliers", "trucks",
            "warehouse-employees", "warehouses");

    private static AnnotationConfigWebApplicationContext context;
    private static MockMvc mvc;

    @BeforeAll
    static void start() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestApp.class);
        context.refresh();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @AfterAll
    static void stop() {
        context.close();
    }

    private static int status(String role, HttpMethod method, String path) throws Exception {
        var call = request(method, path);
        if (role != null) {
            RoleDefinition definition = AccessCatalog.standardRoles().stream()
                    .filter(r -> r.name().equals(role)).findFirst().orElseThrow();
            call = call.with(user("someone").authorities(
                    definition.permissions().stream().map(SimpleGrantedAuthority::new).toList()));
        }
        return mvc.perform(call).andReturn().getResponse().getStatus();
    }

    @Test
    void signedOutCallersGet401() throws Exception {
        for (String collection : COLLECTIONS) {
            assertEquals(401, status(null, HttpMethod.GET, "/api/" + collection), collection);
            assertEquals(401, status(null, HttpMethod.POST, "/api/" + collection), collection);
        }
    }

    @Test
    void signInAndPasswordResetNeedNoSession() throws Exception {
        for (String path : List.of("login", "refresh", "forgot-password", "reset-password")) {
            assertEquals(200, status(null, HttpMethod.POST, "/api/auth/" + path), path);
        }
        assertEquals(401, status(null, HttpMethod.GET, "/api/auth/me"));
    }

    @Test
    void viewersCannotChangeAnything() throws Exception {
        for (String collection : COLLECTIONS) {
            String base = "/api/" + collection;
            assertEquals(403, status("viewer", HttpMethod.POST, base), "POST " + base);
            assertEquals(403, status("viewer", HttpMethod.PUT, base + "/" + ID), "PUT " + base);
            assertEquals(403, status("viewer", HttpMethod.DELETE, base + "/" + ID), "DELETE " + base);
        }
        assertEquals(403, status("viewer", HttpMethod.POST, "/api/orders/" + ID + "/details"));
        assertEquals(403, status("viewer", HttpMethod.PUT, "/api/order-details/" + ID));
        assertEquals(403, status("viewer", HttpMethod.PUT, "/api/suppliers/" + ID + "/products/" + ID));
        assertEquals(403, status("viewer", HttpMethod.DELETE, "/api/containers/" + ID + "/products/" + ID));
    }

    @Test
    void viewersCannotReadTheAuditLogOrSupplierReport() throws Exception {
        assertEquals(403, status("viewer", HttpMethod.GET, "/api/audit-logs"));
        assertEquals(403, status("viewer", HttpMethod.GET, "/api/reports/suppliers.pdf"));
        assertEquals(200, status("manager", HttpMethod.GET, "/api/audit-logs"));
    }

    @Test
    void staffWorkOnOrdersButCannotDeleteThemOrEditProducts() throws Exception {
        assertEquals(200, status("staff", HttpMethod.POST, "/api/orders"));
        assertEquals(200, status("staff", HttpMethod.POST, "/api/orders/" + ID + "/details"));
        assertEquals(403, status("staff", HttpMethod.DELETE, "/api/orders/" + ID));
        assertEquals(403, status("staff", HttpMethod.POST, "/api/products"));
    }

    @Test
    void adminMayDoEverythingListed() throws Exception {
        for (String collection : COLLECTIONS) {
            String base = "/api/" + collection;
            assertEquals(200, status("admin", HttpMethod.GET, base + "/page"), base);
            assertEquals(200, status("admin", HttpMethod.POST, base), base);
            assertEquals(200, status("admin", HttpMethod.DELETE, base + "/" + ID), base);
        }
    }

    @Test
    void routesWithoutARuleAreRefusedEvenForAdmins() throws Exception {
        assertEquals(403, status("admin", HttpMethod.GET, "/api/something-new"));
        assertEquals(403, status("admin", HttpMethod.PATCH, "/api/products/" + ID));
    }

    /** What each screen loads. A role that is given a page must be able to read all of it. */
    @Test
    void everyAssignedPageCanLoadItsData() throws Exception {
        Map<String, List<String>> reads = Map.ofEntries(
                Map.entry("/", List.of("analytics/summary")),
                Map.entry("/history", List.of("audit-logs")),
                Map.entry("/products", List.of("products/page", "categories")),
                Map.entry("/categories", List.of("categories/page")),
                Map.entry("/units", List.of("how-sold/page")),
                Map.entry("/conversions", List.of("how-sold-conversions/page", "how-sold", "products")),
                Map.entry("/inventory", List.of("inventory/page", "products", "how-sold")),
                Map.entry("/reorders", List.of("reorders/page", "suppliers", "products", "how-sold", "inventory")),
                Map.entry("/employees", List.of("warehouse-employees/page", "warehouses")),
                Map.entry("/warehouses", List.of("warehouses/page")),
                Map.entry("/aisles", List.of("aisles/page", "warehouses")),
                Map.entry("/shelves", List.of("shelves/page", "aisles", "warehouses")),
                Map.entry("/rows", List.of("rows/page", "shelves", "aisles", "warehouses")),
                Map.entry("/containers", List.of("containers/page", "rows", "shelves", "aisles", "products")),
                Map.entry("/orders", List.of("orders/page", "customers", "products", "how-sold")),
                Map.entry("/customers", List.of("customers/page")),
                Map.entry("/shipments",
                        List.of("shipments/page", "orders", "customers", "trucks", "warehouse-employees")),
                Map.entry("/trucks", List.of("trucks/page")),
                Map.entry("/suppliers", List.of("suppliers/page", "products")),
                Map.entry("/reports", List.of("reports", "reports/orders.pdf", "reports/products.pdf")));

        for (RoleDefinition role : AccessCatalog.standardRoles()) {
            for (String page : role.pages()) {
                for (String path : reads.getOrDefault(page, List.of())) {
                    assertEquals(200, status(role.name(), HttpMethod.GET, "/api/" + path),
                            role.name() + " on " + page + " reading " + path);
                }
            }
        }
    }
}

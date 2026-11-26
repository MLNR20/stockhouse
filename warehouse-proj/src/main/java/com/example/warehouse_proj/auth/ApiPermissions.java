package com.example.warehouse_proj.auth;

import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AuthorizeHttpRequestsConfigurer;

import java.util.Arrays;

/**
 * Who may call what: every API route and the permission it needs, in one place. Anything not listed here is
 * refused, so a new endpoint stays closed until a rule is added for it.
 *
 * <p>Writing always needs the resource's own permission (POST = create, PUT = update, DELETE = delete). Reading
 * is allowed with the "view" permission of any area whose screens need the data: the Orders page, for example,
 * lists customers and products to name them, so orders:view may read both.
 */
public final class ApiPermissions {

    private ApiPermissions() {
    }

    public static void apply(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth) {
        auth
                // Signing in and out
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/forgot-password", "/api/auth/reset-password").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                // Error dispatches (409, 400, ...) re-enter the chain; without this they all turn into 401.
                .requestMatchers("/error").permitAll()
                // WebSocket handshake carries no Authorization header from browsers; the JWT is
                // checked on the STOMP CONNECT frame instead (StompAuthChannelInterceptor).
                .requestMatchers("/ws/**").permitAll()
                // Everyone has their own notifications (the service only returns the caller's).
                .requestMatchers("/api/notifications/**").authenticated()
                // Roles, page assignment and users check their permission on each method (@PreAuthorize).
                .requestMatchers("/api/access/**", "/api/users/**").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/analytics/**").hasAuthority("dashboard:view")
                .requestMatchers(HttpMethod.GET, "/api/audit-logs/**").hasAuthority("history:view")

                // PDF reports follow the permission of the data they print.
                .requestMatchers(HttpMethod.GET, "/api/reports/orders.pdf", "/api/reports/customers.pdf")
                .hasAnyAuthority("orders:view", "customers:view")
                .requestMatchers(HttpMethod.GET, "/api/reports/products.pdf").hasAuthority("products:view")
                .requestMatchers(HttpMethod.GET, "/api/reports/suppliers.pdf").hasAuthority("suppliers:view")
                .requestMatchers(HttpMethod.GET, "/api/reports")
                .hasAnyAuthority("orders:view", "customers:view", "products:view", "suppliers:view")

                // Routes that change something belonging to a parent record (must come before the general rules).
                .requestMatchers(HttpMethod.POST, "/api/orders/*/details").hasAuthority("orders:update")
                .requestMatchers(HttpMethod.GET, "/api/order-details/**").hasAuthority("orders:view")
                .requestMatchers(HttpMethod.PUT, "/api/order-details/**").hasAuthority("orders:update")
                .requestMatchers(HttpMethod.DELETE, "/api/order-details/**").hasAuthority("orders:update")
                .requestMatchers(HttpMethod.PUT, "/api/suppliers/*/products/*").hasAuthority("suppliers:update")
                .requestMatchers(HttpMethod.DELETE, "/api/suppliers/*/products/*").hasAuthority("suppliers:update")
                .requestMatchers(HttpMethod.PUT, "/api/containers/*/products/*").hasAuthority("shelves:update")
                .requestMatchers(HttpMethod.DELETE, "/api/containers/*/products/*").hasAuthority("shelves:update");

        resource(auth, "/api/aisles/**", "aisles", "aisles", "shelves");
        resource(auth, "/api/categories/**", "categories", "categories", "products");
        resource(auth, "/api/containers/**", "shelves", "shelves");
        resource(auth, "/api/customers/**", "customers", "customers", "orders", "shipments");
        resource(auth, "/api/how-sold/**", "units", "units", "inventory", "orders", "reorders", "products");
        resource(auth, "/api/how-sold-conversions/**", "units", "units", "reorders");
        resource(auth, "/api/inventory/**", "inventory", "inventory", "reorders");
        resource(auth, "/api/orders/**", "orders", "orders", "shipments");
        resource(auth, "/api/products/**", "products",
                "products", "inventory", "orders", "reorders", "units", "suppliers", "shelves");
        resource(auth, "/api/reorders/**", "reorders", "reorders");
        resource(auth, "/api/rows/**", "shelves", "shelves");
        resource(auth, "/api/shelves/**", "shelves", "shelves");
        resource(auth, "/api/shipments/**", "shipments", "shipments");
        resource(auth, "/api/suppliers/**", "suppliers", "suppliers", "reorders");
        resource(auth, "/api/trucks/**", "trucks", "trucks", "shipments");
        resource(auth, "/api/warehouse-employees/**", "employees", "employees", "shipments");
        resource(auth, "/api/warehouses/**", "warehouses", "warehouses", "aisles", "employees", "shelves");

        // Deny by default.
        auth.anyRequest().denyAll();
    }

    private static void resource(
            AuthorizeHttpRequestsConfigurer<HttpSecurity>.AuthorizationManagerRequestMatcherRegistry auth,
            String path,
            String writes,
            String... readers) {
        String[] views = Arrays.stream(readers).map(r -> r + ":view").toArray(String[]::new);
        auth.requestMatchers(HttpMethod.GET, path).hasAnyAuthority(views)
                .requestMatchers(HttpMethod.POST, path).hasAuthority(writes + ":create")
                .requestMatchers(HttpMethod.PUT, path).hasAuthority(writes + ":update")
                .requestMatchers(HttpMethod.DELETE, path).hasAuthority(writes + ":delete");
    }
}

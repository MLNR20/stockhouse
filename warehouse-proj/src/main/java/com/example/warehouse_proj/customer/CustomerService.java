package com.example.warehouse_proj.customer;

import com.example.warehouse_proj.audit.AuditAction;
import com.example.warehouse_proj.audit.AuditLogService;
import com.example.warehouse_proj.common.PageQuery;
import com.example.warehouse_proj.common.PageResponse;
import com.example.warehouse_proj.common.PaginationService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomerService {

    private static final String ENTITY = "Customer";
    private static final Set<String> SORT_FIELDS = Set.of("firstName", "lastName", "email", "phoneNumber", "address");

    private final CustomerRepository customers;
    private final AuditLogService audit;
    private final PaginationService pagination;

    public CustomerService(CustomerRepository customers, AuditLogService audit, PaginationService pagination) {
        this.customers = customers;
        this.audit = audit;
        this.pagination = pagination;
    }

    @Transactional(readOnly = true)
    public List<CustomerDtos.Response> list() {
        return customers.findAllByActiveTrue().stream().map(CustomerDtos.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<CustomerDtos.Response> page(PageQuery query) {
        return pagination.page(customers, query, SORT_FIELDS, CustomerDtos.Response::from);
    }

    @Transactional(readOnly = true)
    public CustomerDtos.Response get(UUID id) {
        return CustomerDtos.Response.from(find(id));
    }

    @Transactional
    public CustomerDtos.Response create(CustomerDtos.Request request) {
        Customer customer = new Customer(
                request.firstName().trim(),
                request.lastName().trim(),
                request.email().trim(),
                request.phoneNumber().trim(),
                request.address().trim());
        customers.save(customer);
        audit.record(AuditAction.CREATE, ENTITY, customer.getId());
        return CustomerDtos.Response.from(customer);
    }

    @Transactional
    public CustomerDtos.Response update(UUID id, CustomerDtos.Request request) {
        Customer customer = find(id);
        customer.setFirstName(request.firstName().trim());
        customer.setLastName(request.lastName().trim());
        customer.setEmail(request.email().trim());
        customer.setPhoneNumber(request.phoneNumber().trim());
        customer.setAddress(request.address().trim());
        // Flush so @PreUpdate has set updatedAt before the response is built.
        customers.saveAndFlush(customer);
        audit.record(AuditAction.UPDATE, ENTITY, customer.getId());
        return CustomerDtos.Response.from(customer);
    }

    /** Soft delete: the row stays, IsActive becomes false. */
    @Transactional
    public void delete(UUID id) {
        find(id).setActive(false);
        audit.record(AuditAction.DELETE, ENTITY, id);
    }

    private Customer find(UUID id) {
        return customers.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Customer not found"));
    }
}

package com.example.warehouse_proj.access;

import com.example.warehouse_proj.common.BaseEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

/** A named bundle of permissions and assigned pages that users are given. */
@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    /** Standard roles cannot be deleted or renamed. */
    @Column(name = "is_system", nullable = false)
    private boolean system;

    /** e.g. "inventory:update". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", nullable = false, length = 100)
    private Set<String> permissions = new HashSet<>();

    /** Page paths the role may open, e.g. "/orders". */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_pages", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "page_path", nullable = false, length = 100)
    private Set<String> pages = new HashSet<>();

    protected Role() {
    }

    public Role(String name, String description, boolean system) {
        this.name = name;
        this.description = description;
        this.system = system;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isSystem() {
        return system;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<String> permissions) {
        this.permissions.clear();
        this.permissions.addAll(permissions);
    }

    public Set<String> getPages() {
        return pages;
    }

    public void setPages(Set<String> pages) {
        this.pages.clear();
        this.pages.addAll(pages);
    }

    /** The admin role always keeps everything, so nobody can lock themselves out. */
    public boolean isAdmin() {
        return system && AccessCatalog.ADMIN.equalsIgnoreCase(name);
    }
}

package com.mockify.backend.sandbox;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provides realistic pre-seeded record data for sandbox workspaces.
 *
 * Data is hardcoded here deliberately — it's presentation-layer content,
 * not business data. Storing it in the DB would require a migration for
 * every update and add operational overhead for no benefit.
 *
 * Values match the field types defined in the corresponding system templates
 * (V10 migration). If a template schema changes, update these records too.
 */
@Component
public class SandboxSeedDataProvider {

    /**
     * Template slugs to apply when creating a sandbox, in order.
     * These must exist in schema_templates table (seeded in V10).
     */
    public static final List<String> DEFAULT_TEMPLATE_SLUGS =
            List.of("user-profile", "product-item");

    /**
     * Returns seed records for a given template slug.
     * Returns an empty list if the slug is unknown — seeding is
     * best-effort and should not fail sandbox creation.
     */
    public List<Map<String, Object>> getSeedRecords(String templateSlug) {
        return switch (templateSlug) {
            case "user-profile" -> userProfileRecords();
            case "product-item" -> productItemRecords();
            default -> List.of();
        };
    }

    // -------------------------------------------------------------------------
    // Seed data definitions
    // -------------------------------------------------------------------------

    private List<Map<String, Object>> userProfileRecords() {
        return List.of(
                Map.of(
                        "id",        UUID.randomUUID().toString(),
                        "name",      "Tony Stark",
                        "email",     "ironman@newyork.com",
                        "age",       50,
                        "isActive",  true,
                        "createdAt", "2025-01-15T10:30:00Z"
                ),
                Map.of(
                        "id",        UUID.randomUUID().toString(),
                        "name",      "Peter Parker",
                        "email",     "spiderman@newyork.com",
                        "age",       20,
                        "isActive",  true,
                        "createdAt", "2025-01-16T14:20:00Z"
                ),
                Map.of(
                        "id",        UUID.randomUUID().toString(),
                        "name",      "Victor Von Doom",
                        "email",     "doctordoom@latveria.com",
                        "age",       40,
                        "isActive",  false,
                        "createdAt", "2025-01-17T09:15:00Z"
                )
        );
    }

    private List<Map<String, Object>> productItemRecords() {
        return List.of(
                Map.of(
                        "id",       UUID.randomUUID().toString(),
                        "name",     "Wireless Headphones",
                        "price",    79.99,
                        "currency", "USD",
                        "inStock",  true
                ),
                Map.of(
                        "id",       UUID.randomUUID().toString(),
                        "name",     "USB-C Hub 7-in-1",
                        "price",    34.99,
                        "currency", "USD",
                        "inStock",  true
                ),
                Map.of(
                        "id",       UUID.randomUUID().toString(),
                        "name",     "Mechanical Keyboard",
                        "price",    129.99,
                        "currency", "USD",
                        "inStock",  false
                )
        );
    }
}
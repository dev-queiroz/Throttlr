package com.example.ratelimiter.domain;

import java.util.Objects;

public record TenantContext(
        String tenantId,
        String organizationId,
        String planId,
        String userId,
        String endpoint,
        int cost
) {

    public TenantContext {
        Objects.requireNonNull(tenantId, "tenantId is required");
        Objects.requireNonNull(endpoint, "endpoint is required");
        if (cost <= 0) {
            cost = 1;
        }
    }
}

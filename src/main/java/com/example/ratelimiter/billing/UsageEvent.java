package com.example.ratelimiter.billing;

import java.time.Instant;

public record UsageEvent(
        String tenantId,
        String userId,
        String endpoint,
        Instant timestamp,
        int cost,
        String policyId,
        boolean softLimited,
        boolean fallbackApplied
) {
}

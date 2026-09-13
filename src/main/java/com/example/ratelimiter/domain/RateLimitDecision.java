package com.example.ratelimiter.domain;

import java.time.Duration;

public record RateLimitDecision(
        boolean allowed,
        boolean softLimited,
        boolean fallbackApplied,
        String policyId,
        RateLimitAlgorithm algorithm,
        long remaining,
        long limit,
        Duration retryAfter,
        String reason
) {

    public static RateLimitDecision allowed(RateLimitPolicy policy, long remaining, boolean softLimited) {
        return new RateLimitDecision(true, softLimited, false, policy.id(), policy.algorithm(), remaining,
                policy.hardLimit(), Duration.ZERO, "allowed");
    }

    public static RateLimitDecision rejected(RateLimitPolicy policy, Duration retryAfter, String reason) {
        return new RateLimitDecision(false, false, false, policy.id(), policy.algorithm(), 0,
                policy.hardLimit(), retryAfter, reason);
    }

    public static RateLimitDecision fallback(RateLimitPolicy policy, boolean allowed, String reason) {
        return new RateLimitDecision(allowed, false, true, policy.id(), policy.algorithm(),
                allowed ? policy.hardLimit() : 0, policy.hardLimit(), Duration.ZERO, reason);
    }
}

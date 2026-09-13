package com.example.ratelimiter.domain;

import java.time.Duration;
import java.util.Objects;

public record RateLimitPolicy(
        String id,
        PolicyScope scope,
        RateLimitAlgorithm algorithm,
        long hardLimit,
        long softLimit,
        long refillRatePerSecond,
        long burstCapacity,
        Duration window,
        int cost,
        FallbackMode fallbackMode
) {

    public RateLimitPolicy {
        Objects.requireNonNull(id, "id is required");
        Objects.requireNonNull(scope, "scope is required");
        Objects.requireNonNull(algorithm, "algorithm is required");
        Objects.requireNonNull(window, "window is required");
        Objects.requireNonNull(fallbackMode, "fallbackMode is required");
        if (hardLimit <= 0) {
            throw new IllegalArgumentException("hardLimit must be positive");
        }
        if (softLimit < 0 || softLimit > hardLimit) {
            throw new IllegalArgumentException("softLimit must be between zero and hardLimit");
        }
        if (cost <= 0) {
            throw new IllegalArgumentException("cost must be positive");
        }
        if (burstCapacity <= 0) {
            burstCapacity = hardLimit;
        }
        if (refillRatePerSecond <= 0) {
            refillRatePerSecond = Math.max(1, hardLimit / Math.max(1, window.toSeconds()));
        }
    }

    public boolean hasSoftLimit() {
        return softLimit > 0;
    }
}

package com.example.ratelimiter.domain;

import java.time.Duration;

public record RedisRateLimitResult(
        boolean allowed,
        long remaining,
        long limit,
        Duration retryAfter
) {
}

package com.example.ratelimiter.domain;

public enum RateLimitAlgorithm {
    TOKEN_BUCKET,
    SLIDING_WINDOW_COUNTER,
    LEAKY_BUCKET
}

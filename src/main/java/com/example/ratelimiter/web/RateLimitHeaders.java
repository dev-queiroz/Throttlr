package com.example.ratelimiter.web;

public final class RateLimitHeaders {

    public static final String TENANT_ID = "X-Tenant-Id";
    public static final String ORGANIZATION_ID = "X-Organization-Id";
    public static final String PLAN_ID = "X-Plan-Id";
    public static final String USER_ID = "X-User-Id";
    public static final String REQUEST_COST = "X-Request-Cost";
    public static final String LIMIT = "X-RateLimit-Limit";
    public static final String REMAINING = "X-RateLimit-Remaining";
    public static final String RETRY_AFTER = "Retry-After";
    public static final String SOFT_LIMIT = "X-RateLimit-Soft-Limit";
    public static final String FALLBACK = "X-RateLimit-Fallback";
    public static final String POLICY = "X-RateLimit-Policy";

    private RateLimitHeaders() {
    }
}

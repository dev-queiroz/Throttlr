package com.example.ratelimiter.policy;

import java.util.Optional;

import com.example.ratelimiter.domain.RateLimitPolicy;
import com.example.ratelimiter.domain.TenantContext;

public interface PolicyRepository {

    Optional<RateLimitPolicy> findUserOverride(TenantContext context);

    Optional<RateLimitPolicy> findOrganizationLimit(TenantContext context);

    Optional<RateLimitPolicy> findPlanLimit(TenantContext context);

    RateLimitPolicy globalLimit();
}

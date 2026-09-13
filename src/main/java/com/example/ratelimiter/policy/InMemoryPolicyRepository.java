package com.example.ratelimiter.policy;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.domain.PolicyScope;
import com.example.ratelimiter.domain.RateLimitPolicy;
import com.example.ratelimiter.domain.TenantContext;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryPolicyRepository implements PolicyRepository {

    private final RateLimiterProperties properties;
    private final RateLimitPolicy globalPolicy;

    public InMemoryPolicyRepository(RateLimiterProperties properties) {
        this.properties = properties;
        this.globalPolicy = properties.getGlobalPolicy().toDomain(properties.getFallback().getDefaultMode());
    }

    @Override
    public Optional<RateLimitPolicy> findUserOverride(TenantContext context) {
        return findPolicy(PolicyScope.USER, context.tenantId(), null, null, context.userId());
    }

    @Override
    public Optional<RateLimitPolicy> findOrganizationLimit(TenantContext context) {
        return findPolicy(PolicyScope.ORGANIZATION, context.tenantId(), null, context.organizationId(), null);
    }

    @Override
    public Optional<RateLimitPolicy> findPlanLimit(TenantContext context) {
        return findPolicy(PolicyScope.PLAN, context.tenantId(), context.planId(), null, null);
    }

    @Override
    public RateLimitPolicy globalLimit() {
        return globalPolicy;
    }

    private Optional<RateLimitPolicy> findPolicy(
            PolicyScope scope,
            String tenantId,
            String planId,
            String organizationId,
            String userId
    ) {
        List<RateLimiterProperties.Policy> configured = properties.getPolicies();
        return configured.stream()
                .filter(policy -> policy.getScope() == scope)
                .filter(policy -> matches(policy.getTenantId(), tenantId))
                .filter(policy -> matches(policy.getPlanId(), planId))
                .filter(policy -> matches(policy.getOrganizationId(), organizationId))
                .filter(policy -> matches(policy.getUserId(), userId))
                .findFirst()
                .map(policy -> policy.toDomain(properties.getFallback().getDefaultMode()));
    }

    private boolean matches(String configured, String requested) {
        return configured == null || Objects.equals(configured, requested);
    }
}

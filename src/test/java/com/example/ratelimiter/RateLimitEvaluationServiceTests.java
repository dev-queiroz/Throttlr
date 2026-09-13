package com.example.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import com.example.ratelimiter.domain.FallbackMode;
import com.example.ratelimiter.domain.PolicyScope;
import com.example.ratelimiter.domain.RateLimitAlgorithm;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.domain.RateLimitPolicy;
import com.example.ratelimiter.domain.RedisRateLimitResult;
import com.example.ratelimiter.domain.TenantContext;
import com.example.ratelimiter.evaluation.RateLimitEvaluationService;
import com.example.ratelimiter.policy.PolicyRepository;
import com.example.ratelimiter.redis.RedisRateLimiterService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class RateLimitEvaluationServiceTests {

    @Test
    void resolvesUserOverrideBeforeLowerPriorityPolicies() {
        PolicyRepository policies = mock(PolicyRepository.class);
        RedisRateLimiterService redis = mock(RedisRateLimiterService.class);
        TenantContext context = new TenantContext("tenant", "org", "plan", "user", "/v1/orders", 1);
        RateLimitPolicy userPolicy = policy("user", PolicyScope.USER, 10);
        RateLimitPolicy orgPolicy = policy("org", PolicyScope.ORGANIZATION, 100);

        when(policies.findUserOverride(context)).thenReturn(Optional.of(userPolicy));
        when(policies.findOrganizationLimit(context)).thenReturn(Optional.of(orgPolicy));
        when(redis.evaluate(userPolicy, context)).thenReturn(new RedisRateLimitResult(true, 5, 10, Duration.ZERO));

        RateLimitDecision decision = service(policies, redis).evaluate(context);

        assertThat(decision.policyId()).isEqualTo("user");
        assertThat(decision.allowed()).isTrue();
    }

    @Test
    void rejectsWhenHardLimitIsExceeded() {
        PolicyRepository policies = mock(PolicyRepository.class);
        RedisRateLimiterService redis = mock(RedisRateLimiterService.class);
        TenantContext context = new TenantContext("tenant", "org", "plan", "user", "/v1/orders", 1);
        RateLimitPolicy global = policy("global", PolicyScope.GLOBAL, 10);

        when(policies.findUserOverride(context)).thenReturn(Optional.empty());
        when(policies.findOrganizationLimit(context)).thenReturn(Optional.empty());
        when(policies.findPlanLimit(context)).thenReturn(Optional.empty());
        when(policies.globalLimit()).thenReturn(global);
        when(redis.evaluate(global, context)).thenReturn(new RedisRateLimitResult(false, 0, 10, Duration.ofSeconds(2)));

        RateLimitDecision decision = service(policies, redis).evaluate(context);

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.reason()).isEqualTo("hard_limit_exceeded");
    }

    private RateLimitEvaluationService service(PolicyRepository policies, RedisRateLimiterService redis) {
        return new RateLimitEvaluationService(policies, redis, new SimpleMeterRegistry());
    }

    private RateLimitPolicy policy(String id, PolicyScope scope, long hardLimit) {
        return new RateLimitPolicy(id, scope, RateLimitAlgorithm.TOKEN_BUCKET, hardLimit, hardLimit - 1,
                hardLimit, hardLimit, Duration.ofSeconds(60), 1, FallbackMode.FAIL_OPEN);
    }
}

package com.example.ratelimiter.evaluation;

import com.example.ratelimiter.domain.FallbackMode;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.domain.RateLimitPolicy;
import com.example.ratelimiter.domain.RedisRateLimitResult;
import com.example.ratelimiter.domain.TenantContext;
import com.example.ratelimiter.policy.PolicyRepository;
import com.example.ratelimiter.redis.RedisRateLimiterService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class RateLimitEvaluationService {

    private final PolicyRepository policyRepository;
    private final RedisRateLimiterService redisRateLimiterService;
    private final Counter rejectedCounter;
    private final Counter softLimitedCounter;
    private final Counter fallbackCounter;

    public RateLimitEvaluationService(
            PolicyRepository policyRepository,
            RedisRateLimiterService redisRateLimiterService,
            MeterRegistry meterRegistry
    ) {
        this.policyRepository = policyRepository;
        this.redisRateLimiterService = redisRateLimiterService;
        this.rejectedCounter = Counter.builder("rate.limit.rejected")
                .description("Requests rejected with HTTP 429")
                .register(meterRegistry);
        this.softLimitedCounter = Counter.builder("rate.limit.soft_limited")
                .description("Requests allowed after crossing the configured soft limit")
                .register(meterRegistry);
        this.fallbackCounter = Counter.builder("rate.limit.fallback")
                .description("Requests decided by fallback behavior")
                .register(meterRegistry);
    }

    @CircuitBreaker(name = "redisRateLimiter", fallbackMethod = "fallback")
    public RateLimitDecision evaluate(TenantContext context) {
        RateLimitPolicy policy = resolvePolicy(context);
        RedisRateLimitResult result = redisRateLimiterService.evaluate(policy, context);
        RateLimitDecision decision = toDecision(policy, result);
        record(decision);
        return decision;
    }

    @SuppressWarnings("unused")
    RateLimitDecision fallback(TenantContext context, Throwable throwable) {
        RateLimitPolicy policy = resolvePolicy(context);
        boolean allowed = policy.fallbackMode() == FallbackMode.FAIL_OPEN;
        RateLimitDecision decision = RateLimitDecision.fallback(policy, allowed,
                allowed ? "redis_unavailable_fail_open" : "redis_unavailable_fail_closed");
        record(decision);
        return decision;
    }

    private RateLimitPolicy resolvePolicy(TenantContext context) {
        return policyRepository.findUserOverride(context)
                .or(() -> policyRepository.findOrganizationLimit(context))
                .or(() -> policyRepository.findPlanLimit(context))
                .orElseGet(policyRepository::globalLimit);
    }

    private RateLimitDecision toDecision(RateLimitPolicy policy, RedisRateLimitResult result) {
        if (!result.allowed()) {
            return RateLimitDecision.rejected(policy, result.retryAfter(), "hard_limit_exceeded");
        }

        boolean softLimited = policy.hasSoftLimit() && result.remaining() <= Math.max(0, policy.hardLimit() - policy.softLimit());
        return RateLimitDecision.allowed(policy, result.remaining(), softLimited);
    }

    private void record(RateLimitDecision decision) {
        if (decision.fallbackApplied()) {
            fallbackCounter.increment();
        }
        if (!decision.allowed()) {
            rejectedCounter.increment();
        }
        if (decision.softLimited()) {
            softLimitedCounter.increment();
        }
    }
}

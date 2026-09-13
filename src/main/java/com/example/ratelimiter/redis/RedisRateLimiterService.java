package com.example.ratelimiter.redis;

import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import com.example.ratelimiter.domain.RateLimitAlgorithm;
import com.example.ratelimiter.domain.RateLimitPolicy;
import com.example.ratelimiter.domain.RedisRateLimitResult;
import com.example.ratelimiter.domain.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class RedisRateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;
    private final Timer latencyTimer;
    private final Counter failureCounter;
    private final DefaultRedisScript<List> tokenBucketScript;
    private final DefaultRedisScript<List> slidingWindowScript;
    private final DefaultRedisScript<List> leakyBucketScript;

    @Autowired
    public RedisRateLimiterService(StringRedisTemplate redisTemplate, MeterRegistry meterRegistry) {
        this(redisTemplate, Clock.systemUTC(), meterRegistry);
    }

    RedisRateLimiterService(StringRedisTemplate redisTemplate, Clock clock, MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
        this.latencyTimer = Timer.builder("redis.rate.limit.latency")
                .publishPercentileHistogram()
                .publishPercentiles(0.95, 0.99)
                .register(meterRegistry);
        this.failureCounter = Counter.builder("redis.rate.limit.failures")
                .description("Redis rate limit execution failures")
                .register(meterRegistry);
        this.tokenBucketScript = script("lua/token_bucket.lua");
        this.slidingWindowScript = script("lua/sliding_window_counter.lua");
        this.leakyBucketScript = script("lua/leaky_bucket.lua");
    }

    public RedisRateLimitResult evaluate(RateLimitPolicy policy, TenantContext context) {
        return latencyTimer.record(() -> evaluateTimed(policy, context));
    }

    private RedisRateLimitResult evaluateTimed(RateLimitPolicy policy, TenantContext context) {
        try {
            long nowMs = clock.millis();
            return switch (policy.algorithm()) {
                case TOKEN_BUCKET -> executeTokenBucket(policy, context, nowMs);
                case SLIDING_WINDOW_COUNTER -> executeSlidingWindow(policy, context, nowMs);
                case LEAKY_BUCKET -> executeLeakyBucket(policy, context, nowMs);
            };
        } catch (RuntimeException ex) {
            failureCounter.increment();
            throw ex;
        }
    }

    private RedisRateLimitResult executeTokenBucket(RateLimitPolicy policy, TenantContext context, long nowMs) {
        String base = hashTaggedBase(policy, context);
        long ttlMs = Math.max(policy.window().multipliedBy(2).toMillis(), Duration.ofSeconds(10).toMillis());
        List<Long> response = execute(tokenBucketScript,
                List.of(base + ":tokens", base + ":ts"),
                policy.burstCapacity(),
                policy.refillRatePerSecond(),
                cost(policy, context),
                nowMs,
                ttlMs);
        return map(response);
    }

    private RedisRateLimitResult executeSlidingWindow(RateLimitPolicy policy, TenantContext context, long nowMs) {
        long windowMs = policy.window().toMillis();
        long currentWindow = Math.floorDiv(nowMs, windowMs);
        String base = hashTaggedBase(policy, context);
        List<Long> response = execute(slidingWindowScript,
                List.of(base + ":sw:" + currentWindow, base + ":sw:" + (currentWindow - 1)),
                policy.hardLimit(),
                windowMs,
                cost(policy, context),
                nowMs);
        return map(response);
    }

    private RedisRateLimitResult executeLeakyBucket(RateLimitPolicy policy, TenantContext context, long nowMs) {
        String base = hashTaggedBase(policy, context);
        long ttlMs = Math.max(policy.window().multipliedBy(2).toMillis(), Duration.ofSeconds(10).toMillis());
        List<Long> response = execute(leakyBucketScript,
                List.of(base + ":level", base + ":ts"),
                policy.burstCapacity(),
                policy.refillRatePerSecond(),
                cost(policy, context),
                nowMs,
                ttlMs);
        return map(response);
    }

    @SuppressWarnings("unchecked")
    private List<Long> execute(DefaultRedisScript<List> script, List<String> keys, Object... args) {
        Object[] serializedArgs = Arrays.stream(args)
                .map(String::valueOf)
                .toArray();
        Object result = redisTemplate.execute(script, keys, serializedArgs);
        if (result instanceof List<?> values) {
            return values.stream()
                    .map(value -> ((Number) value).longValue())
                    .toList();
        }
        throw new IllegalStateException("Unexpected Redis script response: " + result);
    }

    private RedisRateLimitResult map(List<Long> response) {
        boolean allowed = response.get(0) == 1;
        long remaining = response.get(1);
        long limit = response.get(2);
        Duration retryAfter = Duration.ofMillis(response.get(3));
        return new RedisRateLimitResult(allowed, remaining, limit, retryAfter);
    }

    private DefaultRedisScript<List> script(String path) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource(path));
        script.setResultType(List.class);
        return script;
    }

    private String hashTaggedBase(RateLimitPolicy policy, TenantContext context) {
        String identity = switch (policy.scope()) {
            case USER -> "user:" + context.userId();
            case ORGANIZATION -> "org:" + context.organizationId();
            case PLAN -> "plan:" + context.planId();
            case GLOBAL -> "global";
        };
        return "rl:{" + context.tenantId() + "}:" + policy.id() + ":" + identity + ":" + sanitize(context.endpoint());
    }

    private int cost(RateLimitPolicy policy, TenantContext context) {
        return Math.max(policy.cost(), context.cost());
    }

    private String sanitize(String value) {
        return value.replaceAll("[^a-zA-Z0-9:_./-]", "_");
    }
}

package com.example.ratelimiter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.example.ratelimiter.billing.UsageEvent;
import com.example.ratelimiter.billing.UsageEventPublisher;
import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.domain.RateLimitAlgorithm;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.domain.TenantContext;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class UsageEventPublisherTests {

    @Test
    void publishesOnlyAllowedRequests() {
        KafkaTemplate<String, UsageEvent> kafkaTemplate = mock();
        RateLimiterProperties properties = new RateLimiterProperties();
        Executor directExecutor = Runnable::run;
        UsageEventPublisher publisher = new UsageEventPublisher(kafkaTemplate, properties, directExecutor,
                new SimpleMeterRegistry());
        TenantContext context = new TenantContext("tenant", "org", "plan", "user", "/v1/orders", 2);
        RateLimitDecision allowed = new RateLimitDecision(true, false, false, "policy",
                RateLimitAlgorithm.TOKEN_BUCKET, 10, 20, Duration.ZERO, "allowed");

        when(kafkaTemplate.send(eq("api-usage-events"), eq("tenant"), any(UsageEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        publisher.publishAllowed(context, allowed);

        verify(kafkaTemplate).send(eq("api-usage-events"), eq("tenant"), any(UsageEvent.class));
    }

    @Test
    void skipsRejectedRequests() {
        KafkaTemplate<String, UsageEvent> kafkaTemplate = mock();
        RateLimiterProperties properties = new RateLimiterProperties();
        UsageEventPublisher publisher = new UsageEventPublisher(kafkaTemplate, properties, Runnable::run,
                new SimpleMeterRegistry());
        TenantContext context = new TenantContext("tenant", "org", "plan", "user", "/v1/orders", 2);
        RateLimitDecision rejected = new RateLimitDecision(false, false, false, "policy",
                RateLimitAlgorithm.TOKEN_BUCKET, 0, 20, Duration.ofSeconds(1), "hard_limit_exceeded");

        publisher.publishAllowed(context, rejected);

        verify(kafkaTemplate, never()).send(any(), any(), any());
    }
}

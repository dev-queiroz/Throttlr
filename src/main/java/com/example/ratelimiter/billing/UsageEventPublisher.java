package com.example.ratelimiter.billing;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.Executor;

import com.example.ratelimiter.config.RateLimiterProperties;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.domain.TenantContext;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UsageEventPublisher {

    private final KafkaTemplate<String, UsageEvent> kafkaTemplate;
    private final RateLimiterProperties properties;
    private final Executor virtualThreadExecutor;
    private final Clock clock;
    private final Counter publishedCounter;
    private final Counter failedCounter;

    public UsageEventPublisher(
            KafkaTemplate<String, UsageEvent> kafkaTemplate,
            RateLimiterProperties properties,
            @Qualifier("virtualThreadExecutor")
            Executor virtualThreadExecutor,
            MeterRegistry meterRegistry
    ) {
        this(kafkaTemplate, properties, virtualThreadExecutor, Clock.systemUTC(), meterRegistry);
    }

    UsageEventPublisher(
            KafkaTemplate<String, UsageEvent> kafkaTemplate,
            RateLimiterProperties properties,
            Executor virtualThreadExecutor,
            Clock clock,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.virtualThreadExecutor = virtualThreadExecutor;
        this.clock = clock;
        this.publishedCounter = Counter.builder("usage.events.published")
                .description("Usage events accepted by Kafka producer")
                .register(meterRegistry);
        this.failedCounter = Counter.builder("usage.events.failed")
                .description("Usage events that failed to publish")
                .register(meterRegistry);
    }

    public void publishAllowed(TenantContext context, RateLimitDecision decision) {
        if (!decision.allowed()) {
            return;
        }

        virtualThreadExecutor.execute(() -> {
            UsageEvent event = new UsageEvent(
                    context.tenantId(),
                    context.userId(),
                    context.endpoint(),
                    Instant.now(clock),
                    context.cost(),
                    decision.policyId(),
                    decision.softLimited(),
                    decision.fallbackApplied()
            );

            kafkaTemplate.send(properties.getKafka().getUsageTopic(), context.tenantId(), event)
                    .whenComplete((result, error) -> {
                        if (error == null) {
                            publishedCounter.increment();
                        } else {
                            failedCounter.increment();
                        }
                    });
        });
    }
}

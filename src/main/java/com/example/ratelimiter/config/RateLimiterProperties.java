package com.example.ratelimiter.config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.example.ratelimiter.domain.FallbackMode;
import com.example.ratelimiter.domain.PolicyScope;
import com.example.ratelimiter.domain.RateLimitAlgorithm;
import com.example.ratelimiter.domain.RateLimitPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    @Valid
    private Kafka kafka = new Kafka();

    @Valid
    private Fallback fallback = new Fallback();

    @Valid
    private Policy globalPolicy = new Policy();

    @Valid
    private List<Policy> policies = new ArrayList<>();

    public Kafka getKafka() {
        return kafka;
    }

    public void setKafka(Kafka kafka) {
        this.kafka = kafka;
    }

    public Fallback getFallback() {
        return fallback;
    }

    public void setFallback(Fallback fallback) {
        this.fallback = fallback;
    }

    public Policy getGlobalPolicy() {
        return globalPolicy;
    }

    public void setGlobalPolicy(Policy globalPolicy) {
        this.globalPolicy = globalPolicy;
    }

    public List<Policy> getPolicies() {
        return policies;
    }

    public void setPolicies(List<Policy> policies) {
        this.policies = policies;
    }

    public static class Kafka {
        @NotBlank
        private String usageTopic = "api-usage-events";

        public String getUsageTopic() {
            return usageTopic;
        }

        public void setUsageTopic(String usageTopic) {
            this.usageTopic = usageTopic;
        }
    }

    public static class Fallback {
        private FallbackMode defaultMode = FallbackMode.FAIL_OPEN;

        public FallbackMode getDefaultMode() {
            return defaultMode;
        }

        public void setDefaultMode(FallbackMode defaultMode) {
            this.defaultMode = defaultMode;
        }
    }

    public static class Policy {
        @NotBlank
        private String id = "global-default";
        private PolicyScope scope = PolicyScope.GLOBAL;
        private RateLimitAlgorithm algorithm = RateLimitAlgorithm.TOKEN_BUCKET;
        @Positive
        private long hardLimit = 1000;
        private long softLimit = 900;
        private long refillRatePerSecond = 100;
        private long burstCapacity = 1000;
        private Duration window = Duration.ofSeconds(60);
        @Positive
        private int cost = 1;
        private FallbackMode fallbackMode;
        private String tenantId;
        private String planId;
        private String organizationId;
        private String userId;

        public RateLimitPolicy toDomain(FallbackMode defaultFallback) {
            return new RateLimitPolicy(id, scope, algorithm, hardLimit, softLimit, refillRatePerSecond,
                    burstCapacity, window, cost, fallbackMode == null ? defaultFallback : fallbackMode);
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public PolicyScope getScope() {
            return scope;
        }

        public void setScope(PolicyScope scope) {
            this.scope = scope;
        }

        public RateLimitAlgorithm getAlgorithm() {
            return algorithm;
        }

        public void setAlgorithm(RateLimitAlgorithm algorithm) {
            this.algorithm = algorithm;
        }

        public long getHardLimit() {
            return hardLimit;
        }

        public void setHardLimit(long hardLimit) {
            this.hardLimit = hardLimit;
        }

        public long getSoftLimit() {
            return softLimit;
        }

        public void setSoftLimit(long softLimit) {
            this.softLimit = softLimit;
        }

        public long getRefillRatePerSecond() {
            return refillRatePerSecond;
        }

        public void setRefillRatePerSecond(long refillRatePerSecond) {
            this.refillRatePerSecond = refillRatePerSecond;
        }

        public long getBurstCapacity() {
            return burstCapacity;
        }

        public void setBurstCapacity(long burstCapacity) {
            this.burstCapacity = burstCapacity;
        }

        public Duration getWindow() {
            return window;
        }

        public void setWindow(Duration window) {
            this.window = window;
        }

        public int getCost() {
            return cost;
        }

        public void setCost(int cost) {
            this.cost = cost;
        }

        public FallbackMode getFallbackMode() {
            return fallbackMode;
        }

        public void setFallbackMode(FallbackMode fallbackMode) {
            this.fallbackMode = fallbackMode;
        }

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getPlanId() {
            return planId;
        }

        public void setPlanId(String planId) {
            this.planId = planId;
        }

        public String getOrganizationId() {
            return organizationId;
        }

        public void setOrganizationId(String organizationId) {
            this.organizationId = organizationId;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }
    }
}

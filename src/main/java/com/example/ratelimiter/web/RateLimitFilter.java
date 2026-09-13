package com.example.ratelimiter.web;

import java.io.IOException;
import java.time.Duration;
import java.util.Set;

import com.example.ratelimiter.billing.UsageEventPublisher;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.domain.TenantContext;
import com.example.ratelimiter.evaluation.RateLimitEvaluationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    private final RateLimitEvaluationService evaluationService;
    private final UsageEventPublisher usageEventPublisher;

    public RateLimitFilter(RateLimitEvaluationService evaluationService, UsageEventPublisher usageEventPublisher) {
        this.evaluationService = evaluationService;
        this.usageEventPublisher = usageEventPublisher;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TenantContext context = contextFrom(request);
        RateLimitDecision decision = evaluationService.evaluate(context);
        writeHeaders(response, decision);

        if (!decision.allowed()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("""
                    {"error":"too_many_requests","message":"Rate limit exceeded"}
                    """);
            return;
        }

        usageEventPublisher.publishAllowed(context, decision);
        filterChain.doFilter(request, response);
    }

    private TenantContext contextFrom(HttpServletRequest request) {
        String tenantId = headerOrDefault(request, RateLimitHeaders.TENANT_ID, "default");
        String organizationId = headerOrDefault(request, RateLimitHeaders.ORGANIZATION_ID, "default");
        String planId = headerOrDefault(request, RateLimitHeaders.PLAN_ID, "free");
        String userId = headerOrDefault(request, RateLimitHeaders.USER_ID, "anonymous");
        int cost = parseCost(request.getHeader(RateLimitHeaders.REQUEST_COST));
        return new TenantContext(tenantId, organizationId, planId, userId, request.getRequestURI(), cost);
    }

    private void writeHeaders(HttpServletResponse response, RateLimitDecision decision) {
        response.setHeader(RateLimitHeaders.LIMIT, Long.toString(decision.limit()));
        response.setHeader(RateLimitHeaders.REMAINING, Long.toString(decision.remaining()));
        response.setHeader(RateLimitHeaders.POLICY, decision.policyId());
        response.setHeader(RateLimitHeaders.SOFT_LIMIT, Boolean.toString(decision.softLimited()));
        response.setHeader(RateLimitHeaders.FALLBACK, Boolean.toString(decision.fallbackApplied()));
        if (!decision.allowed() && !decision.retryAfter().isZero()) {
            response.setHeader(RateLimitHeaders.RETRY_AFTER, Long.toString(secondsCeil(decision.retryAfter())));
        }
    }

    private String headerOrDefault(HttpServletRequest request, String header, String defaultValue) {
        String value = request.getHeader(header);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private int parseCost(String value) {
        if (value == null || value.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Integer.parseInt(value));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private long secondsCeil(Duration duration) {
        long millis = duration.toMillis();
        return Math.max(1, (millis + 999) / 1000);
    }
}

package com.example.ratelimiter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.example.ratelimiter.billing.UsageEventPublisher;
import com.example.ratelimiter.domain.RateLimitAlgorithm;
import com.example.ratelimiter.domain.RateLimitDecision;
import com.example.ratelimiter.evaluation.RateLimitEvaluationService;
import com.example.ratelimiter.web.RateLimitFilter;
import com.example.ratelimiter.web.RateLimitHeaders;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RateLimitFilterTests {

    @Test
    void rejectsHardLimitedRequestWith429() throws Exception {
        RateLimitEvaluationService evaluation = mock(RateLimitEvaluationService.class);
        UsageEventPublisher publisher = mock(UsageEventPublisher.class);
        RateLimitFilter filter = new RateLimitFilter(evaluation, publisher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        request.addHeader(RateLimitHeaders.TENANT_ID, "tenant");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(evaluation.evaluate(any())).thenReturn(new RateLimitDecision(false, false, false, "policy",
                RateLimitAlgorithm.TOKEN_BUCKET, 0, 10, Duration.ofSeconds(3), "hard_limit_exceeded"));

        filter.doFilter(request, response, chain);

        verify(chain, never()).doFilter(any(), any());
        verify(publisher, never()).publishAllowed(any(), any());
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader(RateLimitHeaders.RETRY_AFTER)).isEqualTo("3");
    }

    @Test
    void allowsRequestAndPublishesUsage() throws Exception {
        RateLimitEvaluationService evaluation = mock(RateLimitEvaluationService.class);
        UsageEventPublisher publisher = mock(UsageEventPublisher.class);
        RateLimitFilter filter = new RateLimitFilter(evaluation, publisher);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(evaluation.evaluate(any())).thenReturn(new RateLimitDecision(true, true, false, "policy",
                RateLimitAlgorithm.TOKEN_BUCKET, 1, 10, Duration.ZERO, "allowed"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(publisher).publishAllowed(any(), any());
        assertThat(response.getHeader(RateLimitHeaders.SOFT_LIMIT)).isEqualTo("true");
    }
}

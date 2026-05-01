package com.db_migrator.etl_system.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    // Configuration
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute

    private long lastCleanup = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only apply rate limiting to authentication endpoints
        if (path.startsWith("/api/auth/login")) {
            String clientIdentifier = getClientIdentifier(request);

            if (!allowRequest(clientIdentifier)) {
                log.warn("Rate limit exceeded for client: {} on path: {}", clientIdentifier, path);
                response.setStatus(429); // Too Many Requests
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again later.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Use IP address as identifier
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            clientIp = clientIp.split(",")[0].trim();
        }
        return clientIp;
    }

    private boolean allowRequest(String clientIdentifier) {
        cleanupOldBuckets();

        RateLimitBucket bucket = buckets.computeIfAbsent(
            clientIdentifier,
            k -> new RateLimitBucket(MAX_REQUESTS_PER_MINUTE)
        );

        return bucket.tryConsume();
    }

    private void cleanupOldBuckets() {
        long now = System.currentTimeMillis();
        if (now - lastCleanup > CLEANUP_INTERVAL_MS) {
            buckets.entrySet().removeIf(entry -> entry.getValue().isExpired());
            lastCleanup = now;
        }
    }

    private static class RateLimitBucket {
        private final int maxRequests;
        private final AtomicInteger count;
        private volatile long windowStart;
        private static final long WINDOW_SIZE_MS = 60000; // 1 minute

        public RateLimitBucket(int maxRequests) {
            this.maxRequests = maxRequests;
            this.count = new AtomicInteger(0);
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();

            // Reset window if expired
            if (now - windowStart > WINDOW_SIZE_MS) {
                windowStart = now;
                count.set(0);
            }

            // Check if under limit
            if (count.get() < maxRequests) {
                count.incrementAndGet();
                return true;
            }

            return false;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - windowStart > WINDOW_SIZE_MS * 2;
        }
    }
}

package uber.taxi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * A small, in-process safety limit for authentication endpoints. Production deployments with
 * multiple instances must also enforce equivalent limits at the ingress or API gateway.
 */
@Component
@Profile("prod")
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {
    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/auth/register", new Limit(5, Duration.ofMinutes(15)),
            "/api/auth/login", new Limit(10, Duration.ofMinutes(15)),
            "/api/auth/verify-email", new Limit(10, Duration.ofMinutes(15)),
            "/api/auth/resend-verification", new Limit(3, Duration.ofMinutes(15)));

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equalsIgnoreCase(request.getMethod()) || !LIMITS.containsKey(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Limit limit = LIMITS.get(path);
        String key = request.getRemoteAddr() + ':' + path;
        Window window = windows.computeIfAbsent(key, ignored -> new Window());
        long retryAfterSeconds = window.tryAcquire(limit);
        if (retryAfterSeconds > 0) {
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.getWriter().write("{\"status\":429,\"error\":\"Too Many Requests\","
                    + "\"message\":\"Too many attempts. Please try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Limit(int maximumRequests, Duration period) {
    }

    private static final class Window {
        private long startedAt = System.currentTimeMillis();
        private int requests;

        synchronized long tryAcquire(Limit limit) {
            long now = System.currentTimeMillis();
            long elapsed = now - startedAt;
            long periodMillis = limit.period().toMillis();
            if (elapsed >= periodMillis) {
                startedAt = now;
                requests = 0;
                elapsed = 0;
            }
            if (requests++ < limit.maximumRequests()) {
                return 0;
            }
            return Math.max(1, (periodMillis - elapsed + 999) / 1000);
        }
    }
}

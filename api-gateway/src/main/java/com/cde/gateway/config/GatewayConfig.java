package com.cde.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

@Configuration
public class GatewayConfig {

    /**
     * Rate limit per user (from JWT sub claim).
     * Falls back to IP if no auth header present (unauthenticated = tighter limit).
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            String auth = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                // Extract sub from JWT — simplified; in prod use JwtDecoder
                return Mono.just(auth.substring(7, Math.min(auth.length(), 30)));
            }
            // Fallback: rate limit by IP
            return Mono.just(exchange.getRequest().getRemoteAddress() != null
                    ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                    : "anonymous");
        };
    }

    /**
     * Circuit breaker fallback routes.
     */
    @Bean
    public RouterFunction<ServerResponse> fallbackRoutes() {
        return RouterFunctions
                .route(RequestPredicates.GET("/fallback/plm"), req -> fallbackResponse("PLM Service"))
                .andRoute(RequestPredicates.GET("/fallback/qlm"), req -> fallbackResponse("QLM Service"))
                .andRoute(RequestPredicates.GET("/fallback/llm"), req -> fallbackResponse("LLM Service"));
    }

    private Mono<ServerResponse> fallbackResponse(String service) {
        Map<String, Object> body = Map.of(
                "timestamp", OffsetDateTime.now().toString(),
                "status", 503,
                "error", "Service Temporarily Unavailable",
                "message", service + " is currently unavailable. Please retry shortly.",
                "retryAfter", "10"
        );
        return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }
}

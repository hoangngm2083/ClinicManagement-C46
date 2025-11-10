package com.clinic.c46.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        // Get the route information
        @SuppressWarnings("null")
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        String routeId = route != null ? route.getId() : "unknown";
        URI targetUri = route != null ? route.getUri() : null;
        
        log.info("🔵 Incoming Request: {} {} | Route: {} | Forwarding to: {}", 
            request.getMethod(), 
            request.getURI().getPath(),
            routeId,
            targetUri);
        
        long startTime = System.currentTimeMillis();
        
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            log.info("🟢 Response: {} | Status: {} | Duration: {}ms", 
                request.getURI().getPath(),
                exchange.getResponse().getStatusCode(),
                duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1; // Highest priority
    }
}

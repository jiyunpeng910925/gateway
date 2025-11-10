package com.jyp.gateway.api.filter;


import com.jyp.gateway.api.util.JwtUtil;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * 过滤器
 */
@Component
public class AuthenticationGlobalFilter implements GlobalFilter, Ordered{

    // 白名单路径，放行不进行认证
    private static final List<String> WHITELIST = Arrays.asList(
            "/user-service/login", // 假设的登录接口
            "/user-service/register" // 假设的注册接口
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        // 1. 白名单路径处理
        String path = request.getURI().getPath();
        if (isWhitelisted(path)) {
            return chain.filter(exchange); // 直接放行
        }

        // 2. 获取 Token
        String token = getToken(request);
        if (!StringUtils.hasText(token)) {
            return unauthorizedResponse(response, "Token not found");
        }

        // 3. 校验 Token
        if (!JwtUtil.validateToken(token)) {
            return unauthorizedResponse(response, "Invalid Token");
        }

        // 4. 解析用户信息，并放入请求头
        String userId = JwtUtil.getUserIdFromToken(token);
        ServerHttpRequest newRequest = request.mutate()
                .header("X-User-ID", userId) // 将解析出的 userId 放入请求头
                .build();

        ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();

        // 5. 放行
        return chain.filter(newExchange);
    }

    /**
     * 设置过滤器的执行顺序，数字越小，优先级越高。
     * 我们需要它在所有路由过滤器之前执行。
     */
    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::startsWith);
    }

    private String getToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorizedResponse(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        // 这里可以返回更详细的 JSON 错误信息
        return response.setComplete();
    }
}

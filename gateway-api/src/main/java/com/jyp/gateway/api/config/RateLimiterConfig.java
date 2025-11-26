package com.jyp.gateway.api.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    /**
     * 创建一个 KeyResolver 的 Bean，用于 IP 限流
     * Bean 的名称 "ipKeyResolver" 必须和配置文件中的 key-resolver 值一致
     */
    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            // 使用 Optional 来优雅地处理可能为 null 的情况
            return Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                    .map(address -> Mono.just(address.getHostName()))
                    .orElse(Mono.empty()); // 如果 getRemoteAddress() 返回 null，则返回一个空的 Mono
        };
    }

    /**
     * 【核心】自定义 RateLimiter Bean 来覆盖默认行为
     * 我们注入默认的 RedisRateLimiter，然后包装它
     */
    @Primary // 【核心】使用 @Primary 注解，确保这个自定义的 Bean 会被优先使用
    @Bean
    public RateLimiter<RedisRateLimiter.Config> customRateLimiter(RedisRateLimiter redisRateLimiter) {
        return new RateLimiter<RedisRateLimiter.Config>() {
            @Override
            public Mono<Response> isAllowed(String routeId, String id) {
                // 直接调用默认的 isAllowed 方法来获取结果
                return redisRateLimiter.isAllowed(routeId, id)
                        .flatMap(response -> {
                            // isAllowed() 返回 true，代表允许通行
                            if (response.isAllowed()) {
                                return Mono.just(response);
                            }

                            // isAllowed() 返回 false，代表被限流
                            // 【核心改造】在这里，我们不再让它走默认的内部异常流程
                            // 而是抛出一个我们自己的、可以被全局异常处理器捕获的异常
                            // 我们附带了自定义的错误消息
                            return Mono.error(new ResponseStatusException(
                                    HttpStatus.TOO_MANY_REQUESTS,
                                    "请求过于频繁，请稍后再试 (Rate limit exceeded)"));
                        });
            }

            @Override
            public Map<String, RedisRateLimiter.Config> getConfig() {
                return redisRateLimiter.getConfig();
            }

            @Override
            public Class<RedisRateLimiter.Config> getConfigClass() {
                return redisRateLimiter.getConfigClass();
            }

            @Override
            public RedisRateLimiter.Config newConfig() {
                return redisRateLimiter.newConfig();
            }
        };
    }
}

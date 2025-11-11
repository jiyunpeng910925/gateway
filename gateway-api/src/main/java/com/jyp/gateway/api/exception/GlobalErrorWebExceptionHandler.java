package com.jyp.gateway.api.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(-2) // 【核心】设置最高优先级，确保在 Spring Boot 默认的异常处理器之前执行
public class JsonExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        // 检查响应是否已经提交，如果已提交则无法再修改
        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        // 准备返回的 JSON 数据
        Map<String, Object> errorResponse = new HashMap<>();
        HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR; // 默认为 500
        String errorMessage = "Internal Server Error";

        // 【核心】判断异常类型
        if (ex instanceof ResponseStatusException) {
            // 当过滤器（如限流过滤器）拒绝请求时，通常会抛出 ResponseStatusException
            ResponseStatusException responseStatusException = (ResponseStatusException) ex;
            // 特别处理 429 Too Many Requests 错误
            if (responseStatusException.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                httpStatus = HttpStatus.TOO_MANY_REQUESTS;
                errorMessage = "请求过于频繁，请稍后再试";
            }
        }
        // 这里还可以添加对其他特定异常的处理，例如 else if (ex instanceof YourCustomException) { ... }

        // 设置响应状态码
        response.setStatusCode(httpStatus);

        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        errorResponse.put("code", httpStatus.value());
        errorResponse.put("message", errorMessage);
        errorResponse.put("data", null);

        // 将 Map 转换为 JSON 字符串并写入响应体
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBufferFactory bufferFactory = response.bufferFactory();
            DataBuffer buffer = bufferFactory.wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            // 如果 JSON 转换失败，记录日志并返回一个基本的错误
            return Mono.error(e);
        }
    }
}

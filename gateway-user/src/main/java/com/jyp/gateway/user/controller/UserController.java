package com.jyp.gateway.user.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    // 这个是之前的不需要认证的接口，保留
    @GetMapping("/{id}")
    public String findUserById(@PathVariable String id) {
        // 模拟从数据库中查找用户
        return "这是 [用户微服务]，查询到的用户ID是: " + id;
    }

    // ==========================================================
    // 【新增】一个需要认证才能访问的接口
    @GetMapping("/me")
    public String getCurrentUserInfo(@RequestHeader("X-User-ID") String userId) {
        // 由于有网关的保护，能访问到这里的请求一定是合法的
        // 我们可以直接信任并使用 X-User-ID 这个请求头
        return "这是 [用户微服务]，当前登录的用户ID是: " + userId;
    }
}

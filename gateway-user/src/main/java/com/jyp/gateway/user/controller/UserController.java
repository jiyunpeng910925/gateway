package com.jyp.gateway.user.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public String findUserById(@PathVariable String id) {
        // 模拟从数据库中查找用户
        return "这是 [用户微服务]，查询到的用户ID是: " + id;
    }
}

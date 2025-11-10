package com.jyp.gateway.api.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Token 过期时间，这里设置为 1 小时
    private static final long EXPIRE_TIME = 60 * 60 * 1000;
    // Token 私钥，在生产环境中应该更复杂且保存在安全的地方
    private static final String TOKEN_SECRET = "my-super-secret-key-for-jwt-12345";

    /**
     * 生成签名，1小时后过期
     */
    public static String createToken(String userId, String username) {
        Date date = new Date(System.currentTimeMillis() + EXPIRE_TIME);
        Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);

        // 附带用户信息
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("username", username)
                .withExpiresAt(date) // 过期时间
                .sign(algorithm);    // 加密
    }

    /**
     * 校验 Token 是否正确
     */
    public static boolean validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(TOKEN_SECRET);
            JWTVerifier verifier = JWT.require(algorithm).build();
            verifier.verify(token); // 校验
            return true;
        } catch (JWTVerificationException e) {
            logger.error("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从 Token 中获取 userId
     */
    public static String getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("userId").asString();
        } catch (Exception e) {
            return null;
        }
    }

    public static void main(String[] args) {
        // 模拟用户 "admin"，用户ID为 "1001"
        String token = JwtUtil.createToken("1001", "admin");
        System.out.println("Generated Token: " + token);
        // SpringApplication.run(ApiGatewayApplication.class, args);
    }
}

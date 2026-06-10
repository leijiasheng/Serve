package com.student.server.util;

import com.student.server.model.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    //生成token字符串
    public String generateToken(UserInfo userInfo) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(userInfo.getStudentNum())
                .claim("id", userInfo.getId())
                .claim("userName", userInfo.getUserName())
                .claim("studentNum", userInfo.getStudentNum())
                .claim("nickName", userInfo.getNickName())
                .claim("email", userInfo.getEmail())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    //解析token
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //校验token是否有效
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public UserInfo getUserInfoFromToken(String token) {
        Claims claims = parseToken(token);
        UserInfo userInfo = new UserInfo();
        userInfo.setId(claims.get("id", Long.class));
        userInfo.setUserName(claims.get("userName", String.class));
        userInfo.setStudentNum(claims.get("studentNum", String.class));
        userInfo.setNickName(claims.get("nickName", String.class));
        userInfo.setEmail(claims.get("email", String.class));
//        userInfo.setPersonSign(claims.get("personSign", String.class));
//        userInfo.setAvatar(claims.get("avatar", String.class));
        return userInfo;
    }
}

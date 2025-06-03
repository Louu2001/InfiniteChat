package com.lou.authenticationservice.utils;

import com.lou.authenticationservice.constants.config.ConfigEnum;
import com.lou.authenticationservice.constants.config.TimeOutEnum;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jodd.util.StringUtil;

import java.time.Duration;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName JwtUtil
 * @Description TODO
 * @Author Lou
 * @Date 2025/5/30 18:28
 */


public class JwtUtil {
    //生成Jwt
    private final static Duration expiration = Duration.ofHours(TimeOutEnum.JWT_TIME_OUT.getTimeOut());

    public static String generate(String userID) {
        Date expiryDate = new Date(System.currentTimeMillis() + expiration.toMillis());

        return Jwts.builder()
                .setSubject(userID)
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, ConfigEnum.TOKEN_SECRET_KEY.getText())
                .compact();
    }

    //解析Jwt
    public static Claims parse(String token) throws JwtException {
        if (StringUtil.isEmpty(token)) {
            throw new JwtException("token 为空");
        }

        //这个Claims对象包含了许多属性，比如签发时间、过期时间以及存放的数据等
        Claims claims = null;
        //解析失败了会抛出异常，所以我们要捕捉一下。token过期、token非法都会导致解析失败
        claims = Jwts.parser()
                .setSigningKey(ConfigEnum.TOKEN_SECRET_KEY.getValue())
                .parseClaimsJws(token)
                .getBody();

        return claims;
    }
}

package com.myplans.core;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class TestJwtHelper {

    public static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private TestJwtHelper() {}

    public static String tokenFor(String email, Integer idUsuario, List<String> roles) {
        return tokenFor(email, idUsuario, roles, 3600_000L);
    }

    public static String tokenFor(String email, Integer idUsuario, List<String> roles, long ttlMillis) {
        SecretKey key = signingKey();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of(
                        "id_usuario", idUsuario,
                        "roles", roles))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ttlMillis))
                .signWith(key)
                .compact();
    }

    public static String expiredTokenFor(String email, Integer idUsuario, List<String> roles) {
        SecretKey key = signingKey();
        return Jwts.builder()
                .subject(email)
                .claims(Map.of("id_usuario", idUsuario, "roles", roles))
                .issuedAt(new Date(System.currentTimeMillis() - 7200_000L))
                .expiration(new Date(System.currentTimeMillis() - 3600_000L))
                .signWith(key)
                .compact();
    }

    private static SecretKey signingKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
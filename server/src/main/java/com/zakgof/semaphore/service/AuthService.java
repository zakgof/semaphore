package com.zakgof.semaphore.service;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.zakgof.semaphore.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.JacksonDeserializer;
import io.jsonwebtoken.io.JacksonSerializer;

public class AuthService implements IAuthService {

    // Ultimate security with hardcoded keys...
    private static final byte[] keybytes = {-70, -112, 55, 72, -51, -120, -6, -31, -12, -96, -105, -56, 48, 5, -19, -55, -78, -115, -99, -25, 54, 54, 15, -63, -84, -55, 101, 3, 45, -43, 59, 92};
    private static final Key key = new SecretKeySpec(keybytes, 0, keybytes.length, SignatureAlgorithm.HS256.getJcaName());

    @Override
    public String login(Long id, String firstName, String lastName, String username, String authDate, String hash) {
        String displayname = firstName + (lastName != null ? " " + lastName : "") + (username != null ? " (" + username + ")" : "");
        // Ultimate security - 2: telegram login not validated !
        return Jwts.builder().setSubject("auth").setClaims(ImmutableMap.of("username", displayname, "id", id)).signWith(key).serializeToJsonWith(new JacksonSerializer(new ObjectMapper())).compact();
    }

    @Override
    public User parseUser(String jwt) {
        Claims claims = Jwts.parser().setSigningKey(key).deserializeJsonWith(new JacksonDeserializer(new ObjectMapper())).parseClaimsJws(jwt).getBody();
        String username = claims.get("username", String.class);
        Long telegramId = claims.get("id", Long.class);
        return new User(telegramId, username);
    }

}

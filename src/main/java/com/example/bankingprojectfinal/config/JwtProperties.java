package com.example.bankingprojectfinal.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JwtProperties {
    @Value("${jwt.expirationInMinutes}")
    Integer expirationInMinutes;

    @Value("${jwt.secret}")
    String secret;
}


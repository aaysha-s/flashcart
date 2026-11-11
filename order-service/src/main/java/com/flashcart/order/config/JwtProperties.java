package com.flashcart.order.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "flashcart.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer
) {}
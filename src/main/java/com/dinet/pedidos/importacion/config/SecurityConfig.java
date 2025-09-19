package com.dinet.pedidos.importacion.config;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.*;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint authEntry = (req, res, ex) ->
                writeJsonError(res, HttpStatus.UNAUTHORIZED.value(), "UNAUTHORIZED", "JWT requerido o inválido");
        AccessDeniedHandler denied = (req, res, ex) ->
                writeJsonError(res, HttpStatus.FORBIDDEN.value(), "FORBIDDEN", "Acceso denegado");

        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/swagger-ui/**","/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(authEntry)
                )
                .exceptionHandling(h -> h
                        .authenticationEntryPoint(authEntry)
                        .accessDeniedHandler(denied)
                )
                .build();
    }

    private void writeJsonError(HttpServletResponse res, int status, String code, String msg) throws java.io.IOException {
        String cid = MDC.get("correlationId");
        res.setStatus(status);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("""
      {"code":"%s","message":"%s","details":[],"correlationId":"%s"}
      """.formatted(code, msg, cid == null ? "" : cid));
    }

    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.hmac-secret}") String secret) {
        SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}

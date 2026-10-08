package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.demo.security.RecordAccessInterceptor;

/**
 * One CORS policy for the whole API. Browsers may only call it from the origins listed in
 * {@code app.cors.allowed-origins} (comma separated). Behind a reverse proxy that serves the website and the API
 * on one address no cross-origin call happens at all, and the default (local development only) is enough.
 */
@Configuration
public class WebSecurityConfig implements WebMvcConfigurer {
    private final String[] allowedOrigins;
    private final RecordAccessInterceptor accessInterceptor;

    public WebSecurityConfig(
            @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}") String[] allowedOrigins,
            RecordAccessInterceptor accessInterceptor) {
        this.allowedOrigins = allowedOrigins;
        this.accessInterceptor = accessInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept")
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(accessInterceptor).addPathPatterns("/api/patients/**", "/api/treatments/**",
                "/api/procedures/**", "/api/photos/**", "/api/prescriptions/**");
    }
}

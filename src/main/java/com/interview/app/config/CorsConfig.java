package com.interview.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Parse comma-separated values
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        List<String> methods = Arrays.asList(allowedMethods.split(","));
        
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(methods);
        config.setAllowedHeaders(Arrays.asList(allowedHeaders));
        config.setAllowCredentials(allowCredentials);
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}

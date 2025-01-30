package com.team2.mosoo_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String DEVELOP_FRONT_ADDRESS = "http://localhost:3000";

    private static final String DEPLOY_FRONT_URL = "https://mosooo.netlify.app";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
            .allowedOrigins(DEVELOP_FRONT_ADDRESS, DEPLOY_FRONT_URL)
            .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
            .exposedHeaders("location")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
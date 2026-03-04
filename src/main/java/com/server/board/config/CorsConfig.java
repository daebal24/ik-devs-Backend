package com.server.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/**")   // 모든 API 중에서
                .allowedOrigins("http://localhost:5000")  // 허용할 프론트 주소
                .allowedMethods("GET", "POST")//"PUT", "PATCH", "DELETE", "OPTIONS"
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}

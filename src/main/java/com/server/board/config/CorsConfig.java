    package com.server.board.config;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.web.servlet.config.annotation.CorsRegistry;
    import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

    @Configuration
    public class CorsConfig implements WebMvcConfigurer {

        @Value("${cors.allowed-origins}")
        private String[] allowedOrigins;

        @Override
        public void addCorsMappings(CorsRegistry registry) {

            registry.addMapping("/**")   // 모든 API 중에서
                    .allowedOrigins(allowedOrigins)  // 허용할 프론트 주소
                    .allowedMethods("GET", "POST", "OPTIONS")//"PUT", "PATCH", "DELETE",
                    .allowedHeaders("*")
                    .allowCredentials(true);
        }
    }

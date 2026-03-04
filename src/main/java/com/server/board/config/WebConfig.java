package com.server.board.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

//이미지 링크 접근 허용설정
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 프로젝트 루트 기준 ./images 폴더
        Path imageDir = Paths.get("").toAbsolutePath().resolve("multimedia");
        String imagePath = imageDir.toUri().toString();   // file:/D:/.../multimedia/ 형태

        System.out.println("Static image dir = " + imagePath);

        registry.addResourceHandler("/multimedia/**")
                .addResourceLocations(imagePath);
    }
}

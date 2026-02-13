package com.greenconnect.greenconnect_api.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 🔥 Cấu hình CORS cho cả Web và Mobile
        registry.addMapping("/**") // Áp dụng cho tất cả các đường dẫn (endpoints)
                
                // 🌐 QUAN TRỌNG: Sử dụng allowedOriginPatterns thay vì allowedOrigins
                // để hỗ trợ mobile app (Android/iOS) và web browser
                // Mobile app không gửi Origin header giống web, cần pattern matching
                .allowedOriginPatterns("*") // Cho phép mọi origin (bao gồm mobile)
                
                // Cho phép các phương thức HTTP này
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS") 
                
                // Cho phép tất cả các header (quan trọng cho mobile Authorization header)
                .allowedHeaders("*")
                
                // 🔥 Cho phép gửi credentials (cookies, authorization headers)
                // PHẢI set true để mobile app gửi được JWT token trong header
                .allowCredentials(true)
                
                // Cache preflight request kết quả 1 giờ
                .maxAge(3600);
    }
}
package com.greenconnect.greenconnect_api;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.TimeZone; // ✅ 1. Thêm import TimeZone

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct; // ✅ 2. Thêm import PostConstruct (Nếu lỗi đỏ dòng này thì đổi thành javax.annotation.PostConstruct)

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableAsync  // ⭐ ENABLE ASYNC để Elasticsearch sync hoạt động
@EnableScheduling  // ⭐ ENABLE SCHEDULING để tự động remove discount campaigns hết hạn
public class GreenconnectApiApplication {

    @Autowired
    private Environment env;

    public static void main(String[] args) {
        // ⭐ CRITICAL: Load .env TRƯỚC KHI Spring khởi động để có DB credentials
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .filename(".env")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> {
                if (System.getProperty(entry.getKey()) == null 
                    && System.getenv(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
            });
            System.out.println("✅ Loaded .env file BEFORE Spring starts");
        } catch (Exception e) {
            System.out.println("⚠️ Could not load .env file: " + e.getMessage());
        }

        // ⭐⭐⭐ CRITICAL: Set timezone TRƯỚC KHI Spring Boot khởi động ⭐⭐⭐
        // Phải đặt ở đây (không phải @PostConstruct) để Hibernate dùng đúng timezone
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        System.out.println("✅✅✅ JVM TIMEZONE SET TO: " + TimeZone.getDefault().getID() + " (BEFORE Spring starts) ✅✅✅");
        
        SpringApplication app = new SpringApplication(GreenconnectApiApplication.class);
        app.run(args);
    }

    // Giữ lại @PostConstruct để double-check và log
    @PostConstruct
    public void init() {
        // Double-check timezone đã được set đúng
        log.info("✅✅✅ VERIFIED TIMEZONE: {} (GMT+7) ✅✅✅", TimeZone.getDefault().getID());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            String protocol = "http";
            if (env.getProperty("server.ssl.key-store") != null) {
                protocol = "https";
            }
            
            String serverPort = env.getProperty("server.port", "8080");
            String contextPath = env.getProperty("server.servlet.context-path", "");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            
            log.info("----------------------------------------------------------");
            log.info("Application '{}' is running! Access URLs:", env.getProperty("spring.application.name"));
            log.info("Local: \t\t{}://localhost:{}{}", protocol, serverPort, contextPath);
            log.info("External: \t{}://{}:{}{}", protocol, hostAddress, serverPort, contextPath);
            log.info("Profile(s): \t{}", java.util.Arrays.toString(env.getActiveProfiles()));
            // In ra giờ hiện tại để kiểm tra luôn
            log.info("Current Server Time: {}", new java.util.Date()); 
            log.info("----------------------------------------------------------");
            
        } catch (UnknownHostException e) {
            log.warn("The host name could not be determined, using `localhost` as fallback");
        }
    }
}
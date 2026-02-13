package com.greenconnect.greenconnect_api.config;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class ApplicationEventLogger {

    private final ApplicationContext applicationContext;
    private final Environment environment;

    public ApplicationEventLogger(ApplicationContext applicationContext, Environment environment) {
        this.applicationContext = applicationContext;
        this.environment = environment;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void onApplicationStarted(ApplicationStartedEvent event) {
        log.info("Application started at: {}", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")));
        
        // Log active profiles
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            log.info("The following {} profile(s) are active: {}", 
                    activeProfiles.length, String.join(", ", activeProfiles));
        } else {
            log.info("No specific profiles set, using default configuration");
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady(ApplicationReadyEvent event) {
        log.info("Application startup completed successfully");
        
        // Log registered endpoints
        logRegisteredEndpoints();
        
        // Log important configurations
        logImportantConfigurations();
    }

    private void logRegisteredEndpoints() {
        try {
            RequestMappingHandlerMapping requestMappingHandlerMapping = 
                applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
            
            Map<RequestMappingInfo, HandlerMethod> map = requestMappingHandlerMapping.getHandlerMethods();
            
            log.info("Registered API endpoints:");
            map.forEach((key, value) -> {
                log.debug("Mapped \"{}\" onto {}", 
                    key, 
                    value.getMethod().getDeclaringClass().getSimpleName() + "." + value.getMethod().getName() + "()");
            });
            
            log.info("Total registered endpoints: {}", map.size());
            
        } catch (Exception e) {
            log.warn("Could not log endpoint mappings: {}", e.getMessage());
        }
    }

    private void logImportantConfigurations() {
        log.info("Important application configurations:");
        
        // Database configuration
        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (datasourceUrl != null) {
            log.info("Database URL: {}", maskSensitiveInfo(datasourceUrl));
        }
        
        // JPA configuration
        String jpaDialect = environment.getProperty("spring.jpa.properties.hibernate.dialect");
        if (jpaDialect != null) {
            log.info("JPA Dialect: {}", jpaDialect);
        }
        
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");
        if (ddlAuto != null) {
            log.info("JPA DDL Auto: {}", ddlAuto);
        }
        
        // Redis configuration
        String redisHost = environment.getProperty("spring.redis.host");
        String redisPort = environment.getProperty("spring.redis.port");
        if (redisHost != null && redisPort != null) {
            log.info("Redis Configuration: {}:{}", redisHost, redisPort);
        }
        
        // Firebase configuration
        String firebaseConfig = environment.getProperty("firebase.config-file");
        if (firebaseConfig != null) {
            log.info("Firebase Config File: {}", firebaseConfig);
        }
        
        // Security configuration
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret != null) {
            log.info("JWT Secret configured: {} characters", jwtSecret.length());
        }
    }

    private String maskSensitiveInfo(String info) {
        if (info == null || info.length() <= 10) {
            return "***";
        }
        return info.substring(0, 10) + "***" + info.substring(info.length() - 4);
    }
}
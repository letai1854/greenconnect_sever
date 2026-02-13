package com.greenconnect.greenconnect_api.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

import javax.sql.DataSource;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class DatabaseLogger {

    private final DataSource dataSource;

    public DatabaseLogger(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logDatabaseInfo() {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            log.info("----------------------------------------------------------");
            log.info("Database Connection Information:");
            log.info("Database Product Name: {}", metaData.getDatabaseProductName());
            log.info("Database Product Version: {}", metaData.getDatabaseProductVersion());
            log.info("Driver Name: {}", metaData.getDriverName());
            log.info("Driver Version: {}", metaData.getDriverVersion());
            log.info("Database URL: {}", connection.getMetaData().getURL());
            log.info("Connection Catalog: {}", connection.getCatalog());
            log.info("----------------------------------------------------------");
            
        } catch (Exception e) {
            log.error("Failed to retrieve database information: {}", e.getMessage());
        }
    }
}
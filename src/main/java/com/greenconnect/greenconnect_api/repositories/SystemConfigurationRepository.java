package com.greenconnect.greenconnect_api.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.SystemConfiguration;

/**
 * Repository cho SystemConfiguration
 */
@Repository
public interface SystemConfigurationRepository extends JpaRepository<SystemConfiguration, UUID> {
    
    /**
     * Tìm cấu hình theo config key
     */
    Optional<SystemConfiguration> findByConfigKey(String configKey);
}

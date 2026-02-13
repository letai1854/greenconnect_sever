package com.greenconnect.greenconnect_api.services.impl;

import com.greenconnect.greenconnect_api.dtos.request.UpdateChatbotPromptRequest;
import com.greenconnect.greenconnect_api.dtos.response.SystemConfigResponse;
import com.greenconnect.greenconnect_api.entities.SystemConfiguration;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.SystemConfigurationRepository;
import com.greenconnect.greenconnect_api.services.SystemConfigurationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationServiceImpl implements SystemConfigurationService {
    
    private final SystemConfigurationRepository systemConfigurationRepository;
    
    @Override
    public SystemConfigResponse getChatbotPrompt() {
        log.info("Getting chatbot prompt configuration");
        
        SystemConfiguration config = systemConfigurationRepository
                .findByConfigKey(SystemConfiguration.CHATBOT_PROMPT_KEY)
                .orElseThrow(() -> {
                    log.error("Chatbot prompt configuration not found");
                    return new BusinessException(ErrorCode.NOT_FOUND);
                });
        
        return mapToResponse(config);
    }
    
    @Override
    @Transactional
    public SystemConfigResponse updateChatbotPrompt(UpdateChatbotPromptRequest request) {
        log.info("Updating chatbot prompt configuration");
        
        // Validate: configValue không được null hoặc rỗng
        if (request.getConfigValue() == null || request.getConfigValue().trim().isEmpty()) {
            log.error("Config value is null or empty");
            throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
        
        // Tìm config hiện tại theo CHATBOT_PROMPT_KEY
        SystemConfiguration config = systemConfigurationRepository
                .findByConfigKey(SystemConfiguration.CHATBOT_PROMPT_KEY)
                .orElse(null);
        
        if (config == null) {
            // Tạo mới nếu chưa tồn tại
            log.info("Creating new chatbot prompt configuration");
            config = SystemConfiguration.builder()
                    .configKey(SystemConfiguration.CHATBOT_PROMPT_KEY)
                    .configValue(request.getConfigValue())
                    .build();
        } else {
            // Cập nhật nếu đã tồn tại (luôn chỉ có 1 dữ liệu duy nhất)
            log.info("Updating existing chatbot prompt configuration");
            config.setConfigValue(request.getConfigValue());
        }
        
        SystemConfiguration savedConfig = systemConfigurationRepository.save(config);
        log.info("Successfully saved chatbot prompt configuration");
        
        return mapToResponse(savedConfig);
    }
    
    /**
     * Map entity sang response DTO
     */
    private SystemConfigResponse mapToResponse(SystemConfiguration config) {
        return SystemConfigResponse.builder()
                .id(config.getId())
                .configKey(config.getConfigKey())
                .configValue(config.getConfigValue())
                .build();
    }
}

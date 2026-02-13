package com.greenconnect.greenconnect_api.services;

import com.greenconnect.greenconnect_api.dtos.request.UpdateChatbotPromptRequest;
import com.greenconnect.greenconnect_api.dtos.response.SystemConfigResponse;

/**
 * Service cho SystemConfiguration
 */
public interface SystemConfigurationService {
    
    /**
     * Lấy nội dung Chatbot Prompt (CHATBOT_PROMPT key)
     * @return SystemConfigResponse
     */
    SystemConfigResponse getChatbotPrompt();
    
    /**
     * Cập nhật hoặc tạo mới Chatbot Prompt (CHATBOT_PROMPT key)
     * - Nếu chưa tồn tại -> Tạo mới
     * - Nếu đã tồn tại -> Cập nhật
     * - Luôn chỉ có duy nhất 1 dữ liệu với key CHATBOT_PROMPT
     * 
     * @param request Request chứa configValue
     * @return SystemConfigResponse
     */
    SystemConfigResponse updateChatbotPrompt(UpdateChatbotPromptRequest request);
}

package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.UpdateChatbotPromptRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.SystemConfigResponse;
import com.greenconnect.greenconnect_api.services.SystemConfigurationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * System Configuration Controller
 * Quản lý cấu hình Chatbot Prompt (CHATBOT_PROMPT key)
 * 
 * Bảng này luôn chỉ có DUY NHẤT 1 dữ liệu với key CHATBOT_PROMPT
 * Chỉ ADMIN mới có quyền truy cập
 */
@RestController
@RequestMapping("/system-configurations")
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationController {
    
    private final SystemConfigurationService systemConfigurationService;
    
    /**
     * Lấy nội dung Chatbot Prompt
     * 
     * Endpoint: GET /system-configurations/chatbot-prompt
     * 
     * @return SystemConfigResponse (chứa configValue của CHATBOT_PROMPT)
     */
    @GetMapping("/chatbot-prompt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> getChatbotPrompt() {
        log.info("Admin getting chatbot prompt configuration");
        
        SystemConfigResponse response = systemConfigurationService.getChatbotPrompt();
        
        ApiResponse<SystemConfigResponse> apiResponse = ResponseUtil.success(
                response,
                "Lấy cấu hình Chatbot Prompt thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Cập nhật hoặc tạo mới Chatbot Prompt
     * 
     * Endpoint: PUT /system-configurations/chatbot-prompt
     * 
     * Luôn update vào đúng key CHATBOT_PROMPT (duy nhất 1 dữ liệu)
     * - Nếu chưa tồn tại -> Tạo mới
     * - Nếu đã tồn tại -> Cập nhật
     * - configValue không được null hoặc rỗng
     * 
     * @param request Request chứa configValue (nội dung prompt)
     * @return SystemConfigResponse
     */
    @PutMapping("/chatbot-prompt")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigResponse>> updateChatbotPrompt(
            @Valid @RequestBody UpdateChatbotPromptRequest request) {
        log.info("Admin updating chatbot prompt configuration");
        
        SystemConfigResponse response = systemConfigurationService.updateChatbotPrompt(request);
        
        ApiResponse<SystemConfigResponse> apiResponse = ResponseUtil.success(
                response,
                "Cập nhật cấu hình Chatbot Prompt thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
}

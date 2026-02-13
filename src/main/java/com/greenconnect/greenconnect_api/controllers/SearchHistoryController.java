package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.SaveSearchHistoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.dtos.response.SearchHistoryResponse;
import com.greenconnect.greenconnect_api.services.SearchHistoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller quản lý lịch sử tìm kiếm
 * Chỉ cho phép CUSTOMER sử dụng
 */
@RestController
@RequestMapping("/search-history")
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryController {
    
    private final SearchHistoryService searchHistoryService;
    
    /**
     * Lưu lịch sử tìm kiếm (CUSTOMER only)
     * 
     * @param request Nội dung tìm kiếm
     * @param authentication Thông tin user đăng nhập
     * @return ApiResponse
     */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> saveSearchHistory(
            @Valid @RequestBody SaveSearchHistoryRequest request,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("💾 Customer {} lưu lịch sử tìm kiếm: '{}'", userEmail, request.getSearchKeyword());
        
        searchHistoryService.saveSearchHistory(request, userEmail);
        
        ApiResponse<Void> response = ResponseUtil.success(null, "Đã lưu lịch sử tìm kiếm");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Lấy 3 lịch sử tìm kiếm mới nhất (CUSTOMER only)
     * 
     * @param authentication Thông tin user đăng nhập
     * @return Danh sách 3 tìm kiếm mới nhất
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<SearchHistoryResponse>>> getRecentSearches(
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("🔍 Customer {} lấy 3 tìm kiếm mới nhất", userEmail);
        
        List<SearchHistoryResponse> searches = searchHistoryService.getRecentSearches(userEmail);
        
        ApiResponse<List<SearchHistoryResponse>> response = ResponseUtil.success(
                searches, 
                "Lấy lịch sử tìm kiếm thành công"
        );
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa 1 lịch sử tìm kiếm theo ID (CUSTOMER only)
     * 
     * @param searchHistoryId ID của lịch sử tìm kiếm cần xóa
     * @param authentication Thông tin user đăng nhập
     * @return ApiResponse
     */
    @DeleteMapping("/{searchHistoryId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteSearchHistory(
            @PathVariable UUID searchHistoryId,
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("🗑️ Customer {} xóa lịch sử tìm kiếm ID: {}", userEmail, searchHistoryId);
        
        searchHistoryService.deleteSearchHistory(searchHistoryId, userEmail);
        
        ApiResponse<Void> response = ResponseUtil.success(null, "Đã xóa lịch sử tìm kiếm");
        return ResponseEntity.ok(response);
    }
    
    /**
     * Xóa tất cả lịch sử tìm kiếm (CUSTOMER only)
     * 
     * @param authentication Thông tin user đăng nhập
     * @return ApiResponse
     */
    @DeleteMapping("/all")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<Void>> deleteAllSearchHistory(
            Authentication authentication) {
        
        String userEmail = authentication.getName();
        log.info("🗑️ Customer {} xóa tất cả lịch sử tìm kiếm", userEmail);
        
        searchHistoryService.deleteAllSearchHistory(userEmail);
        
        ApiResponse<Void> response = ResponseUtil.success(null, "Đã xóa tất cả lịch sử tìm kiếm");
        return ResponseEntity.ok(response);
    }
}

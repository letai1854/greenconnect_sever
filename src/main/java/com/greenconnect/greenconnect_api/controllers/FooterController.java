package com.greenconnect.greenconnect_api.controllers;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.FooterBulkUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterLinkRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterSectionRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterBulkUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterLinkResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterSectionResponse;
import com.greenconnect.greenconnect_api.services.FooterService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/footer")
@RequiredArgsConstructor
@Slf4j
public class FooterController {
    
    private final FooterService footerService;
    
    /**
     * GET - Lấy toàn bộ footer (public)
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<FooterResponse>> getAllFooter(
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive) {
        
        log.info("GET /api/v1/footer - includeInactive: {}", includeInactive);
        
        FooterResponse response = footerService.getAllFooter(includeInactive);
        
        return ResponseEntity.ok(ApiResponse.<FooterResponse>builder()
            .code(1000)
            .message("Lấy dữ liệu footer thành công")
            .data(response)
            .build());
    }
    
    /**
     * POST - Tạo section mới (Admin only)
     */
    @PostMapping("/sections")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FooterSectionResponse>> createSection(
            @Valid @RequestBody FooterSectionRequest request) {
        
        log.info("POST /api/v1/footer/sections - name: {}", request.getName());
        
        FooterSectionResponse response = footerService.createSection(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<FooterSectionResponse>builder()
                .code(1000)
                .message("Tạo nhóm footer thành công")
                .data(response)
                .build());
    }
    
    /**
     * PUT - Cập nhật section (Admin only)
     */
    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FooterSectionResponse>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody FooterSectionRequest request) {
        
        log.info("PUT /api/v1/footer/sections/{} - name: {}", sectionId, request.getName());
        
        FooterSectionResponse response = footerService.updateSection(sectionId, request);
        
        return ResponseEntity.ok(ApiResponse.<FooterSectionResponse>builder()
            .code(1000)
            .message("Cập nhật nhóm footer thành công")
            .data(response)
            .build());
    }
    
    /**
     * DELETE - Xóa section (Admin only)
     */
    @DeleteMapping("/sections/{sectionId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteSection(@PathVariable UUID sectionId) {
        
        log.info("DELETE /api/v1/footer/sections/{}", sectionId);
        
        footerService.deleteSection(sectionId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .code(1000)
            .message("Xóa nhóm footer thành công")
            .build());
    }
    
    /**
     * POST - Tạo link trong section (Admin only)
     */
    @PostMapping("/sections/{sectionId}/links")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FooterLinkResponse>> createLink(
            @PathVariable UUID sectionId,
            @Valid @RequestBody FooterLinkRequest request) {
        
        log.info("POST /api/v1/footer/sections/{}/links - iconKey: {}", sectionId, request.getIconKey());
        
        FooterLinkResponse response = footerService.createLink(sectionId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.<FooterLinkResponse>builder()
                .code(1000)
                .message("Thêm nội dung footer thành công")
                .data(response)
                .build());
    }
    
    /**
     * PUT - Cập nhật link (Admin only)
     */
    @PutMapping("/links/{linkId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FooterLinkResponse>> updateLink(
            @PathVariable UUID linkId,
            @Valid @RequestBody FooterLinkRequest request) {
        
        log.info("PUT /api/v1/footer/links/{} - iconKey: {}", linkId, request.getIconKey());
        
        FooterLinkResponse response = footerService.updateLink(linkId, request);
        
        return ResponseEntity.ok(ApiResponse.<FooterLinkResponse>builder()
            .code(1000)
            .message("Cập nhật nội dung footer thành công")
            .data(response)
            .build());
    }
    
    /**
     * DELETE - Xóa link (Admin only)
     */
    @DeleteMapping("/links/{linkId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteLink(@PathVariable UUID linkId) {
        
        log.info("DELETE /api/v1/footer/links/{}", linkId);
        
        footerService.deleteLink(linkId);
        
        return ResponseEntity.ok(ApiResponse.<Void>builder()
            .code(1000)
            .message("Xóa nội dung footer thành công")
            .build());
    }
    
    /**
     * PUT - Bulk update (Lưu tất cả) (Admin only)
     */
    @PutMapping("/bulk-update")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<FooterBulkUpdateResponse>> bulkUpdate(
            @Valid @RequestBody FooterBulkUpdateRequest request) {
        
        log.info("footer/bulk-update - sections: {}", request.getSections().size());
        
        FooterBulkUpdateResponse response = footerService.bulkUpdate(request);
        
        return ResponseEntity.ok(ApiResponse.<FooterBulkUpdateResponse>builder()
            .code(1000)
            .message("Cập nhật footer thành công")
            .data(response)
            .build());
    }
}

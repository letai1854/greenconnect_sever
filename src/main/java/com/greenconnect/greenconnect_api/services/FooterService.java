package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.FooterBulkUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterLinkRequest;
import com.greenconnect.greenconnect_api.dtos.request.FooterSectionRequest;
import com.greenconnect.greenconnect_api.dtos.response.FooterBulkUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterLinkResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterResponse;
import com.greenconnect.greenconnect_api.dtos.response.FooterSectionResponse;

public interface FooterService {
    
    /**
     * GET - Lấy toàn bộ footer (sections + links)
     */
    FooterResponse getAllFooter(boolean includeInactive);
    
    /**
     * POST - Tạo section mới
     */
    FooterSectionResponse createSection(FooterSectionRequest request);
    
    /**
     * PUT - Cập nhật section
     */
    FooterSectionResponse updateSection(UUID sectionId, FooterSectionRequest request);
    
    /**
     * DELETE - Xóa section (cascade xóa links)
     */
    void deleteSection(UUID sectionId);
    
    /**
     * POST - Tạo link trong section
     */
    FooterLinkResponse createLink(UUID sectionId, FooterLinkRequest request);
    
    /**
     * PUT - Cập nhật link
     */
    FooterLinkResponse updateLink(UUID linkId, FooterLinkRequest request);
    
    /**
     * DELETE - Xóa link
     */
    void deleteLink(UUID linkId);
    
    /**
     * PUT - Bulk update (Lưu tất cả)
     */
    FooterBulkUpdateResponse bulkUpdate(FooterBulkUpdateRequest request);
}

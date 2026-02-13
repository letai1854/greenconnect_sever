package com.greenconnect.greenconnect_api.controllers;

import com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignCreateRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.PromotionCampaignResponse;
import com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.services.PromotionCampaignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
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

@RestController
@RequestMapping("/promotion-campaigns")
@RequiredArgsConstructor
@Slf4j
public class PromotionCampaignController {

    private final PromotionCampaignService campaignService;
    private final ObjectMapper objectMapper;

    /**
     * Tạo campaign mới với danh sách sản phẩm
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<PromotionCampaignResponse>> createCampaign(
            @Valid @RequestBody PromotionCampaignCreateRequest request) {
        log.info("Tạo promotion campaign mới: {}", request.getCampaignName());
        
        // ServiceImpl TỰ ĐỘNG apply discount cho variants
        PromotionCampaignResponse response = campaignService.createCampaign(request);
        
        return new ResponseEntity<>(
            ResponseUtil.success(response, "Tạo promotion campaign thành công"), 
            HttpStatus.CREATED
        );
    }

    /**
     * Lấy chi tiết campaign theo ID
     */
    @GetMapping("/get/{campaignId}")
    public ResponseEntity<ApiResponse<PromotionCampaignResponse>> getCampaignById(@PathVariable UUID campaignId) {
        log.info("Lấy campaign với ID: {}", campaignId);
        PromotionCampaignResponse response = campaignService.getCampaignById(campaignId);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy thông tin campaign thành công"));
    }

    /**
     * Lấy campaign với product count theo ID
     */
    @GetMapping("/get/campaigns/{campaignId}")
    public ResponseEntity<ApiResponse<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse>> getCampaignWithCount(@PathVariable UUID campaignId) {
        log.info("Lấy campaign với product count, ID: {}", campaignId);
        com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse response = campaignService.getCampaignWithCountById(campaignId);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy campaign thành công"));
    }

    /**
     * Update campaign (partial update)
     */
    @PutMapping("/update/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<PromotionCampaignResponse>> updateCampaignPartial(
            @PathVariable UUID campaignId,
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest request) {
        log.info("Cập nhật campaign với ID: {}", campaignId);
        
        // ServiceImpl TỰ ĐỘNG update discount cho variants
        PromotionCampaignResponse response = campaignService.updateCampaignPartial(campaignId, request);
        
        return ResponseEntity.ok(ResponseUtil.success(response, "Cập nhật campaign thành công"));
    }

    /**
     * Thêm sản phẩm vào campaign (append)
     */
    @PostMapping("/add/{campaignId}/products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<PromotionCampaignResponse>> addProductsToCampaign(
            @PathVariable UUID campaignId,
            @RequestBody JsonNode body) {
        log.info("Thêm sản phẩm vào campaign: {}", campaignId);
        
        try {
            List<com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem> products = 
                objectMapper.convertValue(
                    body.get("promotionProducts"), 
                    new TypeReference<List<com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem>>() {}
                );
            
            // ServiceImpl TỰ ĐỘNG apply discount cho variants
            PromotionCampaignResponse response = campaignService.addProductsToCampaign(campaignId, products);
            
            return ResponseEntity.ok(ResponseUtil.success(response, "Thêm sản phẩm vào campaign thành công"));
        } catch (Exception e) {
            log.error("Lỗi khi thêm sản phẩm: {}", e.getMessage());
            throw new IllegalArgumentException("Dữ liệu không hợp lệ: " + e.getMessage());
        }
    }

    /**
     * Thêm danh sách sản phẩm (List UUID) vào campaign
     */
    @PostMapping("/add/{campaignId}/products/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<PromotionCampaignResponse>> addProductsByIds(
            @PathVariable UUID campaignId,
            @RequestBody List<UUID> productIds) {
        log.info("Thêm {} sản phẩm vào campaign: {}", productIds.size(), campaignId);
        
        // ServiceImpl TỰ ĐỘNG apply discount cho variants
        PromotionCampaignResponse response = campaignService.addProductsByIds(campaignId, productIds);
        
        return ResponseEntity.ok(ResponseUtil.success(response, 
            String.format("Thêm %d sản phẩm vào campaign thành công", productIds.size())));
    }

    /**
     * Xóa danh sách sản phẩm (List UUID) khỏi campaign
     */
    @DeleteMapping("/remove/{campaignId}/products/batch")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeProductsByIds(
            @PathVariable UUID campaignId,
            @RequestBody List<UUID> productIds) {
        log.info("Xóa {} sản phẩm khỏi campaign: {}", productIds.size(), campaignId);
        
        // ServiceImpl TỰ ĐỘNG recalculate discount
        campaignService.removeProductsByIds(campaignId, productIds);
        
        return ResponseEntity.ok(ResponseUtil.success(null, 
            String.format("Xóa %d sản phẩm khỏi campaign thành công", productIds.size())));
    }

    /**
     * Xóa TẤT CẢ sản phẩm khỏi campaign
     */
    @DeleteMapping("/removeall/{campaignId}/products")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeAllProductsFromCampaign(@PathVariable UUID campaignId) {
        log.info("Xóa tất cả sản phẩm khỏi campaign: {}", campaignId);
        
        // ServiceImpl TỰ ĐỘNG remove discount từ variants
        campaignService.removeAllProductsFromCampaign(campaignId);
        
        return ResponseEntity.ok(ResponseUtil.success(null, "Xóa tất cả sản phẩm thành công"));
    }

    /**
     * Xóa 1 sản phẩm khỏi campaign
     */
    @DeleteMapping("/remove/{campaignId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeProductFromCampaign(
            @PathVariable UUID campaignId,
            @PathVariable UUID productId) {
        log.info("Xóa sản phẩm {} khỏi campaign {}", productId, campaignId);
        
        campaignService.removeProductFromCampaign(campaignId, productId);
        
        // Note: Không auto-remove discount vì có thể product còn trong campaigns khác
        log.warn("⚠️ Lưu ý: Discount của product variants không tự động remove. Kiểm tra các campaigns khác.");
        
        return ResponseEntity.ok(ResponseUtil.success(null, "Xóa sản phẩm khỏi campaign thành công"));
    }
    
    /**
     * Apply discount cho tất cả variants của products trong campaign
     */
    @PostMapping("/apply-discount/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> applyDiscountToVariants(@PathVariable UUID campaignId) {
        log.info("Apply discount cho variants của campaign: {}", campaignId);
        
        campaignService.applyDiscountToProductVariants(campaignId);
        
        return ResponseEntity.ok(ResponseUtil.success(null, "Apply discount thành công"));
    }
    
    /**
     * Remove discount từ tất cả variants của products trong campaign
     */
    @PostMapping("/remove-discount/{campaignId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> removeDiscountFromVariants(@PathVariable UUID campaignId) {
        log.info("Remove discount từ variants của campaign: {}", campaignId);
        
        campaignService.removeDiscountFromProductVariants(campaignId);
        
        return ResponseEntity.ok(ResponseUtil.success(null, "Remove discount thành công"));
    }

    // ========== PAGINATION ENDPOINTS ==========

    /**
     * Lấy danh sách campaigns đang hoạt động với phân trang sắp xếp theo thời gian tạo
     */
    @GetMapping("/active/latest/page")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getActiveCampaignsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Lấy danh sách campaigns active với phân trang - page: {}, size: {}", page, size);

        Page<PromotionCampainCountResponse> response = campaignService.getActiveCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns thành công"));
    }
    
    /**
     * Lấy danh sách campaigns không hoạt động với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/inactive/latest/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getInactiveCampaignsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Lấy danh sách campaigns inactive với phân trang - page: {}, size: {}", page, size);
        
        Page<PromotionCampainCountResponse> response = campaignService.getInactiveCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns không hoạt động thành công"));
    }
    
    /**
     * Lấy tất cả campaigns với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/all/latest/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getAllCampaignsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("� Lấy tất cả campaigns với phân trang - page: {}, size: {}", page, size);
        
        Page<PromotionCampainCountResponse> response = campaignService.getAllCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns thành công"));
    }
    
    /**
     * Lấy danh sách campaigns còn hiệu lực (đang chạy) với phân trang
     */
    @GetMapping("/admin/valid/latest/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getValidCampaignsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("� Lấy danh sách campaigns còn hiệu lực với phân trang - page: {}, size: {}", page, size);
        
        Page<PromotionCampainCountResponse> response = campaignService.getValidCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns còn hiệu lực thành công"));
    }
    @GetMapping("/customer/valid/latest/page")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getValidCampaignsPaginatedForCustomerByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Lấy danh sách campaigns còn hiệu lực với phân trang - page: {}, size: {}", page, size);
        
        Page<PromotionCampainCountResponse> response = campaignService.getValidCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns còn hiệu lực thành công"));
    }
    
    /**
     * Lấy campaign mới nhất loại BULK_PURCHASE (Ưu đãi mua nhiều) còn hiệu lực
     * Endpoint này dành cho customer hiển thị banner/section ưu đãi mua nhiều
     */
    @GetMapping("/customer/bulk-purchase/latest")
    public ResponseEntity<ApiResponse<PromotionCampainCountResponse>> getLatestBulkPurchaseCampaign() {
        log.info("🛒 Lấy campaign BULK_PURCHASE mới nhất còn hiệu lực");
        
        PromotionCampainCountResponse response = campaignService.getLatestValidBulkPurchaseCampaign();
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy campaign ưu đãi mua nhiều thành công"));
    }
    /**
     * Lấy danh sách campaigns đã hết hạn với phân trang
     */
    @GetMapping("/admin/expired/latest/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<PromotionCampainCountResponse>>> getExpiredCampaignsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("📄 Lấy danh sách campaigns đã hết hạn với phân trang - page: {}, size: {}", page, size);
        
        Page<PromotionCampainCountResponse> response = campaignService.getExpiredCampaignsPaginatedByCreatedAt(page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, "Lấy danh sách campaigns đã hết hạn thành công"));
    }

    /**
     * Tìm kiếm campaigns với phân trang - Hỗ trợ tiếng Việt có dấu
     * @param keyword Từ khóa tìm kiếm (tên campaign, description, slug)
     * @param tab Trạng thái: active, inactive, all, valid, expired
     * @param page Số trang (mặc định 0)
     * @param size Số lượng/trang (mặc định 20)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('MARKETING_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<PromotionCampaignResponse>>> searchCampaigns(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔍 Tìm kiếm campaigns - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        // Validate tab
        if (!tab.matches("^(active|inactive|all|valid|expired)$")) {
            return ResponseEntity.badRequest()
                .body(ResponseUtil.error(null, "Tab không hợp lệ. Chỉ chấp nhận: active, inactive, all, valid, expired"));
        }
        
        Page<PromotionCampaignResponse> response = campaignService.searchCampaigns(keyword, tab, page, size);
        return ResponseEntity.ok(ResponseUtil.success(response, 
            "Tìm kiếm campaigns thành công - Tìm thấy " + response.getTotalElements() + " kết quả"));
    }

    /**
     * Lấy danh sách products với phân trang, đánh dấu products đã có trong campaign
     * Endpoint này dùng để hiển thị danh sách products khi thêm/xóa products vào campaign
     * 
     * @param campaignId UUID của campaign
     * @param categoryId UUID của category (optional - null = tất cả categories)
     * @param page Số trang (mặc định 0)
     * @param size Kích thước trang (mặc định 20)
     * @return Page<ProductInCampaignResponse> với flag isInCampaign
     * 
     * @apiNote Response bao gồm:
     * - Tất cả thông tin của Product (ProductResponse)
     * - isInCampaign: true nếu product đã trong campaign
     * - promotionProductId: UUID của product (vì composite key)
     * - productDiscountValue: Discount riêng của product (nếu có override)
     */
    @GetMapping("/{campaignId}/products/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<com.greenconnect.greenconnect_api.dtos.response.ProductInCampaignResponse>>> getProductsForCampaign(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String categoryInfo = categoryId != null ? "category: " + categoryId : "ALL categories";
        log.info("📦 Lấy danh sách products cho campaign {} - {} - page: {}, size: {}", 
            campaignId, categoryInfo, page, size);
        
        Page<com.greenconnect.greenconnect_api.dtos.response.ProductInCampaignResponse> response = 
            campaignService.getProductsWithCampaignStatus(campaignId, categoryId, page, size);
        
        long inCampaignCount = response.getContent().stream()
            .filter(com.greenconnect.greenconnect_api.dtos.response.ProductInCampaignResponse::getIsInCampaign)
            .count();
        
        String message = String.format(
            "Lấy danh sách products thành công - Tổng: %d, Đã trong campaign: %d",
            response.getTotalElements(),
            inCampaignCount
        );
        
        return ResponseEntity.ok(ResponseUtil.success(response, message));
    }



    /**
     * Lấy danh sách TẤT CẢ products với phân trang theo category
     * Endpoint này dùng để hiển thị danh sách products đơn giản
     * 
     * @param categoryId UUID của category (optional - null = tất cả categories)
     * @param page Số trang (mặc định 0)
     * @param size Kích thước trang (mặc định 20)
     * @return Page<ProductResponse> - TẤT CẢ products theo category/all
     * 
     * @apiNote Response chỉ bao gồm ProductResponse đơn giản
     */
    @GetMapping("/products/all/page")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MARKETING_MANAGER')")
    public ResponseEntity<ApiResponse<Page<com.greenconnect.greenconnect_api.dtos.response.ProductResponse>>> getAllProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        String categoryInfo = categoryId != null ? "category: " + categoryId : "ALL categories";
        log.info("📦 Lấy danh sách TẤT CẢ products - {} - page: {}, size: {}", 
            categoryInfo, page, size);
        
        Page<com.greenconnect.greenconnect_api.dtos.response.ProductResponse> response = 
            campaignService.getAllProductsByCategory(categoryId, page, size);
        
        String message = String.format(
            "Lấy danh sách products thành công - Tổng: %d products",
            response.getNumberOfElements()
        );
        
        return ResponseEntity.ok(ResponseUtil.success(response, message));
    }
}

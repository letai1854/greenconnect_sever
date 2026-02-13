package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.BatchPriceUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateProductRequest;
import com.greenconnect.greenconnect_api.dtos.request.FilterProductsRequest;
import com.greenconnect.greenconnect_api.dtos.request.ProductFilterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRatingRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.BatchPriceUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductImportResultResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductWithTopReviewResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.security.CustomUserPrincipal;
import com.greenconnect.greenconnect_api.services.ProductImportService;
import com.greenconnect.greenconnect_api.services.ProductService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

/**
 * Product Controller - Quản lý sản phẩm
 */
@RestController
@RequestMapping("/products")
@Slf4j
public class ProductController {
    
    private final ProductService productService;
    private final ProductImportService productImportService;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY
    private ElasticsearchSyncService elasticsearchSyncService;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    // Constructor for ProductService and ProductImportService (required dependencies)
    public ProductController(ProductService productService, ProductImportService productImportService) {
        this.productService = productService;
        this.productImportService = productImportService;
    }
    
    /**
     * Fix mainImageUrl for existing variants
     */
    @PostMapping("/fix-main-image-urls")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> fixMainImageUrls() {
        log.info("Fixing mainImageUrl for existing variants");
        
        int updatedCount = productService.fixMainImageUrls();
        ApiResponse<String> apiResponse = ResponseUtil.success(
            "Fixed " + updatedCount + " variants", 
            "Cập nhật mainImageUrl thành công cho " + updatedCount + " variants"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Tạo sản phẩm mới với variants và images
     */
    @PostMapping("/create")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        log.info("Tạo sản phẩm mới: {}", request.getName());
        
        ProductResponse response = productService.createProduct(request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(response.getId());
                log.info("✅ Đã sync product {} vào Elasticsearch", response.getId());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        ApiResponse<ProductResponse> apiResponse = ResponseUtil.success(
            response, 
            "Tạo sản phẩm thành công"
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
    
    /**
     * 💰 Cập nhật giá và % giảm giá hàng loạt cho nhiều biến thể sản phẩm
     * 
     * <p><b>Endpoint:</b> POST /products/updateflash</p>
     * 
     * <p><b>Kịch bản:</b></p>
     * <ul>
     *   <li>Admin/Product Manager cần cập nhật giá flash sale cho nhiều sản phẩm</li>
     *   <li>Hỗ trợ cập nhật cả giá gốc và % giảm giá trong một request</li>
     *   <li>Idempotent: gọi nhiều lần với cùng data không gây side effects</li>
     * </ul>
     * 
     * @param request Danh sách các biến thể cần cập nhật với giá/% giảm giá mới
     * @return BatchPriceUpdateResponse chứa kết quả cập nhật (thành công/thất bại)
     */
    @PostMapping("/updateflash")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<BatchPriceUpdateResponse>> batchUpdatePrices(
            @Valid @RequestBody BatchPriceUpdateRequest request) {
        log.info("💰 Nhận yêu cầu cập nhật giá hàng loạt cho {} biến thể", request.getUpdates().size());
        
        BatchPriceUpdateResponse result = productService.batchUpdatePrices(request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH - Sync tất cả products đã được cập nhật thành công
        if (elasticsearchSyncService != null && result.getSuccessCount() > 0) {
            // Lấy danh sách unique productIds từ request (loại bỏ những cái failed)
            java.util.Set<UUID> failedVariantIds = result.getFailures() != null 
                ? result.getFailures().stream()
                    .map(f -> f.getVariantId())
                    .collect(java.util.stream.Collectors.toSet())
                : java.util.Collections.emptySet();
            
            java.util.Set<UUID> successProductIds = request.getUpdates().stream()
                .filter(u -> !failedVariantIds.contains(u.getVariantId()))
                .map(u -> u.getProductId())
                .collect(java.util.stream.Collectors.toSet());
            
            log.info("🔄 Syncing {} products to Elasticsearch after batch price update", successProductIds.size());
            
            for (UUID productId : successProductIds) {
                try {
                    elasticsearchSyncService.syncProduct(productId);
                    log.debug("✅ Đã sync product {} vào Elasticsearch", productId);
                } catch (Exception e) {
                    log.warn("⚠️ Lỗi sync Elasticsearch cho product {}: {}", productId, e.getMessage());
                }
            }
            
            log.info("✅ Hoàn tất sync {} products vào Elasticsearch", successProductIds.size());
        } else if (elasticsearchSyncService == null) {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        // Tạo message phù hợp
        String message;
        if (result.getFailedCount() == 0) {
            message = String.format("Cập nhật giá thành công cho %d biến thể", result.getSuccessCount());
        } else if (result.getSuccessCount() == 0) {
            message = String.format("Cập nhật giá thất bại cho tất cả %d biến thể", result.getFailedCount());
        } else {
            message = String.format("Cập nhật giá hoàn tất: %d thành công, %d thất bại", 
                    result.getSuccessCount(), result.getFailedCount());
        }
        
        ApiResponse<BatchPriceUpdateResponse> apiResponse = ResponseUtil.success(result, message);
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Import sản phẩm từ file Excel
     * 
     * Quy trình:
     * 1. Nhận file Excel từ frontend (MultipartFile)
     * 2. Parse Excel -> Gom nhóm theo Mã nhóm (RefId)
     * 3. Validate dữ liệu (format, tham chiếu, logic nghiệp vụ)
     * 4. Lưu vào MySQL Database
     * 5. Sync vào Elasticsearch (nếu có)
     * 6. Trả về kết quả tổng hợp: {success: X, failed: Y, errors: [...]}
     * 
     * Chiến lược validate:
     * - Gom nhóm trước (các dòng cùng Mã nhóm = 1 sản phẩm)
     * - Validate sau (nếu 1 biến thể sai -> từ chối toàn bộ sản phẩm)
     * - 3 cấp độ lỗi:
     *   + Lỗi định dạng (Format Error): Cột số nhập chữ, thiếu trường bắt buộc
     *   + Lỗi tham chiếu (Reference Error): Danh mục/NCC không tồn tại
     *   + Lỗi logic nghiệp vụ (Business Logic): Giá âm, không có variant mặc định
     * 
     * @param file File Excel chứa dữ liệu sản phẩm
     * @return Kết quả import (thành công/thất bại cho từng sản phẩm)
     */
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<ProductImportResultResponse>> importProducts(
            @RequestParam("file") MultipartFile file) {
        log.info("📥 Nhận yêu cầu import sản phẩm từ file: {}", file.getOriginalFilename());
        
        // Validate file type
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            log.warn("⚠️ File không đúng định dạng: {}", filename);
            ApiResponse<ProductImportResultResponse> errorResponse = ResponseUtil.error(
                    ErrorCode.INVALID_REQUEST, 
                    "File phải có định dạng Excel (.xlsx hoặc .xls)");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            log.warn("⚠️ File quá lớn: {} bytes", file.getSize());
            ApiResponse<ProductImportResultResponse> errorResponse = ResponseUtil.error(
                    ErrorCode.INVALID_REQUEST, 
                    "Kích thước file không được vượt quá 10MB");
            return ResponseEntity.badRequest().body(errorResponse);
        }
        
        // Import sản phẩm (Service đã tự động sync Elasticsearch cho từng sản phẩm thành công)
        ProductImportResultResponse result = productImportService.importProductsFromExcel(file);
        
        // Tạo message phù hợp
        String message;
        if (result.getFailedCount() == 0) {
            message = String.format("Import thành công %d sản phẩm", result.getSuccessCount());
        } else if (result.getSuccessCount() == 0) {
            message = String.format("Import thất bại %d sản phẩm", result.getFailedCount());
        } else {
            message = String.format("Import hoàn tất: %d thành công, %d thất bại", 
                    result.getSuccessCount(), result.getFailedCount());
        }
        
        ApiResponse<ProductImportResultResponse> apiResponse = ResponseUtil.success(result, message);
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy chi tiết thông tin sản phẩm theo ID
     */
    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID productId) {
        log.info("Lấy chi tiết sản phẩm với ID: {}", productId);
        
        ProductResponse response = productService.getProductById(productId);
        
        // 🔄 Track detail view in Recombee (chỉ khi user đã đăng nhập)
        if (recombeeSyncService != null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserPrincipal) {
                    CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
                    recombeeSyncService.trackDetailView(principal.getUserId(), productId);
                    log.info("✅ [RECOMBEE TRACKING] Detail view tracked → user={}, product={}", principal.getUserId(), productId);
                } else {
                    log.debug("ℹ️ [RECOMBEE] User not authenticated - Detail view tracking skipped");
                }
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track detail view: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ [RECOMBEE] Service not available - Detail view tracking skipped");
        }
        
        ApiResponse<ProductResponse> apiResponse = ResponseUtil.success(
            response, 
            "Lấy thông tin sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy chi tiết sản phẩm active (public API)
     */
    @GetMapping("/public/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> getActiveProductById(@PathVariable UUID productId) {
        log.info("Lấy chi tiết sản phẩm active với ID: {}", productId);
        
        ProductResponse response = productService.getActiveProductById(productId);
        
        // 🔄 Track detail view in Recombee (chỉ khi user đã đăng nhập)
        if (recombeeSyncService != null) {
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserPrincipal) {
                    CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
                    recombeeSyncService.trackDetailView(principal.getUserId(), productId);
                    log.info("✅ [RECOMBEE TRACKING] Detail view tracked → user={}, product={}", principal.getUserId(), productId);
                } else {
                    log.debug("ℹ️ [RECOMBEE] User not authenticated - Detail view tracking skipped");
                }
            } catch (Exception e) {
                log.warn("⚠️ [RECOMBEE TRACKING] Failed to track detail view: {}", e.getMessage());
            }
        } else {
            log.warn("⚠️ [RECOMBEE] Service not available - Detail view tracking skipped");
        }
        
        ApiResponse<ProductResponse> apiResponse = ResponseUtil.success(
            response, 
            "Lấy thông tin sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Cập nhật thông tin sản phẩm
     */
    @PutMapping("update/{productId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PRODUCT_MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request) {
        log.info("Cập nhật sản phẩm với ID: {}", productId);
        
        ProductResponse response = productService.updateProduct(productId, request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(productId);
                log.info("✅ Đã sync product {} vào Elasticsearch", productId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        ApiResponse<ProductResponse> apiResponse = ResponseUtil.success(
            response, 
            "Cập nhật sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Cập nhật rating sản phẩm
     */
    @PutMapping("/rating/{productId}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductRating(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRatingRequest request) {
        log.info("Cập nhật rating sản phẩm với ID: {}", productId);
        
        ProductResponse response = productService.updateProductRating(productId, request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProduct(productId);
                log.info("✅ Đã sync product rating {} vào Elasticsearch", productId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        ApiResponse<ProductResponse> apiResponse = ResponseUtil.success(
            response, 
            "Cập nhật rating sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy danh sách sản phẩm với phân trang và lọc
     * 
     * Hỗ trợ các tham số:
     * - page: Số trang (bắt đầu từ 0)
     * - size: Số sản phẩm mỗi trang (mặc định 12)
     * - sort: Sắp xếp (ví dụ: "createdAt,desc" hoặc "name,asc")
     * - keyword: Từ khóa tìm kiếm
     * - categoryId: ID danh mục
     * - supplierIds: Danh sách ID nhà cung cấp (hỗ trợ lọc nhiều suppliers - logic OR)
     *   Ví dụ: ?supplierIds=uuid1&supplierIds=uuid2&supplierIds=uuid3
     *   → Hiển thị sản phẩm thuộc BẤT KỲ nhà cung cấp nào trong danh sách
     * - priceRange: Khoảng giá (ALL, UNDER_100K, FROM_100K_TO_200K, FROM_200K_TO_500K, FROM_500K_TO_1M, FROM_1M_TO_3M, OVER_3M)
     * - minPrice, maxPrice: Khoảng giá tùy chỉnh (nếu không dùng priceRange)
     * - isOnSale: Sản phẩm khuyến mãi
     * - isFeatured: Sản phẩm nổi bật
     * - inStock: Sản phẩm có hàng
     * - minRating: Rating tối thiểu (ALL, ONE_STAR, TWO_STAR, THREE_STAR, FOUR_STAR, FIVE_STAR)
     * - sortBy: Kiểu sắp xếp (PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC, NEWEST, OLDEST, RATING_ASC, RATING_DESC)
     * 
     * Ví dụ: /products?page=0&size=12&keyword=rau&priceRange=FROM_100K_TO_200K&sortBy=PRICE_ASC&minRating=FOUR_STAR&supplierIds=uuid1&supplierIds=uuid2
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @ModelAttribute ProductFilterRequest filter,
            Pageable pageable) {
        log.info("Lấy danh sách sản phẩm - Page: {}, Size: {}, Filter: {}", 
                pageable.getPageNumber(), pageable.getPageSize(), filter);
        
        Page<ProductResponse> productPage = productService.searchProducts(filter, pageable);
        
        // Tạo message phù hợp
        String message;
        if (productPage.hasContent()) {
            message = String.format("Lấy danh sách sản phẩm thành công. Trang %d/%d, tổng %d sản phẩm", 
                    productPage.getNumber() + 1, 
                    productPage.getTotalPages(), 
                    productPage.getTotalElements());
        } else {
            message = "Không tìm thấy sản phẩm nào phù hợp với tiêu chí lọc";
        }
        
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(productPage, message);
        return ResponseEntity.ok(apiResponse);
    }

    
    /**
     * Lấy danh sách sản phẩm active với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/active/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getActiveProductsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách sản phẩm active với phân trang - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getActiveProductsPaginatedByCreatedAt(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(response, "Lấy danh sách sản phẩm active thành công");
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy danh sách sản phẩm inactive với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/inactive/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getInactiveProductsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách sản phẩm inactive với phân trang - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getInactiveProductsPaginatedByCreatedAt(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(response, "Lấy danh sách sản phẩm inactive thành công");
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy danh sách sản phẩm featured với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/featured/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getFeaturedProductsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách sản phẩm featured với phân trang - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getFeaturedProductsPaginatedByCreatedAt(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(response, "Lấy danh sách sản phẩm featured thành công");
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lấy tất cả sản phẩm với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/all/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProductsPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả sản phẩm với phân trang - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getAllProductsPaginatedByCreatedAt(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(response, "Lấy danh sách tất cả sản phẩm thành công");
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Tìm kiếm sản phẩm với phân trang - Hỗ trợ tiếng Việt có dấu
     * @param keyword Từ khóa tìm kiếm (tiếng Việt có dấu)
     * @param tab Trạng thái: active, inactive, featured, all
     * @param page Số trang (mặc định 0)
     * @param size Số lượng/trang (mặc định 20)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> searchProducts(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔍 Tìm kiếm sản phẩm - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        // Validate tab
        if (!tab.matches("^(active|inactive|featured|all)$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        Page<ProductResponse> response = productService.searchProductsByTab(keyword, tab, page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Tìm kiếm sản phẩm thành công - Tìm thấy " + response.getTotalElements() + " kết quả"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy sản phẩm mới nhất (active) với phân trang - Customer
     */
    @GetMapping("/customer/latest/page")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getLatestActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🆕 Customer lấy sản phẩm mới nhất (active) - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getLatestActiveProducts(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách sản phẩm mới nhất thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy sản phẩm bán chạy (active) với phân trang - Customer
     */
    @GetMapping("/customer/best-selling/page")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getBestSellingActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔥 Customer lấy sản phẩm bán chạy (active) - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getBestSellingActiveProducts(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách sản phẩm bán chạy thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy sản phẩm khuyến mãi (FLASH_SALE active) với phân trang - Customer
     */
    @GetMapping("/customer/flash-sale/page")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getFlashSaleProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("⚡ Customer lấy sản phẩm khuyến mãi (FLASH_SALE) - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getFlashSaleProducts(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách sản phẩm khuyến mãi thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy sản phẩm nổi bật (active + featured) với phân trang - Customer
     */
    @GetMapping("/customer/featured/page")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getFeaturedActiveProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("⭐ Customer lấy sản phẩm nổi bật (active + featured) - page: {}, size: {}", page, size);
        
        Page<ProductResponse> response = productService.getFeaturedActiveProducts(page, size);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách sản phẩm nổi bật thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }

    /**
     * Lấy sản phẩm được xếp hạng cao nhất (sorted by averageRating DESC)
     * Mỗi sản phẩm bao gồm đánh giá cao nhất có hình ảnh + thông tin người dùng
     * - Customer public endpoint
     */
    @GetMapping("/customer/top-rated-with-reviews/page")
    public ResponseEntity<ApiResponse<Page<ProductWithTopReviewResponse>>> getTopRatedProductsWithTopReview(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🏆 Customer lấy sản phẩm được xếp hạng cao nhất kèm đánh giá hàng đầu - page: {}, size: {}", page, size);
        
        Page<ProductWithTopReviewResponse> response = productService.getTopRatedProductsWithTopReview(page, size);
        ApiResponse<Page<ProductWithTopReviewResponse>> apiResponse = ResponseUtil.success(
            response, 
            "Lấy danh sách sản phẩm được xếp hạng cao nhất thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * Lọc sản phẩm theo nhiều tiêu chí
     * - campaignSlug: slug của campaign promotion
     * - isNew: sản phẩm mới nhất
     * - isBestSeller: sản phẩm bán chạy nhất
     * - isOnSale: sản phẩm flash sale
     * - isFeatured: sản phẩm nổi bật
     * - isHighRating: sản phẩm đánh giá cao
     * - categoryIds: danh mục
     * - supplierIds: nhà cung cấp
     * - priceRange: khoảng giá
     * - sortBy: sắp xếp (PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC, NEWEST, OLDEST, RATING_ASC, RATING_DESC)
     * - page: số trang (mặc định 0)
     * - size: số sản phẩm/trang (mặc định 8)
     * - isActive: chỉ lấy sản phẩm hoạt động (mặc định true)
     * 
     * ✅ ĐỒNG BỘ VỚI GET /products - Dùng enum ProductSortType cho sortBy
     * 
     * Ví dụ: /products/customer/filter?isOnSale=true&page=0&size=8&sortBy=PRICE_ASC
     */
    @GetMapping("/customer/filter")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> filterProducts(
            @ModelAttribute FilterProductsRequest request) {
        log.info("🔍 Customer lọc sản phẩm - Page: {}, Size: {}, SortBy: {}, campaignSlug: {}, isNew: {}, isBestSeller: {}, isOnSale: {}, isFeatured: {}, isHighRating: {}, categoryIds: {}, supplierIds: {}, priceRange: {}, isActive: {}", 
                request.getPage(), request.getSize(), request.getSortBy(),
                request.getCampaignSlug(), request.getIsNew(), request.getIsBestSeller(), 
                request.getIsOnSale(), request.getIsFeatured(), request.getIsHighRating(),
                request.getCategoryIds(), request.getSupplierIds(), request.getPriceRange(),
                request.getIsActive());
        
        Page<ProductResponse> response = productService.filterProducts(request);
        ApiResponse<Page<ProductResponse>> apiResponse = ResponseUtil.success(
            response,
            "Lọc sản phẩm thành công"
        );
        
        return ResponseEntity.ok(apiResponse);
    }
    
    /**
     * 🎯 GỢI Ý SẢN PHẨM CÁ NHÂN HÓA (PERSONAL RECOMMENDATIONS)
     * 
     * <p><b>Kịch bản:</b></p>
     * <ul>
     *   <li>Hiển thị "Gợi ý dành cho bạn" trên trang chủ</li>
     *   <li>Dựa trên lịch sử xem, mua, thích, đánh giá của user</li>
     *   <li>AI/ML từ Recombee phân tích và đề xuất sản phẩm phù hợp</li>
     * </ul>
     * 
     * <p><b>Fallback:</b> Nếu Recombee lỗi → Trả về sản phẩm bán chạy</p>
     * 
     * @param pageable Phân trang (page, size)
     * @return Page<ProductResponse> sản phẩm gợi ý cá nhân hóa
     */
    @GetMapping("/recommendations/personal")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getPersonalRecommendations(
            Pageable pageable) {
        
        try {
            // Lấy userId từ SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof CustomUserPrincipal)) {
                // User chưa đăng nhập → Fallback: Best sellers
                log.info("🎯 User chưa đăng nhập → Fallback to best sellers");
                Page<ProductResponse> response = productService.getBestSellingActiveProducts(
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
                );
                return ResponseEntity.ok(ResponseUtil.success(response, 
                    "Lấy sản phẩm bán chạy thành công (fallback)"));
            }
            
            CustomUserPrincipal principal = (CustomUserPrincipal) auth.getPrincipal();
            UUID userId = principal.getUserId();
            
            log.info("🎯 Lấy gợi ý cá nhân hóa cho user: {} - page: {}, size: {}", 
                userId, pageable.getPageNumber(), pageable.getPageSize());
            
            // Gọi Recombee
            if (recombeeSyncService == null) {
                log.warn("⚠️ RecombeeSyncService không khả dụng → Fallback to best sellers");
                Page<ProductResponse> response = productService.getBestSellingActiveProducts(
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
                );
                return ResponseEntity.ok(ResponseUtil.success(response, 
                    "Lấy sản phẩm bán chạy thành công (fallback)"));
            }
            
            // ⭐ Fetch thêm 1 item để kiểm tra có page tiếp theo không
            int requestCount = (pageable.getPageNumber() + 1) * pageable.getPageSize() + 1;
            
            List<String> productIds = recombeeSyncService.getPersonalRecommendations(
                userId, 
                requestCount
            );
            
            if (productIds.isEmpty()) {
                // Recombee trả về rỗng → Fallback: Latest products
                log.info("📭 Recombee trả về rỗng → Fallback to latest products");
                Page<ProductResponse> response = productService.getLatestActiveProducts(
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
                );
                return ResponseEntity.ok(ResponseUtil.success(response, 
                    "Lấy sản phẩm mới nhất thành công (fallback)"));
            }
            
            // Convert String IDs to UUIDs
            List<UUID> uuids = productIds.stream()
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
            
            // Query DB theo thứ tự từ Recombee
            List<ProductResponse> allProducts = new java.util.ArrayList<>();
            for (UUID productId : uuids) {
                try {
                    ProductResponse product = productService.getActiveProductById(productId);
                    allProducts.add(product);
                } catch (Exception e) {
                    log.warn("⚠️ Không tìm thấy product {} hoặc không active", productId);
                }
            }
            
            // Calculate pagination
            int start = pageable.getPageNumber() * pageable.getPageSize();
            int end = Math.min(start + pageable.getPageSize(), allProducts.size());
            
            // Get current page content
            List<ProductResponse> pageContent = start >= allProducts.size() 
                ? java.util.Collections.emptyList()
                : allProducts.subList(start, end);
            
            // ⭐ totalElements = số lượng thực tế có trong allProducts (có thể > requestCount nếu có nhiều hơn)
            // Nếu allProducts.size() == requestCount thì còn data, set totalElements lớn hơn
            long totalElements = allProducts.size() >= requestCount 
                ? requestCount // Có thêm data, giả định có ít nhất requestCount items
                : allProducts.size(); // Hết data rồi
            
            Page<ProductResponse> page = new org.springframework.data.domain.PageImpl<>(
                pageContent,
                pageable,
                totalElements
            );
            
            log.info("✅ Trả về {} sản phẩm gợi ý cá nhân hóa - page: {}/{}, hasNext: {}", 
                pageContent.size(), page.getNumber(), page.getTotalPages(), page.hasNext());
            return ResponseEntity.ok(ResponseUtil.success(page, 
                "Lấy gợi ý sản phẩm thành công"));
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy gợi ý cá nhân hóa: {}", e.getMessage(), e);
            // Fallback: Best sellers
            Page<ProductResponse> response = productService.getBestSellingActiveProducts(
                pageable.getPageNumber(), 
                pageable.getPageSize()
            );
            return ResponseEntity.ok(ResponseUtil.success(response, 
                "Lấy sản phẩm bán chạy thành công (fallback)"));
        }
    }
    
    /**
     * 🔗 GỢI Ý SẢN PHẨM TƯƠNG TỰ (RELATED RECOMMENDATIONS)
     * 
     * <p><b>Kịch bản:</b></p>
     * <ul>
     *   <li>Hiển thị "Sản phẩm tương tự" trên trang chi tiết sản phẩm</li>
     *   <li>Dựa trên người dùng cũng xem/mua sản phẩm nào cùng với sản phẩm này</li>
     *   <li>AI/ML từ Recombee phân tích mối quan hệ giữa các sản phẩm</li>
     * </ul>
     * 
     * <p><b>Fallback:</b> Nếu Recombee lỗi → Trả về sản phẩm cùng danh mục</p>
     * 
     * @param productId ID của sản phẩm hiện tại
     * @param pageable Phân trang (page, size)
     * @return Page<ProductResponse> sản phẩm tương tự
     */
    @GetMapping("/recommendations/related/{productId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getRelatedRecommendations(
            @PathVariable UUID productId,
            Pageable pageable) {
        
        try {
            log.info("🔗 Lấy sản phẩm tương tự cho product: {}", productId);
            
            // Lấy userId nếu có (optional)
            UUID userId = null;
            try {
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserPrincipal) {
                    userId = ((CustomUserPrincipal) auth.getPrincipal()).getUserId();
                }
            } catch (Exception e) {
                // Ignore - user không đăng nhập
            }
            
            // Gọi Recombee
            if (recombeeSyncService == null) {
                log.warn("⚠️ RecombeeSyncService không khả dụng → Fallback to same category");
                return getFallbackRelatedProducts(productId, pageable);
            }
            
            List<String> productIds = recombeeSyncService.getRelatedRecommendations(
                productId, 
                userId, 
                pageable.getPageSize()
            );
            
            if (productIds.isEmpty()) {
                // Recombee trả về rỗng → Fallback: Same category
                log.info("📭 Recombee trả về rỗng → Fallback to same category");
                return getFallbackRelatedProducts(productId, pageable);
            }
            
            // Convert String IDs to UUIDs
            List<UUID> uuids = productIds.stream()
                .map(UUID::fromString)
                .collect(java.util.stream.Collectors.toList());
            
            // Query DB theo thứ tự từ Recombee
            List<ProductResponse> products = new java.util.ArrayList<>();
            for (UUID pid : uuids) {
                try {
                    ProductResponse product = productService.getActiveProductById(pid);
                    products.add(product);
                } catch (Exception e) {
                    log.warn("⚠️ Không tìm thấy product {} hoặc không active", pid);
                }
            }
            
            // Tạo Page từ List
            int start = pageable.getPageNumber() * pageable.getPageSize();
            int end = Math.min(start + pageable.getPageSize(), products.size());
            List<ProductResponse> pageContent = products.subList(
                Math.min(start, products.size()), 
                end
            );
            
            Page<ProductResponse> page = new org.springframework.data.domain.PageImpl<>(
                pageContent,
                pageable,
                products.size()
            );
            
            log.info("✅ Trả về {} sản phẩm tương tự", pageContent.size());
            return ResponseEntity.ok(ResponseUtil.success(page, 
                "Lấy sản phẩm tương tự thành công"));
            
        } catch (Exception e) {
            log.error("❌ Lỗi khi lấy sản phẩm tương tự: {}", e.getMessage(), e);
            // Fallback: Same category
            return getFallbackRelatedProducts(productId, pageable);
        }
    }
    
    /**
     * Fallback: Lấy sản phẩm cùng category (trừ sản phẩm hiện tại)
     */
    private ResponseEntity<ApiResponse<Page<ProductResponse>>> getFallbackRelatedProducts(
            UUID productId, Pageable pageable) {
        try {
            ProductResponse currentProduct = productService.getProductById(productId);
            UUID categoryId = currentProduct.getCategory().getId();
            
            // Filter products cùng category, trừ sản phẩm hiện tại
            FilterProductsRequest filterRequest = new FilterProductsRequest();
            filterRequest.setCategoryIds(java.util.Arrays.asList(categoryId));
            filterRequest.setIsActive(true);
            filterRequest.setPage(pageable.getPageNumber());
            filterRequest.setSize(pageable.getPageSize());
            
            Page<ProductResponse> response = productService.filterProducts(filterRequest);
            
            // Loại bỏ sản phẩm hiện tại khỏi kết quả
            List<ProductResponse> filtered = response.getContent().stream()
                .filter(p -> !p.getId().equals(productId))
                .collect(java.util.stream.Collectors.toList());
            
            Page<ProductResponse> page = new org.springframework.data.domain.PageImpl<>(
                filtered,
                pageable,
                response.getTotalElements() - 1
            );
            
            return ResponseEntity.ok(ResponseUtil.success(page, 
                "Lấy sản phẩm cùng danh mục thành công (fallback)"));
                
        } catch (Exception e) {
            log.error("❌ Fallback cũng thất bại: {}", e.getMessage());
            // Return empty page
            Page<ProductResponse> emptyPage = new org.springframework.data.domain.PageImpl<>(
                java.util.Collections.emptyList(),
                pageable,
                0
            );
            return ResponseEntity.ok(ResponseUtil.success(emptyPage, 
                "Không tìm thấy sản phẩm tương tự"));
        }
    }
}
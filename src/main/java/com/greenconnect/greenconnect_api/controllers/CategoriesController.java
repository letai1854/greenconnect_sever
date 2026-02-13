package com.greenconnect.greenconnect_api.controllers;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.dtos.request.CreateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.ApiResponse;
import com.greenconnect.greenconnect_api.dtos.response.CategoryResponse;
import com.greenconnect.greenconnect_api.dtos.response.ResponseUtil;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.services.CategoryService;
import com.greenconnect.greenconnect_api.utils.JwtUtils;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Categories Controller - Quản lý danh mục sản phẩm.
 * <p>Bao gồm CRUD operations và public endpoints cho danh mục.</p>
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoriesController {

    private final CategoryService categoryService;
    private final JwtUtils jwtUtils;
    
    @Autowired(required = false) // ⭐ OPTIONAL DEPENDENCY - REQUIRED = FALSE
    private ElasticsearchSyncService elasticsearchSyncService;

    /**
     * Tạo danh mục mới - Chỉ Admin
     */                        //@Valid @RequestBody SupplierRequest request
    @PostMapping("/create")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
        public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        // Extract token và kiểm tra admin
        // String token = authHeader.substring(7);
        // Set<Role> userRoles = jwtUtils.getRolesFromToken(token);
        
        // if (!userRoles.contains(Role.ADMIN)) {
        //     throw new BusinessException(ErrorCode.UNAUTHORIZED);
        // }
        
        log.info("Admin tạo danh mục mới: {}", request.getName());
        CategoryResponse response = categoryService.createCategory(request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH (sync tất cả products của category này)
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncCategory(response.getId());
                log.info("✅ Đã sync category {} vào Elasticsearch", response.getId());
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        ApiResponse<CategoryResponse> apiResponse = ResponseUtil.success(
            response,
            "Tạo danh mục thành công"
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }
    
    /**
     * Cập nhật danh mục - Chỉ Admin
     */
    @PutMapping("update/{categoryId}")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(@PathVariable UUID categoryId,
                                                       @Valid @RequestBody UpdateCategoryRequest request) {
        // Extract token và kiểm tra admin
        // String token = authHeader.substring(7);
        // Set<Role> userRoles = jwtUtils.getRolesFromToken(token);
        
        // if (!userRoles.contains(Role.ADMIN)) {
        //     throw new BusinessException(ErrorCode.UNAUTHORIZED);
        // }
        
        log.info("Admin cập nhật danh mục: {}", categoryId);
        CategoryResponse response = categoryService.updateCategory(categoryId, request);
        
        // 🔄 SYNC VÀO ELASTICSEARCH (sync tất cả products của category này)
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncCategory(categoryId);
                log.info("✅ Đã sync category {} vào Elasticsearch", categoryId);
            } catch (Exception e) {
                log.warn("⚠️ Lỗi sync Elasticsearch: {}", e.getMessage());
            }
        } else {
            log.info("ℹ️ Elasticsearch không khả dụng, bỏ qua sync");
        }
        
        ApiResponse<CategoryResponse> apiResponse = ResponseUtil.success(
            response,
            "Cập nhật danh mục thành công"
        );
        return ResponseEntity.status(HttpStatus.OK).body(apiResponse);
    }
    
    /**
     * Lấy tất cả danh mục - Admin (bao gồm cả inactive)
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> getAllCategoriesForAdmin() {
        // Tạm thời disable JWT check để test
        /*
        // Extract token và kiểm tra admin
        String token = authHeader.substring(7);
        Set<Role> userRoles = jwtUtils.getRolesFromToken(token);
        
        if (!userRoles.contains(Role.ADMIN)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        */
        
        log.info("Admin lấy tất cả danh mục");
        List<CategoryResponse> response = categoryService.getAllCategories();
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh mục theo ID - Admin
     */
    @GetMapping("/admin/{categoryId}")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> getCategoryByIdForAdmin(@PathVariable UUID categoryId) {
        // Tạm thời disable JWT check để test
        /*
        // Extract token và kiểm tra admin
        String token = authHeader.substring(7);
        Set<Role> userRoles = jwtUtils.getRolesFromToken(token);
        
        if (!userRoles.contains(Role.ADMIN)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        */
        
        log.info("Admin lấy danh mục theo ID: {}", categoryId);
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        return ResponseUtil.success(response, "Lấy thông tin danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục đang hoạt động - Public
     */
    @GetMapping("/active")
    public ApiResponse<List<CategoryResponse>> getActiveCategories() {
        log.info("Lấy danh sách danh mục đang hoạt động (public)");
        List<CategoryResponse> response = categoryService.getActiveCategories();
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục không hoạt động - Admin
     */
    @GetMapping("/admin/inactive")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> getInactiveCategories() {
        log.info("Admin lấy danh sách danh mục không hoạt động");
        List<CategoryResponse> response = categoryService.getInactiveCategories();
        return ResponseUtil.success(response, "Lấy danh sách danh mục không hoạt động thành công");
    }
    
    /**
     * Lấy danh sách danh mục đang hoạt động với phân trang - Public
     */
    @GetMapping("/active/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getActiveCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy danh sách danh mục đang hoạt động với phân trang - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getActiveCategoriesPaginated(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục không hoạt động với phân trang - Admin
     */
    @GetMapping("/admin/inactive/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getInactiveCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách danh mục không hoạt động với phân trang - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getInactiveCategoriesPaginated(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục không hoạt động thành công");
    }
    
    /**
     * Lấy tất cả danh mục với phân trang - Admin
     */
    @GetMapping("/admin/all/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getAllCategoriesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả danh mục với phân trang - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getAllCategoriesPaginated(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục đang hoạt động sắp xếp theo thời gian tạo giảm dần - Public
     */
    @GetMapping("/active/latest")
    public ApiResponse<List<CategoryResponse>> getActiveCategoriesByCreatedAtDesc() {
        log.info("Lấy danh sách danh mục đang hoạt động sắp xếp theo thời gian tạo giảm dần");
        List<CategoryResponse> response = categoryService.getActiveCategoriesByCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục không hoạt động sắp xếp theo thời gian tạo giảm dần - Admin
     */
    @GetMapping("/admin/inactive/latest")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> getInactiveCategoriesByCreatedAtDesc() {
        log.info("Admin lấy danh sách danh mục không hoạt động sắp xếp theo thời gian tạo giảm dần");
        List<CategoryResponse> response = categoryService.getInactiveCategoriesByCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách danh mục không hoạt động thành công");
    }
    
    /**
     * Lấy tất cả danh mục sắp xếp theo thời gian tạo giảm dần - Admin
     */
    @GetMapping("/admin/all/latest")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<List<CategoryResponse>> getAllCategoriesByCreatedAtDesc() {
        log.info("Admin lấy tất cả danh mục sắp xếp theo thời gian tạo giảm dần");
        List<CategoryResponse> response = categoryService.getAllCategoriesByCreatedAtDesc();
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Lấy danh sách danh mục đang hoạt động với phân trang sắp xếp theo thời gian tạo - Public
     */
    @GetMapping("/active/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getActiveCategoriesPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy danh sách danh mục đang hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getActiveCategoriesPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    @GetMapping("/customer/active/latest/page")
    public ApiResponse<Page<CategoryResponse>> getActiveCategoriesPaginatedForCustomerByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Lấy danh sách danh mục đang hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getActiveCategoriesPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    /**
     * Lấy danh sách danh mục không hoạt động với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/inactive/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getInactiveCategoriesPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy danh sách danh mục không hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getInactiveCategoriesPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục không hoạt động thành công");
    }
    
    /**
     * Lấy tất cả danh mục với phân trang sắp xếp theo thời gian tạo - Admin
     */
    @GetMapping("/admin/all/latest/page")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> getAllCategoriesPaginatedByCreatedAt(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Admin lấy tất cả danh mục với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        Page<CategoryResponse> response = categoryService.getAllCategoriesPaginatedByCreatedAt(page, size);
        return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    }
    
    /**
     * Tìm kiếm danh mục với phân trang - Hỗ trợ tiếng Việt có dấu
     * @param keyword Từ khóa tìm kiếm (tiếng Việt có dấu)
     * @param tab Trạng thái: active, inactive, all
     * @param page Số trang (mặc định 0)
     * @param size Số lượng/trang (mặc định 20)
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> searchCategories(
            @RequestParam String keyword,
            @RequestParam String tab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("🔍 Tìm kiếm danh mục - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        // Validate tab
        if (!tab.matches("^(active|inactive|all)$")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        
        Page<CategoryResponse> response = categoryService.searchCategories(keyword, tab, page, size);
        return ResponseUtil.success(response, "Tìm kiếm danh mục thành công - Tìm thấy " + response.getTotalElements() + " kết quả");
    }
    
    /**
     * Tìm kiếm danh mục với phân trang - Sử dụng Request Body (Alternative)
     */
    @PostMapping("/search")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<Page<CategoryResponse>> searchCategoriesWithBody(
            @Valid @RequestBody com.greenconnect.greenconnect_api.dtos.request.SearchCategoryRequest request) {
        log.info("🔍 Tìm kiếm danh mục (POST) - Keyword: '{}', Tab: '{}', Page: {}, Size: {}", 
                request.getKeyword(), request.getTab(), request.getPage(), request.getSize());
        
        Page<CategoryResponse> response = categoryService.searchCategories(
            request.getKeyword(), 
            request.getTab(), 
            request.getPage(), 
            request.getSize()
        );
        return ResponseUtil.success(response, "Tìm kiếm danh mục thành công - Tìm thấy " + response.getTotalElements() + " kết quả");
    }
    
    /**
     * Lấy tất cả danh mục - Public (chỉ active)
     */
    // @GetMapping("/GetAll")
    // public ApiResponse<List<CategoryResponse>> getPublicCategories() {
    //     log.info("Lấy danh sách danh mục (public)");
    //     List<CategoryResponse> response = categoryService.getActiveCategories();
    //     return ResponseUtil.success(response, "Lấy danh sách danh mục thành công");
    // }
    
    /**
     * Lấy danh mục theo ID - Public (chỉ nếu active)
     */
    @GetMapping("/{categoryId}")
    @PreAuthorize("hasRole('PRODUCT_MANAGER') or hasRole('ADMIN')")
    public ApiResponse<CategoryResponse> getCategoryById(@PathVariable UUID categoryId) {
        log.info("Lấy danh mục theo ID (public): {}", categoryId);
        CategoryResponse response = categoryService.getCategoryById(categoryId);
        
        // Kiểm tra nếu danh mục không active thì không cho truy cập public
        if (!response.getIsActive()) {
            throw new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
        }
        
        return ResponseUtil.success(response, "Lấy thông tin danh mục thành công");
    }
}
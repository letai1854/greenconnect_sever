package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.CreateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.CategoryResponse;

public interface CategoryService {
    
    /**
     * Tạo danh mục mới
     */
    CategoryResponse createCategory(CreateCategoryRequest request);
    
    /**
     * Cập nhật danh mục theo ID
     */
    CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request);
    
    /**
     * Lấy tất cả danh mục
     */
    List<CategoryResponse> getAllCategories();
    
    /**
     * Lấy danh mục theo ID
     */
    CategoryResponse getCategoryById(UUID categoryId);
    
    /**
     * Lấy danh sách danh mục đang hoạt động (cho public API)
     */
    List<CategoryResponse> getActiveCategories();
    
    /**
     * Lấy danh sách danh mục không hoạt động (cho admin)
     */
    List<CategoryResponse> getInactiveCategories();
    
    /**
     * Lấy danh sách danh mục đang hoạt động với phân trang
     */
    Page<CategoryResponse> getActiveCategoriesPaginated(int page, int size);
    
    /**
     * Lấy danh sách danh mục không hoạt động với phân trang
     */
    Page<CategoryResponse> getInactiveCategoriesPaginated(int page, int size);
    
    /**
     * Lấy tất cả danh mục với phân trang
     */
    Page<CategoryResponse> getAllCategoriesPaginated(int page, int size);
    
    /**
     * Lấy danh sách danh mục đang hoạt động sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    List<CategoryResponse> getActiveCategoriesByCreatedAtDesc();
    
    /**
     * Lấy danh sách danh mục không hoạt động sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    List<CategoryResponse> getInactiveCategoriesByCreatedAtDesc();
    
    /**
     * Lấy tất cả danh mục sắp xếp theo thời gian tạo giảm dần (mới nhất trước)
     */
    List<CategoryResponse> getAllCategoriesByCreatedAtDesc();
    
    /**
     * Lấy danh sách danh mục đang hoạt động với phân trang, sắp xếp theo thời gian tạo giảm dần
     */
    Page<CategoryResponse> getActiveCategoriesPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy danh sách danh mục không hoạt động với phân trang, sắp xếp theo thời gian tạo giảm dần
     */
    Page<CategoryResponse> getInactiveCategoriesPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy tất cả danh mục với phân trang, sắp xếp theo thời gian tạo giảm dần
     */
    Page<CategoryResponse> getAllCategoriesPaginatedByCreatedAt(int page, int size);
    
    /**
     * Tìm kiếm danh mục với phân trang theo từ khóa và tab (active/inactive/all)
     */
    Page<CategoryResponse> searchCategories(String keyword, String tab, int page, int size);
}
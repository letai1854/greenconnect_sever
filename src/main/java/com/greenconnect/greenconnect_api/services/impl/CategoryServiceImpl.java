package com.greenconnect.greenconnect_api.services.impl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.CreateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.CategoryResponse;
import com.greenconnect.greenconnect_api.entities.Category;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.mappers.CategoryMapper;
import com.greenconnect.greenconnect_api.repositories.CategoryRepository;
import com.greenconnect.greenconnect_api.services.CategoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryServiceImpl implements CategoryService {
    
    private final CategoryRepository categoryRepository;
    
    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        log.info("Tạo danh mục mới với tên: {}", request.getName());
        
        // Kiểm tra tên danh mục đã tồn tại chưa
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            log.warn("Tên danh mục đã tồn tại: {}", request.getName());
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }
        
        // Tạo category entity từ request
        Category category = CategoryMapper.fromCreateRequest(request);
        
        // 🔢 Tính displayOrder: lấy max hiện tại + 1
        Integer maxOrder = categoryRepository.findMaxDisplayOrder();
        Integer nextOrder = (maxOrder != null) ? maxOrder + 1 : 1;
        category.setDisplayOrder(nextOrder);
        log.info("⏭️ Gán displayOrder tự động: {}", nextOrder);
        
        // Lưu vào database
        Category savedCategory = categoryRepository.save(category);
        log.info("✅ Tạo danh mục thành công với ID: {} - Order: {}", savedCategory.getId(), nextOrder);
        
        return CategoryMapper.toCategoryResponse(savedCategory);
    }
    
    @Override
    @Transactional
    public CategoryResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        log.info("Cập nhật danh mục với ID: {}", categoryId);
        
        // Tìm category theo ID
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy danh mục với ID: {}", categoryId);
                    return new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                });
        
        // Nếu có thay đổi displayOrder
        if (request.getDisplayOrder() != null && !request.getDisplayOrder().equals(category.getDisplayOrder())) {
            Integer newOrder = request.getDisplayOrder();
            Integer oldOrder = category.getDisplayOrder();
            
            // 📊 Lấy max displayOrder
            Integer maxOrder = categoryRepository.findMaxDisplayOrder();
            
            // ✅ Nếu newOrder vượt quá max, set = max
            if (newOrder > maxOrder) {
                log.warn("⚠️ displayOrder {} vượt quá max ({}) - điều chỉnh thành {}", newOrder, maxOrder, maxOrder);
                newOrder = maxOrder;
            }
            
            log.info("Phát hiện thay đổi displayOrder từ {} sang {}", oldOrder, newOrder);
            
            // Tìm category có displayOrder trùng với order mới
            Category existingCategoryWithOrder = categoryRepository.findByDisplayOrder(newOrder);
            
            if (existingCategoryWithOrder != null && !existingCategoryWithOrder.getId().equals(categoryId)) {
                log.info("🔄 Tìm thấy category khác có displayOrder: {} - Tiến hành xử lý", newOrder);
                
                // Nếu oldOrder == newOrder (2 số giống nhau)
                if (oldOrder.equals(newOrder)) {
                    log.warn("⚠️ Phát hiện oldOrder == newOrder ({}) - Chỉ giữ lại 1 order, cập nhật category đang sửa thành max", oldOrder);
                    
                    // Category hiện tại cập nhật thành order lớn nhất + 1
                    Integer nextMaxOrder = maxOrder + 1;
                    category.setDisplayOrder(nextMaxOrder);
                    categoryRepository.save(category);
                    log.info("✅ Category {} nhận order {} (max + 1)", categoryId, nextMaxOrder);
                    
                    // Xóa return ở đây, tiếp tục lưu category ở dưới
                    return CategoryMapper.toCategoryResponse(category);
                } else {
                    // Hoán đổi bình thường: category cũ nhận oldOrder, category mới nhận newOrder
                    existingCategoryWithOrder.setDisplayOrder(oldOrder);
                    categoryRepository.save(existingCategoryWithOrder);
                    log.info("✅ Hoán đổi thành công - Category {} nhận order {}", existingCategoryWithOrder.getId(), oldOrder);
                    
                    // Cập nhật category hiện tại với newOrder
                    category.setDisplayOrder(newOrder);
                }
            } else if (existingCategoryWithOrder == null) {
                // Không có category nào có order này, cập nhật bình thường
                category.setDisplayOrder(newOrder);
            }
        }
        
        boolean hasChanges = false;
        
        // Cập nhật tên nếu khác null
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            // Kiểm tra tên đã tồn tại chưa (trừ category hiện tại)
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), categoryId)) {
                log.warn("Tên danh mục đã tồn tại: {}", request.getName());
                throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
            }
            category.setName(request.getName().trim());
            hasChanges = true;
        }
        
        // Cập nhật description nếu khác null
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim().isEmpty() ? null : request.getDescription().trim());
            hasChanges = true;
        }
        
        // Cập nhật imageUrl nếu khác null
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl().trim().isEmpty() ? null : request.getImageUrl().trim());
            hasChanges = true;
        }
        
        // Cập nhật isActive nếu khác null
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
            hasChanges = true;
        }
        
        // Lưu nếu có thay đổi
        if (hasChanges || request.getDisplayOrder() != null) {
            Category savedCategory = categoryRepository.save(category);
            log.info("✅ Cập nhật danh mục thành công: {}", categoryId);
            
            return CategoryMapper.toCategoryResponse(savedCategory);
        } else {
            log.info("Không có thay đổi nào cho danh mục: {}", categoryId);
            
            return CategoryMapper.toCategoryResponse(category);
        }
    }
    
    @Override
    public List<CategoryResponse> getAllCategories() {
        log.info("Lấy tất cả danh mục");
        
        List<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public CategoryResponse getCategoryById(UUID categoryId) {
        log.info("Lấy danh mục theo ID: {}", categoryId);
        
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy danh mục với ID: {}", categoryId);
                    return new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                });
        
        return CategoryMapper.toCategoryResponse(category);
    }
    
    @Override
    public List<CategoryResponse> getActiveCategories() {
        log.info("Lấy danh sách danh mục đang hoạt động");
        
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CategoryResponse> getInactiveCategories() {
        log.info("Lấy danh sách danh mục không hoạt động");
        
        List<Category> categories = categoryRepository.findByIsActiveFalseOrderByDisplayOrderAsc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<CategoryResponse> getActiveCategoriesPaginated(int page, int size) {
        log.info("Lấy danh sách danh mục đang hoạt động với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public Page<CategoryResponse> getInactiveCategoriesPaginated(int page, int size) {
        log.info("Lấy danh sách danh mục không hoạt động với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findByIsActiveFalseOrderByDisplayOrderAsc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public Page<CategoryResponse> getAllCategoriesPaginated(int page, int size) {
        log.info("Lấy tất cả danh mục với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findAllByOrderByDisplayOrderAsc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public List<CategoryResponse> getActiveCategoriesByCreatedAtDesc() {
        log.info("Lấy danh sách danh mục đang hoạt động sắp xếp theo thời gian tạo giảm dần");
        
        List<Category> categories = categoryRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CategoryResponse> getInactiveCategoriesByCreatedAtDesc() {
        log.info("Lấy danh sách danh mục không hoạt động sắp xếp theo thời gian tạo giảm dần");
        
        List<Category> categories = categoryRepository.findByIsActiveFalseOrderByCreatedAtDesc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<CategoryResponse> getAllCategoriesByCreatedAtDesc() {
        log.info("Lấy tất cả danh mục sắp xếp theo thời gian tạo giảm dần");
        
        List<Category> categories = categoryRepository.findAllByOrderByCreatedAtDesc();
        
        return categories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<CategoryResponse> getActiveCategoriesPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách danh mục đang hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findByIsActiveTrueOrderByCreatedAtDesc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public Page<CategoryResponse> getInactiveCategoriesPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách danh mục không hoạt động với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findByIsActiveFalseOrderByCreatedAtDesc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public Page<CategoryResponse> getAllCategoriesPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy tất cả danh mục với phân trang sắp xếp theo thời gian tạo - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories = categoryRepository.findAllByOrderByCreatedAtDesc(pageable);
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
    
    @Override
    public Page<CategoryResponse> searchCategories(String keyword, String tab, int page, int size) {
        log.info("🔍 Tìm kiếm danh mục - Từ khóa: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Category> categories;
        
        switch (tab.toLowerCase()) {
            case "active":
                categories = categoryRepository.searchActiveCategoriesByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong danh mục ACTIVE - Tìm thấy {} kết quả", categories.getTotalElements());
                break;
            case "inactive":
                categories = categoryRepository.searchInactiveCategoriesByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong danh mục INACTIVE - Tìm thấy {} kết quả", categories.getTotalElements());
                break;
            case "all":
                categories = categoryRepository.searchAllCategoriesByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong TẤT CẢ danh mục - Tìm thấy {} kết quả", categories.getTotalElements());
                break;
            default:
                log.warn("⚠️ Tab không hợp lệ: '{}' - Sử dụng 'all' mặc định", tab);
                categories = categoryRepository.searchAllCategoriesByKeyword(keyword, pageable);
                break;
        }
        
        return categories.map(CategoryMapper::toCategoryResponse);
    }
}
package com.greenconnect.greenconnect_api.mappers;

import com.greenconnect.greenconnect_api.dtos.request.CreateCategoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.CategoryResponse;
import com.greenconnect.greenconnect_api.entities.Category;

public class CategoryMapper {
    
    /**
     * Convert CreateCategoryRequest to Category entity
     */
    public static Category fromCreateRequest(CreateCategoryRequest request) {
        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
    }
    
    /**
     * Convert Category entity to CategoryResponse
     */
    public static CategoryResponse toCategoryResponse(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive())
                .displayOrder(category.getDisplayOrder())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
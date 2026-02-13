package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRatingRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;

/**
 * Service riêng để xử lý update sản phẩm
 * Tách riêng từ ProductService để code dễ maintain
 */
public interface ProductUpdateService {
    
    /**
     * Cập nhật thông tin cơ bản của sản phẩm
     */
    ProductResponse updateProduct(UUID productId, UpdateProductRequest request);
    
    /**
     * Cập nhật rating và review count
     */
    ProductResponse updateProductRating(UUID productId, UpdateProductRatingRequest request);
}
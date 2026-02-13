package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.ProductImage;

/**
 * Repository interface cho thực thể ProductImage.
 * 
 * <p>Quản lý các hình ảnh liên quan đến sản phẩm (Product). 
 * Tất cả các variants của một product sẽ dùng chung các ảnh này.</p>
 */
@Repository
public interface ProductImageRepository extends JpaRepository<ProductImage, UUID> {

    /**
     * Lấy tất cả images của một product, sắp xếp theo displayOrder
     */
    List<ProductImage> findByProductIdOrderByDisplayOrderAsc(UUID productId);
    
    /**
     * Lấy images của một product theo media type
     */
    List<ProductImage> findByProductIdAndMediaTypeOrderByDisplayOrderAsc(UUID productId, com.greenconnect.greenconnect_api.enums.MediaType mediaType);
    
    /**
     * Lấy ảnh chính của product (isMain = true)
     */
    List<ProductImage> findByProductIdAndIsMainTrue(UUID productId);
    
    /**
     * Xóa tất cả images của một product
     */
    void deleteByProductId(UUID productId);
    
}
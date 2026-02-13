package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

/**
 * ProductElasticsearchSyncService - Service để tự động đồng bộ với Elasticsearch 
 * khi có thay đổi dữ liệu Product (CRUD operations)
 * 
 * 🎯 MỤC ĐÍCH: Bạn yêu cầu tự động gọi sync khi CRUD Product
 * 
 * CÁCH SỬ DỤNG trong ProductService:
 * 
 * @Autowired private ProductElasticsearchSyncService elasticsearchSyncService;
 * 
 * // Khi CREATE Product:
 * Product savedProduct = productRepository.save(product);
 * elasticsearchSyncService.onProductCreated(savedProduct.getId());
 * 
 * // Khi UPDATE Product:  
 * Product updatedProduct = productRepository.save(product);
 * elasticsearchSyncService.onProductUpdated(updatedProduct.getId());
 * 
 * // Khi DELETE Product (set isActive = false):
 * product.setIsActive(false);
 * productRepository.save(product);
 * elasticsearchSyncService.onProductDeactivated(product.getId());
 * 
 * // Khi HARD DELETE Product:
 * productRepository.deleteById(productId);
 * elasticsearchSyncService.onProductDeleted(productId);
 */
public interface ProductElasticsearchSyncService {
    
    /**
     * Gọi khi tạo mới Product
     * @param productId ID của product vừa tạo
     */
    void onProductCreated(UUID productId);
    
    /**
     * Gọi khi cập nhật Product (name, description, category, supplier...)
     * @param productId ID của product vừa cập nhật
     */
    void onProductUpdated(UUID productId);
    
    /**
     * Gọi khi Product bị vô hiệu hóa (isActive = false) 
     * @param productId ID của product bị deactive
     */
    void onProductDeactivated(UUID productId);
    
    /**
     * Gọi khi Product bị xóa hoàn toàn khỏi database
     * @param productId ID của product bị xóa
     */
    void onProductDeleted(UUID productId);
    
        /**
     * 🆕 Gọi khi ProductVariant được tạo mới
     * ✅ CẬP NHẬT: Ảnh hưởng đến price min/max, stock, mainImageUrl nếu là default
     * @param productId ID của product chứa variant
     * @param variantId ID của variant vừa tạo
     */
    void onProductVariantCreated(UUID productId, UUID variantId);
    
    /**
     * 🆕 Gọi khi ProductVariant được cập nhật (giá, % giảm giá, stock, ảnh, active, default...)
     * ✅ CẬP NHẬT: Ảnh hưởng đến price aggregation, stock, mainImageUrl
     * @param productId ID của product chứa variant
     * @param variantId ID của variant vừa update
     */
    void onProductVariantUpdated(UUID productId, UUID variantId);
    
    /**
     * 🆕 Gọi khi ProductVariant bị xóa hoặc deactive
     * ✅ CẬP NHẬT: Ảnh hưởng đến price min/max, stock, có thể ảnh hưởng mainImageUrl
     * @param productId ID của product chứa variant
     * @param variantId ID của variant bị xóa
     */
    void onProductVariantDeleted(UUID productId, UUID variantId);
    
    /**
     * 🆕 Gọi khi thay đổi default variant (isDefault thay đổi)
     * ✅ CẬP NHẬT: Ảnh hưởng đến mainImageUrl trong ProductDocument
     * @param productId ID của product
     * @param newDefaultVariantId ID của variant mới làm default
     */
    void onDefaultVariantChanged(UUID productId, UUID newDefaultVariantId);
    
    /**
     * Gọi khi ProductVariant thay đổi (LEGACY method - khuyến nghị dùng methods cụ thể hơn ở trên)
     * @param productId ID của product có variant thay đổi
     */
    void onProductVariantChanged(UUID productId);
    
    /**
     * Gọi khi Category được cập nhật (ảnh hưởng đến tất cả products trong category)
     * @param categoryId ID của category vừa thay đổi
     */
    void onCategoryUpdated(UUID categoryId);
    
    /**
     * Gọi khi Supplier được cập nhật (ảnh hưởng đến tất cả products của supplier)
     * @param supplierId ID của supplier vừa thay đổi  
     */
    void onSupplierUpdated(UUID supplierId);
    
    /**
     * Sync thủ công một product cụ thể
     * @param productId ID của product cần sync
     * @return true nếu sync thành công
     */
    boolean syncProductNow(UUID productId);
    
    /**
     * Batch sync nhiều products cùng lúc (cho performance tốt hơn)
     * @param productIds List các product ID cần sync
     * @return Số lượng products sync thành công
     */
    int batchSyncProducts(java.util.List<UUID> productIds);
}
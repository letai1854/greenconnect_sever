package com.greenconnect.greenconnect_api.elasticsearch.services;

import java.util.UUID;

/**
 * ElasticsearchSyncService - Service đồng bộ dữ liệu MySQL → Elasticsearch
 * 
 * ✅ ĐƠN GIẢN, DỄ HIỂU
 * ✅ SẼ ĐƯỢC GỌI TỪ CÁC CONTROLLER HIỆN TẠI
 * 
 * CÁCH GỌI trong ProductController/CategoryController/SupplierController:
 * 
 * @Autowired private ElasticsearchSyncService elasticsearchSyncService;
 * 
 * // Sau khi save/update/delete trong controller:
 * elasticsearchSyncService.syncProduct(productId);
 * elasticsearchSyncService.syncCategory(categoryId);
 * elasticsearchSyncService.syncSupplier(supplierId);
 */
public interface ElasticsearchSyncService {
    
    /**
     * Đồng bộ 1 product từ MySQL sang Elasticsearch
     * GỌI TỪ: ProductController sau khi create/update/delete Product
     * 
     * @param productId UUID của product cần sync
     */
    void syncProduct(UUID productId);
    
    /**
     * Đồng bộ 1 product variant từ MySQL sang Elasticsearch
     * GỌI TỪ: ProductController sau khi create/update/delete ProductVariant
     * 
     * @param productId UUID của product chứa variant
     */
    void syncProductVariant(UUID productId);
    
    /**
     * Đồng bộ tất cả products của 1 category từ MySQL sang Elasticsearch
     * GỌI TỪ: CategoriesController sau khi update Category
     * 
     * @param categoryId UUID của category được update
     */
    void syncCategory(UUID categoryId);
    
    /**
     * Đồng bộ tất cả products của 1 supplier từ MySQL sang Elasticsearch
     * GỌI TỪ: SupplierController sau khi update Supplier
     * 
     * @param supplierId UUID của supplier được update
     */
    void syncSupplier(UUID supplierId);
    
    /**
     * Xóa product khỏi Elasticsearch
     * GỌI TỪ: ProductController sau khi delete Product
     * 
     * @param productId UUID của product bị xóa
     */
    void deleteProduct(UUID productId);
    
    /**
     * Cập nhật favorite count cho product trong Elasticsearch
     * GỌI TỪ: FavoriteController sau khi add/remove favorite
     * 
     * @param productId UUID của product cần cập nhật favorite count
     */
    void syncProductFavoriteCount(UUID productId);
    
    /**
     * Đồng bộ tất cả products từ MySQL sang Elasticsearch
     * GỌI TỪ: Admin hoặc lần đầu setup
     */
    void syncAllProducts();
    
    /**
     * Kiểm tra kết nối Elasticsearch
     * @return true nếu kết nối OK
     */
    boolean isElasticsearchHealthy();
}
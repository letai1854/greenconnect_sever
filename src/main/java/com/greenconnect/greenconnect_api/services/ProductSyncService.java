package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

/**
 * ProductSyncService - Service để đồng bộ dữ liệu Product giữa MySQL và Elasticsearch
 * 
 * Đảm bảo mỗi khi sản phẩm thay đổi trong MySQL, nó cũng được cập nhật trong Elasticsearch.
 */
public interface ProductSyncService {

    /**
     * Đồng bộ một sản phẩm cụ thể từ MySQL sang Elasticsearch
     * @param productId UUID của sản phẩm cần đồng bộ
     */
    void syncProduct(UUID productId);
    
    /**
     * Xóa một sản phẩm khỏi Elasticsearch index
     * @param productId UUID của sản phẩm cần xóa
     */
    void deleteProductFromIndex(UUID productId);
    
    /**
     * Đồng bộ toàn bộ sản phẩm từ MySQL sang Elasticsearch
     * Hữu ích khi mới bắt đầu hoặc rebuild index
     */
    void syncAllProducts();
    
    /**
     * Đồng bộ sản phẩm theo category
     * @param categoryId UUID của category
     */
    void syncProductsByCategory(UUID categoryId);
    
    /**
     * Đồng bộ sản phẩm theo supplier  
     * @param supplierId UUID của supplier
     */
    void syncProductsBySupplier(UUID supplierId);
    
    /**
     * Kiểm tra xem sản phẩm có tồn tại trong Elasticsearch không
     * @param productId UUID của sản phẩm
     * @return true nếu tồn tại
     */
    boolean existsInIndex(UUID productId);
    
    /**
     * Rebuild toàn bộ Elasticsearch index
     * (Xóa tất cả và tạo lại)
     */
    void rebuildIndex();
}
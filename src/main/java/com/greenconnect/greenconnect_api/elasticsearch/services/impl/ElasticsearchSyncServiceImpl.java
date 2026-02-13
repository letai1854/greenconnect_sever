package com.greenconnect.greenconnect_api.elasticsearch.services.impl;

// // === ELASTICSEARCH IMPORTS (ĐÃ COMMENT OUT) ===
// import java.math.BigDecimal;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Optional;
import java.util.UUID;

// import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;

// import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;
// import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProductImage;
// import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProductVariant;
// import com.greenconnect.greenconnect_api.elasticsearch.repositories.EsProductRepository;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
// import com.greenconnect.greenconnect_api.entities.Product;
// import com.greenconnect.greenconnect_api.entities.ProductImage;
// import com.greenconnect.greenconnect_api.entities.ProductVariant;
// import com.greenconnect.greenconnect_api.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ElasticsearchSyncServiceImpl - NO-OP Implementation (Elasticsearch đã được thay thế bằng MySQL LIKE)
 * 
 * ✅ TẤT CẢ CÁC METHOD ĐỀU LÀ NO-OP
 * ✅ GIỮ LẠI ĐỂ BACKWARD COMPATIBILITY (các controller/service vẫn inject được)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSyncServiceImpl implements ElasticsearchSyncService {
    
    // // === ELASTICSEARCH DEPENDENCIES (ĐÃ COMMENT OUT) ===
    // private final ProductRepository productRepository;
    // private final EsProductRepository esProductRepository;
    
    @Override
    public void syncProduct(UUID productId) {
        // NO-OP: Không cần sync vì đã dùng MySQL LIKE thay cho Elasticsearch
        log.debug("🔄 [NO-OP] syncProduct({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", productId);
    }
    
    @Override
    public void syncProductVariant(UUID productId) {
        // NO-OP
        log.debug("🔄 [NO-OP] syncProductVariant({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", productId);
    }
    
    @Override
    public void syncCategory(UUID categoryId) {
        // NO-OP
        log.debug("🔄 [NO-OP] syncCategory({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", categoryId);
    }
    
    @Override
    public void syncSupplier(UUID supplierId) {
        // NO-OP
        log.debug("🔄 [NO-OP] syncSupplier({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", supplierId);
    }
    
    @Override
    public void deleteProduct(UUID productId) {
        // NO-OP
        log.debug("🔄 [NO-OP] deleteProduct({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", productId);
    }
    
    @Override
    public void syncProductFavoriteCount(UUID productId) {
        // NO-OP
        log.debug("🔄 [NO-OP] syncProductFavoriteCount({}) - Elasticsearch đã được thay thế bằng MySQL LIKE", productId);
    }
    
    @Override
    public void syncAllProducts() {
        // NO-OP
        log.debug("🔄 [NO-OP] syncAllProducts() - Elasticsearch đã được thay thế bằng MySQL LIKE");
    }
    
    @Override
    public boolean isElasticsearchHealthy() {
        // Trả về true vì giờ dùng MySQL (luôn available)
        log.debug("🔍 [NO-OP] isElasticsearchHealthy() - Đang dùng MySQL LIKE, trả về true");
        return true;
    }

    /*
     * ============================================================================
     * CÁC METHOD CONVERT ELASTICSEARCH ĐÃ ĐƯỢC COMMENT OUT VÌ KHÔNG CÒN CẦN
     * ============================================================================
     * 
     * private EsProduct convertToEsProduct(Product product) { ... }
     * private EsProductVariant convertToEsProductVariant(ProductVariant variant) { ... }
     */
}
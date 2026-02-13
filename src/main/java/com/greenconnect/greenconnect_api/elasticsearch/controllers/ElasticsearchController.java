package com.greenconnect.greenconnect_api.elasticsearch.controllers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSearchService;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService;
import com.greenconnect.greenconnect_api.enums.PriceRange;
import com.greenconnect.greenconnect_api.enums.ProductSortType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ElasticsearchController - Controller đơn giản cho Elasticsearch
 * 
 * ✅ API ĐƠN GIẢN, DỄ HIỂU
 * ✅ SỬ DỤNG SERVICE LAYERS
 */
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchController {
    
    private final ElasticsearchSearchService searchService;
    private final ElasticsearchSyncService syncService;
    
    /**
     * 🔍 TÌM KIẾM CƠ BẢN
     */
    @GetMapping("/products")
    public ResponseEntity<Page<EsProduct>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("🔍 [SEARCH] keyword='{}', page={}, size={}", keyword, page, size);
        
        Page<EsProduct> result = searchService.searchProducts(keyword, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 TÌM KIẾM NÂNG CAO
     * Hỗ trợ:
     * - keyword: Từ khóa tìm kiếm
     * - categoryIds: Danh sách ID danh mục (logic OR) - ?categoryIds=uuid1&categoryIds=uuid2
     * - supplierIds: Danh sách ID nhà cung cấp (logic OR) - ?supplierIds=uuid1&supplierIds=uuid2
     * - priceRange: Khoảng giá enum (ALL, UNDER_100K, FROM_100K_TO_200K, FROM_200K_TO_500K, FROM_500K_TO_1M, FROM_1M_TO_3M, OVER_3M)
     * - sortBy: Kiểu sắp xếp enum (PRICE_ASC, PRICE_DESC, NAME_ASC, NAME_DESC, NEWEST, OLDEST, RATING_ASC, RATING_DESC)
     * - inStock: Lọc sản phẩm còn hàng
     * - page, size: Phân trang
     */
    @GetMapping("/products/advanced")
    public ResponseEntity<Page<EsProduct>> advancedSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<UUID> categoryIds,
            @RequestParam(required = false) List<UUID> supplierIds,
            @RequestParam(required = false) PriceRange priceRange,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) ProductSortType sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        log.info("🔍 [ADVANCED] keyword='{}', categories={}, suppliers={}, priceRange={}, inStock={}, sortBy={}", 
                keyword, categoryIds, supplierIds, priceRange, inStock, sortBy);
        
        Page<EsProduct> result = searchService.advancedSearch(
                keyword, categoryIds, supplierIds, priceRange, 
                inStock, sortBy, page, size);
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 TÌM THEO CATEGORY
     */
    @GetMapping("/products/category")
    public ResponseEntity<Page<EsProduct>> searchByCategory(
            @RequestParam UUID categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<EsProduct> result = searchService.searchByCategory(categoryId, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 TÌM THEO SUPPLIER
     */
    @GetMapping("/products/supplier")
    public ResponseEntity<Page<EsProduct>> searchBySupplier(
            @RequestParam UUID supplierId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<EsProduct> result = searchService.searchBySupplier(supplierId, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 TÌM THEO KHOẢNG GIÁ
     */
    @GetMapping("/products/price-range")
    public ResponseEntity<Page<EsProduct>> searchByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Page<EsProduct> result = searchService.searchByPriceRange(minPrice, maxPrice, page, size);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 🔍 SUGGESTIONS
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSearchSuggestions(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit) {
        
        List<String> suggestions = searchService.getSearchSuggestions(keyword, limit);
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * 🔍 SẢN PHẨM FEATURED
     */
    @GetMapping("/products/featured")
    public ResponseEntity<List<EsProduct>> getFeaturedProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<EsProduct> products = searchService.getFeaturedProducts(limit);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 🔍 SẢN PHẨM POPULAR
     */
    @GetMapping("/products/popular")
    public ResponseEntity<List<EsProduct>> getPopularProducts(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<EsProduct> products = searchService.getPopularProducts(limit);
        return ResponseEntity.ok(products);
    }
    
    /**
     * 🔍 SẢN PHẨM CÒN HÀNG
     */
    @GetMapping("/products/in-stock")
    public ResponseEntity<Page<EsProduct>> getInStockProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<EsProduct> result = searchService.getInStockProducts(pageable);
        return ResponseEntity.ok(result);
    }
    
    /**
     * 📊 THỐNG KÊ
     */
    @GetMapping("/stats/total")
    public ResponseEntity<Long> getTotalProducts() {
        long total = searchService.getTotalProducts();
        return ResponseEntity.ok(total);
    }
    
    /**
     * 🔄 ADMIN: SYNC TẤT CẢ PRODUCTS
     */
    @GetMapping("/admin/sync-all")
    public ResponseEntity<String> syncAllProducts() {
        log.info("🔄 [ADMIN] Sync all products request");
        
        try {
            syncService.syncAllProducts();
            return ResponseEntity.ok("✅ Sync all products started successfully!");
        } catch (Exception e) {
            log.error("❌ [ADMIN] Sync all products failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("❌ Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * 🔄 ADMIN: SYNC 1 PRODUCT
     */
    @GetMapping("/admin/sync-product")
    public ResponseEntity<String> syncProduct(@RequestParam UUID productId) {
        log.info("🔄 [ADMIN] Sync product: {}", productId);
        
        try {
            syncService.syncProduct(productId);
            return ResponseEntity.ok("✅ Sync product " + productId + " started successfully!");
        } catch (Exception e) {
            log.error("❌ [ADMIN] Sync product {} failed: {}", productId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("❌ Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * 🔍 ADMIN: HEALTH CHECK
     */
    @GetMapping("/admin/health")
    public ResponseEntity<String> healthCheck() {
        boolean healthy = syncService.isElasticsearchHealthy();
        
        if (healthy) {
            return ResponseEntity.ok("✅ Elasticsearch is healthy!");
        } else {
            return ResponseEntity.internalServerError()
                    .body("❌ Elasticsearch is unhealthy!");
        }
    }
}
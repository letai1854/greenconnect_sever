package com.greenconnect.greenconnect_api.elasticsearch.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;
import com.greenconnect.greenconnect_api.enums.PriceRange;
import com.greenconnect.greenconnect_api.enums.ProductSortType;

/**
 * ElasticsearchSearchService - Interface tìm kiếm Elasticsearch
 * 
 * ✅ TÌM KIẾM ĐƠN GIẢN, POWERFUL
 * ✅ SỬ DỤNG DỮ LIỆU Y HỆT MySQL
 */
public interface ElasticsearchSearchService {
    
    /**
     * Tìm kiếm cơ bản theo keyword
     */
    Page<EsProduct> searchProducts(String keyword, int page, int size);
    
    /**
     * Tìm kiếm nâng cao với nhiều filters (hỗ trợ multi-category, multi-supplier, priceRange enum, sortBy enum)
     * - categoryIds: Danh sách category IDs (logic OR)
     * - supplierIds: Danh sách supplier IDs (logic OR)
     * - priceRange: Khoảng giá enum (ALL, UNDER_100K, FROM_100K_TO_200K, ...)
     * - sortBy: Kiểu sắp xếp enum (PRICE_ASC, PRICE_DESC, NAME_ASC, ...)
     * - Chỉ lấy sản phẩm isActive = true
     */
    Page<EsProduct> advancedSearch(String keyword, List<UUID> categoryIds, List<UUID> supplierIds, 
                                 PriceRange priceRange, Boolean inStock, 
                                 ProductSortType sortBy, int page, int size);
    
    /**
     * Tìm kiếm theo category
     */
    Page<EsProduct> searchByCategory(UUID categoryId, int page, int size);
    
    /**
     * Tìm kiếm theo supplier
     */
    Page<EsProduct> searchBySupplier(UUID supplierId, int page, int size);
    
    /**
     * Tìm kiếm theo khoảng giá
     */
    Page<EsProduct> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size);
    
    /**
     * Lấy suggestions cho search
     */
    List<String> getSearchSuggestions(String keyword, int limit);
    
    /**
     * Lấy sản phẩm featured
     */
    List<EsProduct> getFeaturedProducts(int limit);
    
    /**
     * Lấy sản phẩm popular (nhiều review)
     */
    List<EsProduct> getPopularProducts(int limit);
    
    /**
     * Lấy sản phẩm còn hàng (có stock)
     */
    Page<EsProduct> getInStockProducts(Pageable pageable);
    
    /**
     * Đếm tổng số sản phẩm active
     */
    long getTotalProducts();
}
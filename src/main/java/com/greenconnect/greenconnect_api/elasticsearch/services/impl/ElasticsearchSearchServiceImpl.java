package com.greenconnect.greenconnect_api.elasticsearch.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;
import com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSearchService;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.enums.PriceRange;
import com.greenconnect.greenconnect_api.enums.ProductSortType;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ElasticsearchSearchServiceImpl - Thay thế Elasticsearch bằng MySQL LIKE queries
 * 
 * ✅ SỬ DỤNG LIKE QUERY TRÊN MySQL THAY CHO ELASTICSEARCH
 * ✅ LOGIC GIỮ NGUYÊN, CHỈ ĐỔI DATA SOURCE
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchSearchServiceImpl implements ElasticsearchSearchService {
    
    // // === ELASTICSEARCH DEPENDENCIES (ĐÃ COMMENT OUT) ===
    // private final EsProductRepository esProductRepository;
    // private final ElasticsearchOperations elasticsearchOperations;
    
    // === MYSQL DEPENDENCY (THAY THẾ) ===
    private final ProductRepository productRepository;
    
    @Override
    public Page<EsProduct> searchProducts(String keyword, int page, int size) {
        log.info("🔍 [SEARCH] Tìm kiếm: '{}' (page: {}, size: {})", keyword, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            
            Page<Product> productPage;
            if (!StringUtils.hasText(keyword)) {
                productPage = productRepository.findByIsActiveTrue(pageable);
            } else {
                productPage = productRepository.searchByKeyword(keyword.trim(), pageable);
            }
            
            // Convert Product -> EsProduct
            List<EsProduct> esProducts = productPage.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [SEARCH] Lỗi tìm kiếm '{}': {}", keyword, e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public Page<EsProduct> advancedSearch(String keyword, List<UUID> categoryIds, List<UUID> supplierIds, 
                                         PriceRange priceRange, Boolean inStock, 
                                         ProductSortType sortBy, int page, int size) {
        
        log.info("🔍 [ADVANCED] keyword='{}', categories={}, suppliers={}, priceRange={}, inStock={}, sortBy={}", 
                keyword, categoryIds, supplierIds, priceRange, inStock, sortBy);
        
        try {
            Sort sort = buildSortFromEnum(sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);
            
            boolean hasKeyword = StringUtils.hasText(keyword);
            boolean hasCategories = categoryIds != null && !categoryIds.isEmpty();
            boolean hasSuppliers = supplierIds != null && !supplierIds.isEmpty();
            String cleanKeyword = hasKeyword ? keyword.trim() : "";
            
            Page<Product> productPage;
            
            // Chọn query phù hợp dựa trên filters
            if (hasKeyword && hasCategories && hasSuppliers) {
                productPage = productRepository.searchByKeywordAndCategoriesAndSuppliers(cleanKeyword, categoryIds, supplierIds, pageable);
            } else if (hasKeyword && hasCategories) {
                productPage = productRepository.searchByKeywordAndCategories(cleanKeyword, categoryIds, pageable);
            } else if (hasKeyword && hasSuppliers) {
                productPage = productRepository.searchByKeywordAndSuppliers(cleanKeyword, supplierIds, pageable);
            } else if (hasKeyword) {
                productPage = productRepository.searchByKeyword(cleanKeyword, pageable);
            } else if (hasCategories && hasSuppliers) {
                productPage = productRepository.findByCategoriesAndSuppliers(categoryIds, supplierIds, pageable);
            } else if (hasCategories) {
                productPage = productRepository.findByCategories(categoryIds, pageable);
            } else if (hasSuppliers) {
                productPage = productRepository.findBySuppliers(supplierIds, pageable);
            } else {
                productPage = productRepository.findByIsActiveTrue(pageable);
            }
            
            // Post-filter: Price range
            List<Product> filtered = productPage.getContent();
            if (priceRange != null && priceRange != PriceRange.ALL) {
                BigDecimal minPrice = priceRange.getMinPrice();
                BigDecimal maxPrice = priceRange.getMaxPrice();
                filtered = filtered.stream()
                    .filter(p -> {
                        BigDecimal productMinPrice = getMinDiscountedPrice(p);
                        if (productMinPrice == null) return false;
                        boolean aboveMin = (minPrice == null) || productMinPrice.compareTo(minPrice) >= 0;
                        boolean belowMax = (maxPrice == null) || productMinPrice.compareTo(maxPrice) <= 0;
                        return aboveMin && belowMax;
                    })
                    .toList();
            }
            
            // Post-filter: inStock
            if (inStock != null && inStock) {
                filtered = filtered.stream()
                    .filter(p -> {
                        if (p.getProductVariants() == null) return false;
                        return p.getProductVariants().stream()
                            .anyMatch(v -> v.getIsActive() && v.getStockQuantity() > 0);
                    })
                    .toList();
            }
            
            List<EsProduct> esProducts = filtered.stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [ADVANCED] Lỗi advanced search: {}", e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public Page<EsProduct> searchByCategory(UUID categoryId, int page, int size) {
        log.info("🔍 [CATEGORY] Category: {} (page: {}, size: {})", categoryId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Product> productPage = productRepository.findByCategoryIdAndIsActiveTrue(categoryId, pageable);
            
            List<EsProduct> esProducts = productPage.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [CATEGORY] Lỗi search category {}: {}", categoryId, e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public Page<EsProduct> searchBySupplier(UUID supplierId, int page, int size) {
        log.info("🔍 [SUPPLIER] Supplier: {} (page: {}, size: {})", supplierId, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<Product> productPage = productRepository.findBySupplierIdAndIsActiveTrue(supplierId, pageable);
            
            List<EsProduct> esProducts = productPage.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [SUPPLIER] Lỗi search supplier {}: {}", supplierId, e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public Page<EsProduct> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size) {
        log.info("🔍 [PRICE] Price: {}~{} (page: {}, size: {})", minPrice, maxPrice, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Product> productPage = productRepository.findByPriceRangeAndIsActiveTrue(minPrice, maxPrice, pageable);
            
            List<EsProduct> esProducts = productPage.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [PRICE] Lỗi search price {}~{}: {}", minPrice, maxPrice, e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public List<String> getSearchSuggestions(String keyword, int limit) {
        log.info("🔍 [SUGGEST] Keyword: '{}' (limit: {})", keyword, limit);
        
        try {
            if (!StringUtils.hasText(keyword) || keyword.length() < 2) {
                return new ArrayList<>();
            }
            
            Pageable pageable = PageRequest.of(0, limit);
            return productRepository.findProductNameSuggestions(keyword.trim(), pageable);
            
        } catch (Exception e) {
            log.error("❌ [SUGGEST] Lỗi get suggestions '{}': {}", keyword, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<EsProduct> getFeaturedProducts(int limit) {
        log.info("🔍 [FEATURED] Limit: {}", limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<Product> products = productRepository.findByIsFeaturedTrueAndIsActiveTrue(pageable);
            
            return products.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
        } catch (Exception e) {
            log.error("❌ [FEATURED] Lỗi get featured products: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public List<EsProduct> getPopularProducts(int limit) {
        log.info("🔍 [POPULAR] Limit: {}", limit);
        
        try {
            Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "reviewCount", "averageRating"));
            Page<Product> products = productRepository.findByIsActiveTrue(pageable);
            
            return products.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
        } catch (Exception e) {
            log.error("❌ [POPULAR] Lỗi get popular products: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public Page<EsProduct> getInStockProducts(Pageable pageable) {
        log.info("🔍 [IN-STOCK] Pageable: {}", pageable);
        
        try {
            Page<Product> productPage = productRepository.findInStockAndIsActiveTrue(pageable);
            
            List<EsProduct> esProducts = productPage.getContent().stream()
                    .map(this::convertToEsProduct)
                    .toList();
            
            return new PageImpl<>(esProducts, pageable, productPage.getTotalElements());
            
        } catch (Exception e) {
            log.error("❌ [IN-STOCK] Lỗi get in-stock products: {}", e.getMessage(), e);
            return Page.empty();
        }
    }
    
    @Override
    public long getTotalProducts() {
        try {
            return productRepository.countByIsActiveTrue();
        } catch (Exception e) {
            log.error("❌ [COUNT] Lỗi count products: {}", e.getMessage(), e);
            return 0;
        }
    }
    
    /**
     * 🔧 HELPER: Build Sort từ ProductSortType enum (dùng tên field MySQL thay vì ES)
     */
    private Sort buildSortFromEnum(ProductSortType sortBy) {
        if (sortBy == null) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        return switch (sortBy) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "createdAt"); // Không sort được theo price trực tiếp trong JPA entity, dùng createdAt
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "createdAt");
            case NAME_ASC -> Sort.by(Sort.Direction.ASC, "name");
            case NAME_DESC -> Sort.by(Sort.Direction.DESC, "name");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case RATING_ASC -> Sort.by(Sort.Direction.ASC, "averageRating");
            case RATING_DESC -> Sort.by(Sort.Direction.DESC, "averageRating");
        };
    }
    
    /**
     * 🔧 HELPER: Lấy giá thấp nhất (discounted) của product
     */
    private BigDecimal getMinDiscountedPrice(Product product) {
        if (product.getProductVariants() == null || product.getProductVariants().isEmpty()) {
            return null;
        }
        return product.getProductVariants().stream()
                .filter(v -> v.getIsActive())
                .map(ProductVariant::getDiscountedPrice)
                .min(BigDecimal::compareTo)
                .orElse(null);
    }
    
    /**
     * 🔧 HELPER: Convert Product entity sang EsProduct DTO (để giữ nguyên response format)
     */
    private EsProduct convertToEsProduct(Product product) {
        BigDecimal minPrice = null, maxPrice = null;
        BigDecimal minDiscountedPrice = null, maxDiscountedPrice = null;
        BigDecimal defaultPrice = null, defaultDiscountedPrice = null;
        int totalStock = 0;
        
        if (product.getProductVariants() != null) {
            for (ProductVariant v : product.getProductVariants()) {
                if (!v.getIsActive()) continue;
                
                if (minPrice == null || v.getPrice().compareTo(minPrice) < 0) minPrice = v.getPrice();
                if (maxPrice == null || v.getPrice().compareTo(maxPrice) > 0) maxPrice = v.getPrice();
                
                BigDecimal dp = v.getDiscountedPrice();
                if (minDiscountedPrice == null || dp.compareTo(minDiscountedPrice) < 0) minDiscountedPrice = dp;
                if (maxDiscountedPrice == null || dp.compareTo(maxDiscountedPrice) > 0) maxDiscountedPrice = dp;
                
                if (v.getIsDefault() != null && v.getIsDefault()) {
                    defaultPrice = v.getPrice();
                    defaultDiscountedPrice = dp;
                }
                
                totalStock += v.getStockQuantity();
            }
        }
        
        if (defaultPrice == null) {
            defaultPrice = minPrice;
            defaultDiscountedPrice = minDiscountedPrice;
        }
        
        return EsProduct.builder()
                .id(product.getId().toString())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .sellNumber(product.getSellNumber())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .categoryId(product.getCategory() != null ? product.getCategory().getId().toString() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .categoryImageUrl(product.getCategory() != null ? product.getCategory().getImageUrl() : null)
                .categoryDisplayOrder(product.getCategory() != null ? product.getCategory().getDisplayOrder() : null)
                .categoryIsActive(product.getCategory() != null ? product.getCategory().getIsActive() : null)
                .supplierId(product.getSupplier() != null ? product.getSupplier().getId().toString() : null)
                .supplierName(product.getSupplier() != null ? product.getSupplier().getName() : null)
                .supplierLogoUrl(product.getSupplier() != null ? product.getSupplier().getLogoUrl() : null)
                .supplierIsActive(product.getSupplier() != null ? product.getSupplier().getIsActive() : null)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minDiscountedPrice(minDiscountedPrice)
                .maxDiscountedPrice(maxDiscountedPrice)
                .defaultPrice(defaultPrice)
                .defaultDiscountedPrice(defaultDiscountedPrice)
                .totalStock(totalStock)
                .inStock(totalStock > 0)
                .mainImageUrl(product.getMainImageUrlResolved())
                .build();
    }
}
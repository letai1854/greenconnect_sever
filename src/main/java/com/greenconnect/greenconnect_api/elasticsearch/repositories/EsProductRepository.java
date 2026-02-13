package com.greenconnect.greenconnect_api.elasticsearch.repositories;

/*
 * ============================================================================
 * ES PRODUCT REPOSITORY - ĐÃ COMMENT OUT TOÀN BỘ
 * Lý do: Đã thay thế Elasticsearch bằng MySQL LIKE queries
 * Tìm kiếm giờ dùng ProductRepository với LIKE queries
 * ============================================================================
 */

/*
import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.elasticsearch.entities.EsProduct;

@Repository
public interface EsProductRepository extends ElasticsearchRepository<EsProduct, String> {
    Page<EsProduct> findByIsActiveTrue(Pageable pageable);
    Page<EsProduct> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<EsProduct> findByNameContainingIgnoreCaseAndIsActiveTrue(String name, Pageable pageable);
    Page<EsProduct> findBySearchTextContainingIgnoreCaseAndIsActiveTrue(String searchText, Pageable pageable);
    Page<EsProduct> findByCategoryIdAndIsActiveTrue(String categoryId, Pageable pageable);
    Page<EsProduct> findBySupplierIdAndIsActiveTrue(String supplierId, Pageable pageable);
    Page<EsProduct> findByMinDiscountedPriceBetweenAndIsActiveTrue(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
    default Page<EsProduct> findByPriceRangeAndIsActiveTrue(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return findByMinDiscountedPriceBetweenAndIsActiveTrue(minPrice, maxPrice, pageable);
    }
    Page<EsProduct> findByIsFeaturedTrueAndIsActiveTrue(Pageable pageable);
    Page<EsProduct> findByInStockTrueAndIsActiveTrue(Pageable pageable);
    long countByIsActiveTrue();
}
*/
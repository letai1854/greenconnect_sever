package com.greenconnect.greenconnect_api.specifications;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.greenconnect.greenconnect_api.dtos.request.FilterProductsRequest;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.PromotionCampaign;
import com.greenconnect.greenconnect_api.entities.PromotionProduct;
import com.greenconnect.greenconnect_api.enums.PriceRange;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

/**
 * Specification builder cho FilterProductsRequest
 * Kết hợp TẤT CẢ các filter (categoryIds, supplierIds, priceRange) vào mọi query
 */
public final class FilterProductSpecification {

    private FilterProductSpecification() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }

    /**
     * Tạo Specification từ FilterProductsRequest - áp dụng TẤT CẢ các filter
     */
    public static Specification<Product> buildSpecification(FilterProductsRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join với ProductVariant để lọc theo giá
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            predicates.add(criteriaBuilder.equal(variantJoin.get("isActive"), true));

            // 1. Lọc theo isActive (mặc định true)
            if (request.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), request.getIsActive()));
            }

            // 2. ⭐ Lọc theo CAMPAIGN SLUG (join với PromotionProduct + PromotionCampaign)
            if (request.getCampaignSlug() != null && !request.getCampaignSlug().isEmpty()) {
                // Subquery: Tìm products có trong campaign theo slug
                var subquery = query.subquery(UUID.class);
                var subRoot = subquery.from(PromotionProduct.class);
                var campaignJoin = subRoot.join("campaign", JoinType.INNER);
                
                subquery.select(subRoot.get("product").get("id"))
                    .where(
                        criteriaBuilder.equal(campaignJoin.get("slug"), request.getCampaignSlug()),
                        criteriaBuilder.equal(campaignJoin.get("isActive"), true)
                    );
                
                predicates.add(root.get("id").in(subquery));
            }

            // 3. Lọc theo isNew (sản phẩm mới nhất) - không filter ở đây, chỉ đánh dấu
            // Logic sắp xếp sẽ xử lý trong Service layer

            // 4. Lọc theo isBestSeller (sản phẩm bán chạy)
            // Logic sắp xếp sẽ xử lý trong Service layer

            // 5. ⭐ Lọc theo isOnSale (FLASH_SALE campaign - join với PromotionProduct)
            if (request.getIsOnSale() != null && request.getIsOnSale()) {
                // Subquery: Tìm products có trong FLASH_SALE campaign đang active
                var subquery = query.subquery(UUID.class);
                var subRoot = subquery.from(PromotionProduct.class);
                var campaignJoin = subRoot.join("campaign", JoinType.INNER);
                
                subquery.select(subRoot.get("product").get("id"))
                    .where(
                        criteriaBuilder.equal(campaignJoin.get("isActive"), true),
                        criteriaBuilder.equal(campaignJoin.get("campaignType"), 
                            com.greenconnect.greenconnect_api.enums.CampaignType.FLASH_SALE),
                        criteriaBuilder.lessThanOrEqualTo(campaignJoin.get("startDate"), 
                            java.time.LocalDateTime.now()),
                        criteriaBuilder.greaterThanOrEqualTo(campaignJoin.get("endDate"), 
                            java.time.LocalDateTime.now())
                    );
                
                predicates.add(root.get("id").in(subquery));
            }

            // 6. Lọc theo isFeatured (sản phẩm nổi bật)
            if (request.getIsFeatured() != null && request.getIsFeatured()) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), true));
            }

            // 7. ⭐ Lọc theo isHighRating (sắp xếp theo rating giảm dần - xử lý ở Service layer)
            // Không filter ở đây, chỉ đánh dấu để sort trong Service

            // 8. ⭐ Lọc theo DANH MỤC (categoryIds) - áp dụng cho MỌI trường hợp
            if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
                predicates.add(root.get("category").get("id").in(request.getCategoryIds()));
            }

            // 9. ⭐ Lọc theo NHÀ CUNG CẤP (supplierIds) - áp dụng cho MỌI trường hợp
            if (request.getSupplierIds() != null && !request.getSupplierIds().isEmpty()) {
                predicates.add(root.get("supplier").get("id").in(request.getSupplierIds()));
            }

            // 10. ⭐ Lọc theo KHOẢNG GIÁ (priceRange) - áp dụng cho MỌI trường hợp
            if (request.getPriceRange() != null && request.getPriceRange() != PriceRange.ALL) {
                BigDecimal rangeMin = request.getPriceRange().getMinPrice();
                BigDecimal rangeMax = request.getPriceRange().getMaxPrice();
                
                if (rangeMin != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), rangeMin));
                }
                if (rangeMax != null) {
                    predicates.add(criteriaBuilder.lessThan(variantJoin.get("price"), rangeMax));
                }
            }

            // Distinct để tránh duplicate khi join với variants/promotions
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Specification riêng cho campaign products với đầy đủ filter
     */
    public static Specification<Product> campaignProducts(String campaignSlug, List<UUID> categoryIds, 
                                                          List<UUID> supplierIds, PriceRange priceRange) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join với PromotionProduct và PromotionCampaign
            Join<Product, PromotionProduct> promotionProductJoin = root.join("promotionProducts", JoinType.INNER);
            Join<PromotionProduct, PromotionCampaign> campaignJoin = promotionProductJoin.join("promotionCampaign", JoinType.INNER);
            
            // Campaign slug filter
            predicates.add(criteriaBuilder.equal(campaignJoin.get("slug"), campaignSlug));
            predicates.add(criteriaBuilder.equal(campaignJoin.get("isActive"), true));
            predicates.add(criteriaBuilder.equal(root.get("isActive"), true));

            // Join với ProductVariant
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            predicates.add(criteriaBuilder.equal(variantJoin.get("isActive"), true));

            // ⭐ Áp dụng category filter
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // ⭐ Áp dụng supplier filter
            if (supplierIds != null && !supplierIds.isEmpty()) {
                predicates.add(root.get("supplier").get("id").in(supplierIds));
            }

            // ⭐ Áp dụng price range filter
            if (priceRange != null && priceRange != PriceRange.ALL) {
                BigDecimal rangeMin = priceRange.getMinPrice();
                BigDecimal rangeMax = priceRange.getMaxPrice();
                
                if (rangeMin != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), rangeMin));
                }
                if (rangeMax != null) {
                    predicates.add(criteriaBuilder.lessThan(variantJoin.get("price"), rangeMax));
                }
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Specification cho flash sale products với đầy đủ filter
     */
    public static Specification<Product> flashSaleProducts(List<UUID> categoryIds, 
                                                           List<UUID> supplierIds, 
                                                           PriceRange priceRange) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join với ProductVariant
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            
            // Flash sale filter
            predicates.add(criteriaBuilder.isNotNull(variantJoin.get("discountPrice")));
            predicates.add(criteriaBuilder.greaterThan(variantJoin.get("discountPrice"), BigDecimal.ZERO));
            predicates.add(criteriaBuilder.equal(root.get("isActive"), true));
            predicates.add(criteriaBuilder.equal(variantJoin.get("isActive"), true));

            // ⭐ Áp dụng category filter
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }

            // ⭐ Áp dụng supplier filter
            if (supplierIds != null && !supplierIds.isEmpty()) {
                predicates.add(root.get("supplier").get("id").in(supplierIds));
            }

            // ⭐ Áp dụng price range filter
            if (priceRange != null && priceRange != PriceRange.ALL) {
                BigDecimal rangeMin = priceRange.getMinPrice();
                BigDecimal rangeMax = priceRange.getMaxPrice();
                
                if (rangeMin != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), rangeMin));
                }
                if (rangeMax != null) {
                    predicates.add(criteriaBuilder.lessThan(variantJoin.get("price"), rangeMax));
                }
            }

            query.distinct(true);
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}

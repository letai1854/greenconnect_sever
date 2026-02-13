package com.greenconnect.greenconnect_api.specifications;

import org.springframework.data.jpa.domain.Specification;

import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.dtos.request.ProductFilterRequest;
import com.greenconnect.greenconnect_api.enums.PriceRange;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Specification builder để tạo query động cho Product
 * Hỗ trợ lọc theo nhiều tiêu chí khác nhau
 */
public final class ProductSpecification {

    private ProductSpecification() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }

    /**
     * Tạo Specification từ ProductFilterRequest
     */
    public static Specification<Product> buildSpecification(ProductFilterRequest filter) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Join với ProductVariant để lọc theo giá và stock
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);

            // Lọc theo từ khóa trong tên sản phẩm
            if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
                predicates.add(
                    criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + filter.getKeyword().toLowerCase().trim() + "%"
                    )
                );
            }

            // Lọc theo danh mục
            if (filter.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), filter.getCategoryId()));
            }

            // Lọc theo nhiều nhà cung cấp (supplierIds)
            if (filter.getSupplierIds() != null && !filter.getSupplierIds().isEmpty()) {
                predicates.add(root.get("supplier").get("id").in(filter.getSupplierIds()));
            }

            // ⭐ Lọc theo khoảng giá (priceRange enum) - ưu tiên hơn minPrice/maxPrice tùy chỉnh
            if (filter.getPriceRange() != null && filter.getPriceRange() != PriceRange.ALL) {
                BigDecimal rangeMin = filter.getPriceRange().getMinPrice();
                BigDecimal rangeMax = filter.getPriceRange().getMaxPrice();
                
                if (rangeMin != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), rangeMin));
                }
                if (rangeMax != null) {
                    predicates.add(criteriaBuilder.lessThan(variantJoin.get("price"), rangeMax));
                }
            } 
            // Nếu không có priceRange, sử dụng minPrice/maxPrice tùy chỉnh
            else {
                // Lọc theo giá tối thiểu (từ variant)
                if (filter.getMinPrice() != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(variantJoin.get("price"), filter.getMinPrice()));
                }

                // Lọc theo giá tối đa (từ variant)
                if (filter.getMaxPrice() != null) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(variantJoin.get("price"), filter.getMaxPrice()));
                }
            }

            // Lọc sản phẩm đang khuyến mãi
            if (filter.getIsOnSale() != null && filter.getIsOnSale()) {
                predicates.add(criteriaBuilder.isNotNull(variantJoin.get("discountPrice")));
                predicates.add(criteriaBuilder.greaterThan(variantJoin.get("discountPrice"), BigDecimal.ZERO));
            }

            // Lọc sản phẩm nổi bật
            if (filter.getIsFeatured() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFeatured"), filter.getIsFeatured()));
            }

            // Lọc sản phẩm có hàng
            if (filter.getInStock() != null && filter.getInStock()) {
                predicates.add(criteriaBuilder.greaterThan(variantJoin.get("stockQuantity"), 0));
            }

            // Lọc theo rating tối thiểu
            if (filter.getMinRating() != null && filter.getMinRating() != com.greenconnect.greenconnect_api.enums.MinimumRating.ALL) {
                BigDecimal ratingValue = filter.getMinRating().getValue();
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("averageRating"), ratingValue));
            }

            // Lọc sản phẩm đang hoạt động
            if (filter.getIsActive() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), filter.getIsActive()));
            }

            // Đảm bảo variant cũng đang hoạt động
            predicates.add(criteriaBuilder.equal(variantJoin.get("isActive"), true));

            // Distinct để tránh duplicate khi join với variants
            query.distinct(true);

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Specification đơn giản để lấy tất cả sản phẩm active
     */
    public static Specification<Product> activeProducts() {
        return (root, query, criteriaBuilder) -> {
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            query.distinct(true);
            
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("isActive"), true),
                criteriaBuilder.equal(variantJoin.get("isActive"), true)
            );
        };
    }

    /**
     * Specification để lấy sản phẩm nổi bật
     */
    public static Specification<Product> featuredProducts() {
        return (root, query, criteriaBuilder) -> {
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            query.distinct(true);
            
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("isActive"), true),
                criteriaBuilder.equal(root.get("isFeatured"), true),
                criteriaBuilder.equal(variantJoin.get("isActive"), true)
            );
        };
    }

    /**
     * Specification để lấy sản phẩm mới nhất
     */
    public static Specification<Product> newestProducts() {
        return (root, query, criteriaBuilder) -> {
            Join<Product, ProductVariant> variantJoin = root.join("productVariants", JoinType.LEFT);
            query.distinct(true);
            query.orderBy(criteriaBuilder.desc(root.get("createdAt")));
            
            return criteriaBuilder.and(
                criteriaBuilder.equal(root.get("isActive"), true),
                criteriaBuilder.equal(variantJoin.get("isActive"), true)
            );
        };
    }
}
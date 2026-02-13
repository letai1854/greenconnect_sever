package com.greenconnect.greenconnect_api.mappers;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductImage;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.repositories.ProductImageRepository;

/**
 * Mapper utility class for Product entity conversions.
 * <p>Chuyển đổi giữa các layers: Entity → Response DTOs</p>
 * <p>Sử dụng static methods để dễ dàng import và sử dụng trong Services.</p>
 */
@Component
public final class ProductMapper {
    
    // Remove private constructor for Spring to instantiate
    public ProductMapper() {
    }
    
    /**
     * Chuyển đổi Product entity sang ProductResponse DTO
     * @param product Product entity
     * @param variants Danh sách ProductVariant của product
     * @param productImageRepository Repository để lấy images
     * @return ProductResponse DTO
     */
    public static ProductResponse toProductResponse(Product product, 
                                                   List<ProductVariant> variants,
                                                   ProductImageRepository productImageRepository) {
        
        // Build category info
        ProductResponse.CategoryInfo categoryInfo = ProductResponse.CategoryInfo.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .imageUrl(product.getCategory().getImageUrl())
                .build();
        
        // Build supplier info
        ProductResponse.SupplierInfo supplierInfo = ProductResponse.SupplierInfo.builder()
                .id(product.getSupplier().getId())
                .name(product.getSupplier().getName())
                .contactEmail(product.getSupplier().getEmail())
                .phoneNumber(product.getSupplier().getPhoneNumber())
                .build();
        
        // Get images for the product (all variants share the same images)
        List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductResponse.ProductImageInfo> imageInfos = productImages.stream()
                .map(image -> ProductResponse.ProductImageInfo.builder()
                        .id(image.getId())
                        .mediaType(image.getMediaType().name())
                        .mediaUrl(image.getMediaUrl())
                        .displayOrder(image.getDisplayOrder())
                        .isMain(image.getIsMain())
                        .build())
                .collect(Collectors.toList());
        
        // Build variants info
        java.util.List<ProductResponse.ProductVariantInfo> variantInfos = variants.stream()
                .map(variant -> {
                    
                                        java.math.BigDecimal price = variant.getPrice();
                                        java.math.BigDecimal discountPct = variant.getDiscountPercentage();
                                        java.math.BigDecimal discountedPrice = variant.getDiscountedPrice();

                                        java.math.BigDecimal minValue = null;
                                        java.math.BigDecimal maxValue = null;
                                        if (price != null) {
                                                if (discountPct != null && discountPct.compareTo(java.math.BigDecimal.ZERO) != 0) {
                                                        // price * (1 - discountPct/100)
                                                        java.math.BigDecimal factor = java.math.BigDecimal.ONE.subtract(discountPct.divide(new java.math.BigDecimal("100")));
                                                        java.math.BigDecimal computed = price.multiply(factor);
                                                        minValue = computed;
                                                        maxValue = computed;
                                                } else {
                                                        minValue = price;
                                                        maxValue = price;
                                                }
                                        }

                                        return ProductResponse.ProductVariantInfo.builder()
                                                        .id(variant.getId())
                                                        .name(variant.getName())
                                                        .sku(variant.getSku())
                                                        .price(variant.getPrice())
                                                        .discountPercentage(variant.getDiscountPercentage())
                                                        .discountedPrice(discountedPrice)
                                                        .minValue(minValue)
                                                        .maxValue(maxValue)
                                                        .discountAmount(variant.getDiscountAmount())
                                                        .stockQuantity(variant.getStockQuantity())
                                                        .unit(variant.getUnit())
                                                        // ⚠️ mainImageUrl đã chuyển lên product level
                                                        .isActive(variant.getIsActive())
                                                        .isDefault(variant.getIsDefault())
                                                        .build();
                })
                .collect(Collectors.toList());
        
                // compute product-level min/max across variants
                java.math.BigDecimal prodMin = null;
                java.math.BigDecimal prodMax = null;
                for (ProductResponse.ProductVariantInfo v : variantInfos) {
                        if (v.getMinValue() != null) {
                                if (prodMin == null || v.getMinValue().compareTo(prodMin) < 0) prodMin = v.getMinValue();
                                if (prodMax == null || v.getMaxValue().compareTo(prodMax) > 0) prodMax = v.getMaxValue();
                        }
                }

                // Get main image URL (first image with isMain = true)
                String mainImageUrl = product.getMainImageUrl(); // Cached field from Product entity
                
                return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .sellNumber(product.getSellNumber()) // ⚡ Số lượng đã bán
                .createdAt(product.getCreatedAt())
                                .updatedAt(product.getUpdatedAt())
                                .minValue(prodMin)
                                .maxValue(prodMax)
                .category(categoryInfo)
                .supplier(supplierInfo)
                .mainImageUrl(mainImageUrl)  // ⭐ Main image URL ở product level
                .images(imageInfos)  // ⭐ All images ở product level
                .variants(variantInfos)
                .build();
    }
    
    /**
     * Chuyển đổi Product entity sang ProductResponse DTO (dùng cho FavoriteService)
     * @param product Product entity with lazy-loaded associations
     * @return ProductResponse DTO
     */
    public static ProductResponse toProductResponse(Product product) {
        // Build category info
        ProductResponse.CategoryInfo categoryInfo = null;
        if (product.getCategory() != null) {
            categoryInfo = ProductResponse.CategoryInfo.builder()
                    .id(product.getCategory().getId())
                    .name(product.getCategory().getName())
                    .imageUrl(product.getCategory().getImageUrl())
                    .build();
        }
        
        // Build supplier info
        ProductResponse.SupplierInfo supplierInfo = null;
        if (product.getSupplier() != null) {
            supplierInfo = ProductResponse.SupplierInfo.builder()
                    .id(product.getSupplier().getId())
                    .name(product.getSupplier().getName())
                    .contactEmail(product.getSupplier().getEmail())
                    .phoneNumber(product.getSupplier().getPhoneNumber())
                    .build();
        }
        
        // Get images
        List<ProductResponse.ProductImageInfo> imageInfos = null;
        if (product.getProductImages() != null) {
            imageInfos = product.getProductImages().stream()
                    .map(image -> ProductResponse.ProductImageInfo.builder()
                            .id(image.getId())
                            .mediaType(image.getMediaType().name())
                            .mediaUrl(image.getMediaUrl())
                            .displayOrder(image.getDisplayOrder())
                            .isMain(image.getIsMain())
                            .build())
                    .collect(Collectors.toList());
        }
        
        // Build variants info
        List<ProductResponse.ProductVariantInfo> variantInfos = null;
        java.math.BigDecimal prodMin = null;
        java.math.BigDecimal prodMax = null;
        
        if (product.getProductVariants() != null) {
            variantInfos = product.getProductVariants().stream()
                    .map(variant -> {
                        java.math.BigDecimal price = variant.getPrice();
                        java.math.BigDecimal discountPct = variant.getDiscountPercentage();
                        java.math.BigDecimal discountedPrice = variant.getDiscountedPrice();
                        
                        java.math.BigDecimal minValue = null;
                        java.math.BigDecimal maxValue = null;
                        if (price != null) {
                            if (discountPct != null && discountPct.compareTo(java.math.BigDecimal.ZERO) != 0) {
                                java.math.BigDecimal factor = java.math.BigDecimal.ONE.subtract(discountPct.divide(new java.math.BigDecimal("100")));
                                java.math.BigDecimal computed = price.multiply(factor);
                                minValue = computed;
                                maxValue = computed;
                            } else {
                                minValue = price;
                                maxValue = price;
                            }
                        }
                        
                        return ProductResponse.ProductVariantInfo.builder()
                                .id(variant.getId())
                                .name(variant.getName())
                                .sku(variant.getSku())
                                .price(variant.getPrice())
                                .discountPercentage(variant.getDiscountPercentage())
                                .discountedPrice(discountedPrice)
                                .minValue(minValue)
                                .maxValue(maxValue)
                                .discountAmount(variant.getDiscountAmount())
                                .stockQuantity(variant.getStockQuantity())
                                .unit(variant.getUnit())
                                .isActive(variant.getIsActive())
                                .isDefault(variant.getIsDefault())
                                .build();
                    })
                    .collect(Collectors.toList());
            
            // Compute product-level min/max across variants
            for (ProductResponse.ProductVariantInfo v : variantInfos) {
                if (v.getMinValue() != null) {
                    if (prodMin == null || v.getMinValue().compareTo(prodMin) < 0) prodMin = v.getMinValue();
                    if (prodMax == null || v.getMaxValue().compareTo(prodMax) > 0) prodMax = v.getMaxValue();
                }
            }
        }
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .sellNumber(product.getSellNumber()) // ⚡ Số lượng đã bán
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .minValue(prodMin)
                .maxValue(prodMax)
                .category(categoryInfo)
                .supplier(supplierInfo)
                .mainImageUrl(product.getMainImageUrl())
                .images(imageInfos)
                .variants(variantInfos)
                .build();
    }
}
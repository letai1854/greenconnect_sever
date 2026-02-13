package com.greenconnect.greenconnect_api.elasticsearch.services;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.repositories.FavoriteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ElasticsearchProductResponseEnricher
 * 
 * Service helper để thêm thông tin runtime vào ProductResponse
 * - isFavorited: đánh dấu sản phẩm có trong favorite của user hay không
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchProductResponseEnricher {
    
    private final FavoriteRepository favoriteRepository;
    
    /**
     * Thêm thông tin isFavorited cho 1 ProductResponse
     * 
     * @param product ProductResponse cần enriched
     * @param userId UUID của user hiện tại (null nếu không login)
     */
    public void enrichProductWithFavorite(ProductResponse product, UUID userId) {
        if (userId == null || product == null) {
            if (product != null) {
                product.setIsFavorited(false);
            }
            return;
        }
        
        try {
            boolean isFavorited = favoriteRepository.existsByUserIdAndProductId(userId, product.getId());
            product.setIsFavorited(isFavorited);
        } catch (Exception e) {
            log.error("❌ Lỗi check favorite cho product {}: {}", product.getId(), e.getMessage());
            product.setIsFavorited(false);
        }
    }
    
    /**
     * Thêm thông tin isFavorited cho danh sách ProductResponse
     * (Tối ưu: query 1 lần để lấy tất cả favorites của user)
     * 
     * @param products List ProductResponse cần enriched
     * @param userId UUID của user hiện tại (null nếu không login)
     */
    public void enrichProductsWithFavorites(List<ProductResponse> products, UUID userId) {
        if (userId == null || products == null || products.isEmpty()) {
            if (products != null) {
                products.forEach(p -> p.setIsFavorited(false));
            }
            return;
        }
        
        try {
            // Lấy danh sách product IDs
            List<UUID> productIds = products.stream()
                    .map(ProductResponse::getId)
                    .collect(Collectors.toList());
            
            // Query 1 lần để lấy tất cả favorites của user cho các products này
            Set<UUID> favoritedProductIds = favoriteRepository
                    .findByUserIdAndProductIdInAndIsActiveTrue(userId, productIds)
                    .stream()
                    .map(fav -> fav.getProduct().getId())
                    .collect(Collectors.toSet());
            
            // Set isFavorited cho từng product
            products.forEach(product -> {
                product.setIsFavorited(favoritedProductIds.contains(product.getId()));
            });
            
        } catch (Exception e) {
            log.error("❌ Lỗi check favorites cho user {}: {}", userId, e.getMessage());
            products.forEach(p -> p.setIsFavorited(false));
        }
    }
    
    /**
     * Thêm thông tin isFavorited cho Page của ProductResponse
     * 
     * @param productPage Page<ProductResponse> cần enriched
     * @param userId UUID của user hiện tại (null nếu không login)
     */
    public void enrichProductPageWithFavorites(Page<ProductResponse> productPage, UUID userId) {
        if (productPage == null || !productPage.hasContent()) {
            return;
        }
        
        enrichProductsWithFavorites(productPage.getContent(), userId);
    }
}

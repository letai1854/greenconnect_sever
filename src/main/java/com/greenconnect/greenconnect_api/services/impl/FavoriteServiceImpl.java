package com.greenconnect.greenconnect_api.services.impl;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.entities.Favorite;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.mappers.ProductMapper;
import com.greenconnect.greenconnect_api.repositories.FavoriteRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.FavoriteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * FavoriteServiceImpl - Quản lý sản phẩm yêu thích
 * Giới hạn tối đa 20 sản phẩm yêu thích cho mỗi user
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FavoriteServiceImpl implements FavoriteService {
    
    private static final int MAX_FAVORITES = 20;
    
    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.greenconnect.greenconnect_api.elasticsearch.services.ElasticsearchSyncService elasticsearchSyncService;
    
    @Override
    @Transactional
    public void addFavorite(UUID userId, UUID productId) {
        log.info("Thêm sản phẩm {} vào yêu thích của user {}", productId, userId);
        
        // Kiểm tra xem đã yêu thích chưa
        if (favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            log.warn("Sản phẩm {} đã có trong danh sách yêu thích của user {}", productId, userId);
            throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
        }
        
        // Kiểm tra giới hạn 20 sản phẩm
        long currentCount = favoriteRepository.countByUserIdAndIsActiveTrue(userId);
        if (currentCount >= MAX_FAVORITES) {
            log.warn("User {} đã đạt giới hạn {} sản phẩm yêu thích", userId, MAX_FAVORITES);
            throw new BusinessException(ErrorCode.FAVORITE_LIMIT_EXCEEDED);
        }
        
        // Kiểm tra user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Kiểm tra product tồn tại và active
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        
        if (!product.getIsActive()) {
            log.warn("Không thể thêm sản phẩm {} vì đã bị vô hiệu hóa", productId);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_ACTIVE);
        }
        
        // Tạo favorite mới
        Favorite favorite = Favorite.builder()
                .user(user)
                .product(product)
                .isActive(true)
                .build();
        
        favoriteRepository.save(favorite);
        log.info("✅ Đã thêm sản phẩm {} vào yêu thích của user {}", productId, userId);
        
        // Đồng bộ với Elasticsearch (nếu có)
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProductFavoriteCount(productId);
            } catch (Exception e) {
                log.warn("⚠️ Không thể đồng bộ favorite count với Elasticsearch: {}", e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional
    public void removeFavorite(UUID userId, UUID productId) {
        log.info("Xóa sản phẩm {} khỏi yêu thích của user {}", productId, userId);
        
        // Kiểm tra xem có tồn tại không
        if (!favoriteRepository.existsByUserIdAndProductId(userId, productId)) {
            log.warn("Sản phẩm {} không có trong danh sách yêu thích của user {}", productId, userId);
            throw new BusinessException(ErrorCode.FAVORITE_NOT_FOUND);
        }
        
        favoriteRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("✅ Đã xóa sản phẩm {} khỏi yêu thích của user {}", productId, userId);
        
        // Đồng bộ với Elasticsearch (nếu có)
        if (elasticsearchSyncService != null) {
            try {
                elasticsearchSyncService.syncProductFavoriteCount(productId);
            } catch (Exception e) {
                log.warn("⚠️ Không thể đồng bộ favorite count với Elasticsearch: {}", e.getMessage());
            }
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> listFavorites(UUID userId, Pageable pageable) {
        log.info("Lấy danh sách yêu thích của user {}, page: {}", userId, pageable.getPageNumber());
        
        Page<Favorite> favorites = favoriteRepository.findByUserIdAndIsActiveTrue(userId, pageable);
        
        return favorites.map(favorite -> {
            Product product = favorite.getProduct();
            return ProductMapper.toProductResponse(product);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean isFavorite(UUID userId, UUID productId) {
        return favoriteRepository.existsByUserIdAndProductId(userId, productId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long countFavorites(UUID userId) {
        return favoriteRepository.countByUserIdAndIsActiveTrue(userId);
    }
}

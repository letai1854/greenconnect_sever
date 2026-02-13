package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;

/**
 * FavoriteService - Service quản lý sản phẩm yêu thích
 * Giới hạn tối đa 20 sản phẩm yêu thích cho mỗi user
 */
public interface FavoriteService {
    
    /**
     * Thêm sản phẩm vào danh sách yêu thích
     * @param userId ID của user
     * @param productId ID của sản phẩm
     * @throws BusinessException nếu đã đạt giới hạn 20 sản phẩm
     */
    void addFavorite(UUID userId, UUID productId);
    
    /**
     * Xóa sản phẩm khỏi danh sách yêu thích
     * @param userId ID của user
     * @param productId ID của sản phẩm
     */
    void removeFavorite(UUID userId, UUID productId);
    
    /**
     * Lấy danh sách sản phẩm yêu thích của user
     * @param userId ID của user
     * @param pageable Thông tin phân trang
     * @return Page chứa danh sách ProductResponse
     */
    Page<ProductResponse> listFavorites(UUID userId, Pageable pageable);
    
    /**
     * Kiểm tra xem sản phẩm có trong danh sách yêu thích không
     * @param userId ID của user
     * @param productId ID của sản phẩm
     * @return true nếu đã yêu thích
     */
    boolean isFavorite(UUID userId, UUID productId);
    
    /**
     * Đếm số lượng sản phẩm yêu thích hiện tại của user
     * @param userId ID của user
     * @return Số lượng sản phẩm yêu thích
     */
    long countFavorites(UUID userId);
}

package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Favorite;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, UUID> {
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    Page<Favorite> findByUserIdAndIsActiveTrue(UUID userId, Pageable pageable);
    void deleteByUserIdAndProductId(UUID userId, UUID productId);
    long countByUserIdAndIsActiveTrue(UUID userId);
    long countByProductIdAndIsActiveTrue(UUID productId);
    
    // Lấy danh sách favorites của user cho nhiều products (dùng cho enrich response)
    java.util.List<Favorite> findByUserIdAndProductIdInAndIsActiveTrue(UUID userId, java.util.List<UUID> productIds);
    
    // Lấy tất cả favorites của user (dùng cho login response)
    java.util.List<Favorite> findByUserIdAndIsActiveTrue(UUID userId);
}

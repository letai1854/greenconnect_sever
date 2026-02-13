package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.PromotionCampaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionCampaignRepository extends JpaRepository<PromotionCampaign, UUID> {
    boolean existsBySlug(String slug);
    
    Optional<PromotionCampaign> findBySlugAndIsActiveTrueAndStartDateBeforeAndEndDateAfter(
        String slug, LocalDateTime now1, LocalDateTime now2);
    
    /**
     * Tìm campaigns active nhưng đã hết hạn (endDate < now)
     * Dùng cho scheduler tự động remove discount
     */
    List<PromotionCampaign> findByIsActiveTrueAndEndDateBefore(LocalDateTime endDate);
    
    /**
     * Tìm campaigns inactive và đã hết hạn lâu (>30 ngày)
     * Dùng cho cleanup log
     */
    List<PromotionCampaign> findByIsActiveFalseAndEndDateBefore(LocalDateTime endDate);
    
    // Pagination methods
    Page<PromotionCampaign> findByIsActiveTrue(Pageable pageable);
    
    Page<PromotionCampaign> findByIsActiveFalse(Pageable pageable);
    
    Page<PromotionCampaign> findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(
        LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    Page<PromotionCampaign> findByEndDateBefore(LocalDateTime endDate, Pageable pageable);
    
    // Search methods
    @Query("SELECT c FROM PromotionCampaign c WHERE c.isActive = true AND " +
           "(LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<PromotionCampaign> searchActiveCampaignsByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT c FROM PromotionCampaign c WHERE c.isActive = false AND " +
           "(LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<PromotionCampaign> searchInactiveCampaignsByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT c FROM PromotionCampaign c WHERE " +
           "(LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<PromotionCampaign> searchAllCampaignsByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT c FROM PromotionCampaign c WHERE c.isActive = true AND " +
           "c.startDate <= :now AND c.endDate >= :now AND " +
           "(LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<PromotionCampaign> searchValidCampaignsByKeyword(@Param("keyword") String keyword, @Param("now") LocalDateTime now, Pageable pageable);
    
    @Query("SELECT c FROM PromotionCampaign c WHERE c.endDate < :now AND " +
           "(LOWER(c.campaignName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.slug) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.createdAt DESC")
    Page<PromotionCampaign> searchExpiredCampaignsByKeyword(@Param("keyword") String keyword, @Param("now") LocalDateTime now, Pageable pageable);
    
    /**
     * Lấy displayOrder lớn nhất hiện tại
     * Dùng để tự động tăng displayOrder khi tạo campaign mới
     */
    @Query("SELECT MAX(c.displayOrder) FROM PromotionCampaign c")
    Integer findMaxDisplayOrder();
    
    /**
     * Lấy campaign mới nhất theo type và còn hiệu lực
     * @param campaignType Loại campaign (BULK_PURCHASE, FLASH_SALE, etc.)
     * @param now Thời gian hiện tại
     * @return Campaign mới nhất theo type còn hiệu lực
     */
    @Query("SELECT c FROM PromotionCampaign c WHERE c.campaignType = :campaignType " +
           "AND c.isActive = true AND c.startDate <= :now AND c.endDate >= :now " +
           "ORDER BY c.createdAt DESC")
    Optional<PromotionCampaign> findLatestValidCampaignByType(
        @Param("campaignType") com.greenconnect.greenconnect_api.enums.CampaignType campaignType,
        @Param("now") LocalDateTime now);
}
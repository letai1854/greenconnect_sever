package com.greenconnect.greenconnect_api.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.PromotionCampaign;
import com.greenconnect.greenconnect_api.repositories.PromotionCampaignRepository;
import com.greenconnect.greenconnect_api.services.PromotionCampaignService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduler tự động xử lý campaigns hết hạn
 * - Mỗi 5 phút: Remove discount từ variants của campaigns expired
 * - Mỗi ngày 00:05: Log campaigns cũ cần cleanup
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PromotionCampaignScheduler {

    private final PromotionCampaignRepository promotionCampaignRepository;
    private final PromotionCampaignService promotionCampaignService;

    /**
     * Chạy mỗi 5 phút để kiểm tra campaigns hết hạn
     * Tự động remove discount từ product variants
     */
    @Scheduled(cron = "0 */5 * * * *") // Mỗi 5 phút
    @Transactional
    public void autoRemoveExpiredCampaignDiscounts() {
        log.info("⏰ [SCHEDULER] Bắt đầu kiểm tra campaigns hết hạn...");
        
        LocalDateTime now = LocalDateTime.now();
        
        // Tìm các campaigns đã hết hạn nhưng vẫn active
        List<PromotionCampaign> expiredCampaigns = promotionCampaignRepository
            .findByIsActiveTrueAndEndDateBefore(now);
        
        if (expiredCampaigns.isEmpty()) {
            log.info("✅ [SCHEDULER] Không có campaign nào hết hạn");
            return;
        }
        
        log.warn("⚠️ [SCHEDULER] Tìm thấy {} campaigns hết hạn", expiredCampaigns.size());
        
        for (PromotionCampaign campaign : expiredCampaigns) {
            try {
                log.info("🔄 [SCHEDULER] Xử lý campaign hết hạn: {} (ID: {})", 
                    campaign.getCampaignName(), campaign.getId());
                
                // 1. Remove discount từ tất cả product variants
                promotionCampaignService.removeDiscountFromProductVariants(campaign.getId());
                
                // 2. Đánh dấu campaign là inactive
                campaign.setIsActive(false);
                promotionCampaignRepository.save(campaign);
                
                log.info("✅ [SCHEDULER] Đã xử lý xong campaign: {}", campaign.getCampaignName());
                
            } catch (Exception e) {
                log.error("❌ [SCHEDULER] Lỗi khi xử lý campaign {}: {}", 
                    campaign.getId(), e.getMessage(), e);
            }
        }
        
        log.info("🎉 [SCHEDULER] Hoàn thành kiểm tra campaigns hết hạn");
    }
    
    /**
     * Chạy mỗi ngày lúc 00:05 để log campaigns cũ
     */
    @Scheduled(cron = "0 5 0 * * *") // 00:05 mỗi ngày
    @Transactional(readOnly = true)
    public void logOldExpiredCampaigns() {
        log.info("🧹 [CLEANUP] Bắt đầu kiểm tra campaigns cũ...");
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        List<PromotionCampaign> oldCampaigns = promotionCampaignRepository
            .findByIsActiveFalseAndEndDateBefore(thirtyDaysAgo);
        
        if (oldCampaigns.isEmpty()) {
            log.info("✅ [CLEANUP] Không có campaign cũ (>30 ngày)");
            return;
        }
        
        log.info("📋 [CLEANUP] Tìm thấy {} campaigns cũ (>30 ngày):", oldCampaigns.size());
        
        for (PromotionCampaign campaign : oldCampaigns) {
            log.info("  📝 {} (ID: {}, kết thúc: {})", 
                campaign.getCampaignName(), campaign.getId(), campaign.getEndDate());
        }
        
        log.info("ℹ️ [CLEANUP] Các campaigns trên có thể được xóa thủ công nếu cần");
    }
}

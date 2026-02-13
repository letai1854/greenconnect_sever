package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.PromotionProduct;
import com.greenconnect.greenconnect_api.entities.PromotionProductId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, PromotionProductId> {
    // Không cần phương thức custom cho việc tạo mới vì đã có Cascade
    org.springframework.data.domain.Page<PromotionProduct> findByCampaignId(java.util.UUID campaignId, org.springframework.data.domain.Pageable pageable);
}
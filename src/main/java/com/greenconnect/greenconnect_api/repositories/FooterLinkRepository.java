package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.FooterLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FooterLinkRepository extends JpaRepository<FooterLink, UUID> {
    
    /**
     * Tìm tất cả links theo section ID
     */
    List<FooterLink> findBySectionId(UUID sectionId);
    
    /**
     * Đếm số lượng links trong một section
     */
    long countBySectionId(UUID sectionId);
    
    /**
     * Xóa tất cả links theo section ID (cascade delete backup)
     */
    void deleteBySectionId(UUID sectionId);
}

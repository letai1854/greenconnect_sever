package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.FooterSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FooterSectionRepository extends JpaRepository<FooterSection, UUID> {
    
    /**
     * Lấy tất cả sections với links (eager loading để tránh N+1)
     * Chỉ lấy các section và link đang active
     */
    @Query("SELECT DISTINCT fs FROM FooterSection fs " +
           "LEFT JOIN FETCH fs.links fl " +
           "WHERE fs.isActive = true AND (fl.isActive = true OR fl.isActive IS NULL) " +
           "ORDER BY fs.sortOrder ASC")
    List<FooterSection> findAllActiveSectionsWithLinks();
    
    /**
     * Lấy tất cả sections với links (bao gồm cả inactive)
     */
    @Query("SELECT DISTINCT fs FROM FooterSection fs " +
           "LEFT JOIN FETCH fs.links " +
           "ORDER BY fs.sortOrder ASC")
    List<FooterSection> findAllSectionsWithLinks();
    
    /**
     * Sắp xếp theo sort_order
     */
    List<FooterSection> findAllByOrderBySortOrderAsc();
}

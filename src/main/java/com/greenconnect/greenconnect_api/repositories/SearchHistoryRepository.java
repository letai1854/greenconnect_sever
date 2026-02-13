package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.SearchHistory;

/**
 * Repository cho SearchHistory
 */
@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, UUID> {
    
    /**
     * Lấy 3 lịch sử tìm kiếm mới nhất của user
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.user.id = :userId ORDER BY sh.createdAt DESC LIMIT 3")
    List<SearchHistory> findTop3ByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    /**
     * Tìm SearchHistory theo ID và user ID (để verify quyền sở hữu)
     */
    @Query("SELECT sh FROM SearchHistory sh WHERE sh.id = :searchHistoryId AND sh.user.id = :userId")
    Optional<SearchHistory> findByIdAndUserId(@Param("searchHistoryId") UUID searchHistoryId, @Param("userId") UUID userId);
    
    /**
     * Xóa tất cả lịch sử tìm kiếm của user
     */
    @Modifying
    @Query("DELETE FROM SearchHistory sh WHERE sh.user.id = :userId")
    void deleteAllByUserId(@Param("userId") UUID userId);
}

package com.greenconnect.greenconnect_api.services;

import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.dtos.request.SaveSearchHistoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.SearchHistoryResponse;

/**
 * Service interface cho SearchHistory
 */
public interface SearchHistoryService {
    
    /**
     * Lưu lịch sử tìm kiếm của customer
     * 
     * @param request Nội dung tìm kiếm
     * @param userEmail Email của customer
     */
    void saveSearchHistory(SaveSearchHistoryRequest request, String userEmail);
    
    /**
     * Lấy 3 lịch sử tìm kiếm mới nhất của customer
     * 
     * @param userEmail Email của customer
     * @return Danh sách 3 tìm kiếm mới nhất
     */
    List<SearchHistoryResponse> getRecentSearches(String userEmail);
    
    /**
     * Xóa 1 lịch sử tìm kiếm theo ID
     * 
     * @param searchHistoryId ID của lịch sử tìm kiếm
     * @param userEmail Email của customer (để verify quyền sở hữu)
     */
    void deleteSearchHistory(UUID searchHistoryId, String userEmail);
    
    /**
     * Xóa tất cả lịch sử tìm kiếm của customer
     * 
     * @param userEmail Email của customer
     */
    void deleteAllSearchHistory(String userEmail);
}

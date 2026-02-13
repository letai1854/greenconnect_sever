package com.greenconnect.greenconnect_api.services.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.SaveSearchHistoryRequest;
import com.greenconnect.greenconnect_api.dtos.response.SearchHistoryResponse;
import com.greenconnect.greenconnect_api.entities.SearchHistory;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.SearchHistoryRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.SearchHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation cho SearchHistory
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchHistoryServiceImpl implements SearchHistoryService {
    
    private final SearchHistoryRepository searchHistoryRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public void saveSearchHistory(SaveSearchHistoryRequest request, String userEmail) {
        log.info("💾 Lưu lịch sử tìm kiếm: '{}' cho user: {}", request.getSearchKeyword(), userEmail);
        
        // Tìm user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Tạo SearchHistory mới
        SearchHistory searchHistory = SearchHistory.builder()
                .user(user)
                .searchKeyword(request.getSearchKeyword().trim())
                .build();
        
        searchHistoryRepository.save(searchHistory);
        log.info("✅ Đã lưu lịch sử tìm kiếm cho user: {}", userEmail);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getRecentSearches(String userEmail) {
        log.info("🔍 Lấy 3 tìm kiếm mới nhất cho user: {}", userEmail);
        
        // Tìm user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Lấy 3 tìm kiếm mới nhất
        List<SearchHistory> histories = searchHistoryRepository.findTop3ByUserIdOrderByCreatedAtDesc(user.getId());
        
        return histories.stream()
                .map(history -> SearchHistoryResponse.builder()
                        .id(history.getId())
                        .searchKeyword(history.getSearchKeyword())
                        .createdAt(history.getCreatedAt())
                        .build())
                .toList();
    }
    
    @Override
    @Transactional
    public void deleteSearchHistory(UUID searchHistoryId, String userEmail) {
        log.info("🗑️ Xóa lịch sử tìm kiếm ID: {} của user: {}", searchHistoryId, userEmail);
        
        // Tìm user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Tìm SearchHistory và verify quyền sở hữu
        SearchHistory searchHistory = searchHistoryRepository.findByIdAndUserId(searchHistoryId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        
        // Xóa
        searchHistoryRepository.delete(searchHistory);
        log.info("✅ Đã xóa lịch sử tìm kiếm ID: {}", searchHistoryId);
    }
    
    @Override
    @Transactional
    public void deleteAllSearchHistory(String userEmail) {
        log.info("🗑️ Xóa tất cả lịch sử tìm kiếm của user: {}", userEmail);
        
        // Tìm user
        User user = userRepository.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // Xóa tất cả
        searchHistoryRepository.deleteAllByUserId(user.getId());
        log.info("✅ Đã xóa tất cả lịch sử tìm kiếm của user: {}", userEmail);
    }
}

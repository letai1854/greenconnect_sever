package com.greenconnect.greenconnect_api.services.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.RefreshToken;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.RefreshTokenRepository;
import com.greenconnect.greenconnect_api.services.RefreshTokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của RefreshTokenService interface.
 * <p>Xử lý multi-device refresh token management.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    
    @Value("${app.security.max-tokens-per-user:20}")
    private int maxTokensPerUser; // Tối đa 5 tokens mỗi user (5 thiết bị)
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, String tokenHash, String deviceInfo, LocalDateTime expiryDate) {
        log.info("Tạo refresh token mới cho user: {} trên device: {}", user.getEmail(), deviceInfo);
        
        // 1. Tạo refresh token mới
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .deviceInfo(deviceInfo)
                .expiryDate(expiryDate)
                .build();
        
        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        
        // 2. Cleanup tokens cũ nếu vượt quá giới hạn
        cleanupOldTokensForUser(user, maxTokensPerUser);
        
        log.info("Đã tạo refresh token cho user: {} với ID: {}", user.getEmail(), savedToken.getId());
        return savedToken;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(User user, String tokenHash) {
        log.debug("Validate refresh token cho user: {}", user.getEmail());
        
        return refreshTokenRepository.findByUserAndTokenHashAndExpiryDateAfter(
                user, tokenHash, LocalDateTime.now())
                .orElseThrow(() -> {
                    log.warn("Refresh token không hợp lệ hoặc đã hết hạn cho user: {}", user.getEmail());
                    return new BusinessException(ErrorCode.INVALID_TOKEN);
                });
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public boolean revokeRefreshToken(User user, String tokenHash) {
        log.info("Revoke refresh token cho user: {}", user.getEmail());
        
        RefreshToken token = refreshTokenRepository.findByUserAndTokenHashAndExpiryDateAfter(
                user, tokenHash, LocalDateTime.now())
                .orElse(null);
        
        if (token != null) {
            refreshTokenRepository.delete(token);
            log.info("Đã xóa refresh token ID: {} cho user: {}", token.getId(), user.getEmail());
            return true;
        }
        
        log.warn("Không tìm thấy refresh token để xóa cho user: {}", user.getEmail());
        return false;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int revokeAllRefreshTokens(User user) {
        log.info("Revoke tất cả refresh tokens cho user: {}", user.getEmail());
        
        int deletedCount = refreshTokenRepository.deleteByUser(user);
        log.info("Đã xóa {} refresh tokens cho user: {}", deletedCount, user.getEmail());
        return deletedCount;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RefreshToken> getActiveRefreshTokens(User user) {
        return refreshTokenRepository.findByUserAndExpiryDateAfter(user, LocalDateTime.now());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        log.info("Bắt đầu cleanup expired refresh tokens");
        
        LocalDateTime now = LocalDateTime.now();
        List<RefreshToken> expiredTokens = refreshTokenRepository.findAll()
                .stream()
                .filter(token -> token.getExpiryDate().isBefore(now))
                .toList();
        
        int deletedCount = expiredTokens.size();
        if (deletedCount > 0) {
            refreshTokenRepository.deleteAll(expiredTokens);
            log.info("Đã cleanup {} expired refresh tokens", deletedCount);
        }
        
        return deletedCount;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public int cleanupOldTokensForUser(User user, int maxTokensPerUser) {
        List<RefreshToken> activeTokens = getActiveRefreshTokens(user);
        
        if (activeTokens.size() <= maxTokensPerUser) {
            return 0; // Không vượt quá giới hạn
        }
        
        // Sắp xếp theo thời gian tạo, lấy tokens cũ nhất để xóa
        List<RefreshToken> tokensToDelete = activeTokens.stream()
                .sorted((t1, t2) -> t1.getCreatedAt().compareTo(t2.getCreatedAt()))
                .limit(activeTokens.size() - maxTokensPerUser)
                .toList();
        
        refreshTokenRepository.deleteAll(tokensToDelete);
        
        log.info("Đã cleanup {} old tokens cho user: {} (giữ lại {} tokens)", 
                tokensToDelete.size(), user.getEmail(), maxTokensPerUser);
        
        return tokensToDelete.size();
    }
}
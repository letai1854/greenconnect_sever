package com.greenconnect.greenconnect_api.services;

import java.time.LocalDateTime;
import java.util.List;

import com.greenconnect.greenconnect_api.entities.RefreshToken;
import com.greenconnect.greenconnect_api.entities.User;

/**
 * Service interface for RefreshToken management operations.
 * <p>Quản lý multiple refresh tokens cho multi-device support.</p>
 */
public interface RefreshTokenService {
    
    /**
     * Tạo refresh token mới cho user và device.
     * <p>Tự động cleanup tokens cũ nếu vượt quá giới hạn cho phép.</p>
     *
     * @param user User sở hữu token
     * @param tokenHash JWT refresh token string
     * @param deviceInfo Thông tin thiết bị
     * @param expiryDate Thời gian hết hạn
     * @return RefreshToken đã được lưu
     */
    RefreshToken createRefreshToken(User user, String tokenHash, String deviceInfo, LocalDateTime expiryDate);
    
    /**
     * Xác thực refresh token.
     * <p>Kiểm tra token có tồn tại và chưa hết hạn.</p>
     *
     * @param user User sở hữu token
     * @param tokenHash JWT refresh token string
     * @return RefreshToken nếu hợp lệ
     * @throws BusinessException nếu token không hợp lệ hoặc hết hạn
     */
    RefreshToken validateRefreshToken(User user, String tokenHash);
    
    /**
     * Xóa refresh token cụ thể (logout từ một thiết bị).
     *
     * @param user User sở hữu token
     * @param tokenHash JWT refresh token string
     * @return true nếu xóa thành công
     */
    boolean revokeRefreshToken(User user, String tokenHash);
    
    /**
     * Xóa tất cả refresh token của user (logout từ tất cả thiết bị).
     *
     * @param user User cần logout
     * @return số lượng tokens đã xóa
     */
    int revokeAllRefreshTokens(User user);
    
    /**
     * Lấy danh sách active refresh tokens của user.
     *
     * @param user User cần kiểm tra
     * @return List của RefreshToken còn hạn
     */
    List<RefreshToken> getActiveRefreshTokens(User user);
    
    /**
     * Cleanup tokens hết hạn của tất cả users.
     * <p>Chạy định kỳ để dọn dẹp database.</p>
     *
     * @return số lượng tokens đã xóa
     */
    int cleanupExpiredTokens();
    
    /**
     * Cleanup tokens cũ của user khi vượt quá giới hạn cho phép.
     * <p>Giữ lại các tokens mới nhất.</p>
     *
     * @param user User cần cleanup
     * @param maxTokensPerUser Số lượng tokens tối đa cho phép mỗi user
     * @return số lượng tokens đã xóa
     */
    int cleanupOldTokensForUser(User user, int maxTokensPerUser);
}
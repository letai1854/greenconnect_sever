package com.greenconnect.greenconnect_api.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Utility class for password hashing and verification.
 * <p>Sử dụng BCrypt algorithm với salt tự động để bảo mật cao.
 * BCrypt là một trong những thuật toán hash password được khuyến nghị nhất hiện tại.</p>
 */
public final class PasswordUtils {
    
    // Sử dụng BCrypt với strength level 12 (mạnh mẽ nhưng không quá chậm)
    private static final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);
    
    // Private constructor để ngăn khởi tạo instance
    private PasswordUtils() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }
    
    /**
     * Mã hóa mật khẩu thô thành hash string.
     * <p>Mỗi lần gọi sẽ tạo ra một hash khác nhau (do salt tự động)
     * nhưng tất cả đều có thể verify được với cùng một mật khẩu gốc.</p>
     *
     * @param rawPassword Mật khẩu thô cần mã hóa
     * @return Chuỗi hash đã mã hóa, an toàn để lưu vào database
     * @throws IllegalArgumentException nếu rawPassword null hoặc rỗng
     */
    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu không được null hoặc rỗng");
        }
        return passwordEncoder.encode(rawPassword);
    }
    
    /**
     * Kiểm tra mật khẩu thô có khớp với hash đã lưu không.
     * <p>Được sử dụng trong quá trình đăng nhập để xác thực người dùng.</p>
     *
     * @param rawPassword Mật khẩu thô từ user nhập vào
     * @param hashedPassword Hash password đã lưu trong database
     * @return {@code true} nếu khớp, {@code false} nếu không khớp
     */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return passwordEncoder.matches(rawPassword, hashedPassword);
    }
    
    /**
     * Lấy instance của PasswordEncoder để sử dụng trong Spring Security configuration.
     * <p>Đảm bảo consistency trong toàn bộ application.</p>
     *
     * @return PasswordEncoder instance được sử dụng trong class này
     */
    public static PasswordEncoder getPasswordEncoder() {
        return passwordEncoder;
    }
}
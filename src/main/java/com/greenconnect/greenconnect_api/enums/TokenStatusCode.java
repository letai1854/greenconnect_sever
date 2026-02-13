package com.greenconnect.greenconnect_api.enums;

/**
 * HTTP Status codes cho các trường hợp token/authentication issues.
 * <p>Định nghĩa các status codes thống nhất để client xử lý token problems.</p>
 * 
 * <pre>
 * 400 - Bad Request: Validation lỗi, missing fields
 * 401 - Unauthorized: Token invalid/malformed
 * 403 - Forbidden: User không có permission
 * 409 - Conflict: Access token hết hạn, cần refresh
 * 410 - Gone: Refresh token hết hạn, cần login lại
 * 411 - Length Required: (Không dùng) - dùng 410 cho refresh token expiry
 * 422 - Unprocessable Entity: User bị ban
 * 429 - Too Many Requests: Rate limit (login attempts)
 * 500 - Internal Server Error
 * </pre>
 */
public final class TokenStatusCode {
    
    // ========== 400 - BAD REQUEST ==========
    
    /**
     * HTTP 400: Validation error (missing email, password, etc).
     */
    public static final int BAD_REQUEST = 400;
    
    /**
     * HTTP 400: Email không hợp lệ.
     */
    public static final int INVALID_EMAIL = 400;
    
    /**
     * HTTP 400: Password quá ngắn.
     */
    public static final int INVALID_PASSWORD = 400;
    
    // ========== 401 - UNAUTHORIZED ==========
    
    /**
     * HTTP 401: Token không được cung cấp.
     */
    public static final int MISSING_TOKEN = 401;
    
    /**
     * HTTP 401: Token format không hợp lệ (malformed).
     */
    public static final int MALFORMED_TOKEN = 401;
    
    /**
     * HTTP 401: Token invalid (signature không khớp, corrupt).
     */
    public static final int INVALID_TOKEN = 401;
    
    /**
     * HTTP 401: Email/password không đúng.
     */
    public static final int INVALID_CREDENTIALS = 401;
    
    // ========== 403 - FORBIDDEN ==========
    
    /**
     * HTTP 403: User không có quyền truy cập resource.
     */
    public static final int FORBIDDEN = 403;
    
    /**
     * HTTP 403: User không có role yêu cầu.
     */
    public static final int INSUFFICIENT_PERMISSIONS = 403;
    
    // ========== 409 - CONFLICT ==========
    
    /**
     * HTTP 409: Access token đã hết hạn.
     * <p>Client nên gửi refresh token để lấy access token mới.</p>
     */
    public static final int ACCESS_TOKEN_EXPIRED = 409;
    
    /**
     * HTTP 409: Device không tồn tại (khi quản lý multi-device).
     */
    public static final int DEVICE_NOT_FOUND = 409;
    
    // ========== 410 - GONE ==========
    
    /**
     * HTTP 410: Refresh token đã hết hạn.
     * <p>Client phải login lại để lấy refresh token mới.</p>
     */
    public static final int REFRESH_TOKEN_EXPIRED = 410;
    
    /**
     * HTTP 410: User không tồn tại (possibly deleted).
     */
    public static final int USER_NOT_FOUND = 410;
    
    // ========== 422 - UNPROCESSABLE ENTITY ==========
    
    /**
     * HTTP 422: User bị ban (not allowed to login).
     */
    public static final int USER_BANNED = 422;
    
    /**
     * HTTP 422: User chưa được active (verify email).
     */
    public static final int USER_NOT_ACTIVE = 422;
    
    /**
     * HTTP 422: Email đã được sử dụng (user already exists).
     */
    public static final int USER_ALREADY_EXISTS = 422;
    
    /**
     * HTTP 422: Validation error.
     */
    public static final int VALIDATION_ERROR = 422;
    
    // ========== 429 - TOO MANY REQUESTS ==========
    
    /**
     * HTTP 429: Quá nhiều login attempts (rate limit).
     */
    public static final int TOO_MANY_LOGIN_ATTEMPTS = 429;
    
    /**
     * HTTP 429: Quá nhiều OTP attempts (rate limit).
     */
    public static final int TOO_MANY_OTP_ATTEMPTS = 429;
    
    // ========== 500 - INTERNAL SERVER ERROR ==========
    
    /**
     * HTTP 500: Lỗi server (token generation failed, etc).
     */
    public static final int INTERNAL_SERVER_ERROR = 500;
    
    /**
     * HTTP 500: Database error.
     */
    public static final int DATABASE_ERROR = 500;
    
    /**
     * HTTP 500: JWT configuration error.
     */
    public static final int JWT_ERROR = 500;
}

package com.greenconnect.greenconnect_api.exceptions;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // Success codes (1000-1999)
    SUCCESS(1000, "Success", HttpStatus.OK),
    
    // Client errors (4000-4999)
    BAD_REQUEST(4000, "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(4001, "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(4003, "Forbidden", HttpStatus.FORBIDDEN),
    NOT_FOUND(4004, "Resource not found", HttpStatus.NOT_FOUND),
    
    // Validation errors (4100-4199)
    VALIDATION_ERROR(4100, "Validation failed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(4101, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    EMAIL_INVALID(4102, "Email format is invalid", HttpStatus.BAD_REQUEST),
    
    // Business logic errors (4200-4299)
    USER_ALREADY_EXISTS(4201, "Tài khoản đã tồn tại", HttpStatus.CONFLICT),
    USER_NOT_FOUND(4202, "User not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(4203, "Invalid username or password", HttpStatus.UNAUTHORIZED),
   
    SUPPLIER_NAME_ALREADY_EXISTS(4204, "Tên nhà cung cấp đã tồn tại", HttpStatus.CONFLICT),
    SUPPLIER_EMAIL_ALREADY_EXISTS(4205, "Email nhà cung cấp đã tồn tại", HttpStatus.CONFLICT),
    
    // Invitation errors (4207-4209)
    EMAIL_ALREADY_EXISTS(4207, "Email đã được sử dụng", HttpStatus.CONFLICT),
    EXPIRED_TOKEN(4208, "Token kích hoạt đã hết hạn", HttpStatus.BAD_REQUEST),
    INVALID_STATUS(4209, "Trạng thái người dùng không hợp lệ", HttpStatus.BAD_REQUEST),
    
    // Category errors (4210-4219)
    CATEGORY_ALREADY_EXISTS(4210, "Tên danh mục đã tồn tại", HttpStatus.CONFLICT),
    CATEGORY_NOT_FOUND(4211, "Danh mục không tồn tại", HttpStatus.NOT_FOUND),
    SUPPLIER_NOT_FOUND(4206, "Nhà cung cấp không tồn tại", HttpStatus.NOT_FOUND),

    // Product errors (4320-4339)
    PRODUCT_NAME_ALREADY_EXISTS(4320, "Tên sản phẩm đã tồn tại", HttpStatus.CONFLICT),
    PRODUCT_SLUG_ALREADY_EXISTS(4321, "Slug sản phẩm đã tồn tại", HttpStatus.CONFLICT),
    PRODUCT_SKU_ALREADY_EXISTS(4322, "SKU sản phẩm đã tồn tại", HttpStatus.CONFLICT),
    PRODUCT_NOT_FOUND(4323, "Sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    MULTIPLE_DEFAULT_VARIANTS(4324, "Chỉ được có 1 variant mặc định", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_NOT_FOUND(4325, "Biến thể sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_AVAILABLE(4326, "Sản phẩm không khả dụng", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_STOCK(4327, "Không đủ hàng trong kho", HttpStatus.BAD_REQUEST),

    // Cart errors (4340-4359)
    CART_ITEM_NOT_FOUND(4340, "Sản phẩm không có trong giỏ hàng", HttpStatus.NOT_FOUND),
    ACCESS_DENIED(4341, "Không có quyền truy cập", HttpStatus.FORBIDDEN),

    // Voucher errors (4360-4379)
    VOUCHER_CODE_ALREADY_EXISTS(4360, "Mã voucher đã tồn tại", HttpStatus.CONFLICT),
    VOUCHER_NOT_FOUND(4361, "Không tìm thấy voucher", HttpStatus.NOT_FOUND),
    VOUCHER_INVALID_DATE_RANGE(4362, "Ngày bắt đầu phải trước ngày kết thúc", HttpStatus.BAD_REQUEST),
    VOUCHER_EMPTY_CODE(4363, "Mã voucher không được để trống", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_AVAILABLE(4364, "Voucher không khả dụng hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED(4365, "Voucher đã hết hạn", HttpStatus.BAD_REQUEST),
    VOUCHER_INACTIVE(4366, "Voucher không hoạt động", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_EXCEEDED(4367, "Voucher đã hết lượt sử dụng", HttpStatus.BAD_REQUEST),
    VOUCHER_USER_LIMIT_EXCEEDED(4368, "Bạn đã sử dụng hết lượt voucher này", HttpStatus.BAD_REQUEST),
    VOUCHER_MIN_ORDER_VALUE_NOT_MET(4369, "Giá trị đơn hàng chưa đạt yêu cầu tối thiểu để sử dụng voucher", HttpStatus.BAD_REQUEST),
    VOUCHER_ALREADY_USED_IN_ORDER(4370, "Voucher này đã được sử dụng trong đơn hàng", HttpStatus.BAD_REQUEST),

    // Order errors (4380-4399)
    ORDER_NOT_FOUND(4380, "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(4381, "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_ORDER_STATUS(4382, "Trạng thái đơn hàng không hợp lệ", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_LOYALTY_POINTS(4383, "Số điểm loyalty không đủ để sử dụng", HttpStatus.BAD_REQUEST),
    REFUND_REQUEST_ALREADY_EXISTS(4384, "Bạn đã có yêu cầu hoàn tiền đang được xử lý cho đơn hàng này", HttpStatus.CONFLICT),
    PRODUCT_NOT_SELLING(4385, "Sản phẩm không còn bán", HttpStatus.BAD_REQUEST),

    // Favorite errors (4400-4419)
    FAVORITE_ALREADY_EXISTS(4400, "Sản phẩm đã có trong danh sách yêu thích", HttpStatus.CONFLICT),
    FAVORITE_NOT_FOUND(4401, "Sản phẩm không có trong danh sách yêu thích", HttpStatus.NOT_FOUND),
    FAVORITE_LIMIT_EXCEEDED(4402, "Đã đạt giới hạn 20 sản phẩm yêu thích", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_ACTIVE(4403, "Sản phẩm không hoạt động", HttpStatus.BAD_REQUEST),
    
    // Footer errors (4410-4419)
    FOOTER_SECTION_NOT_FOUND(4410, "Không tìm thấy nhóm footer với ID này", HttpStatus.NOT_FOUND),
    FOOTER_LINK_NOT_FOUND(4411, "Không tìm thấy link footer với ID này", HttpStatus.NOT_FOUND),

    // Token & Auth errors (4420-4439)
    UNAUTHENTICATED(4420, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(4421, "Token không hợp lệ", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(4422, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_EXPIRED(4423, "Access token đã hết hạn - vui lòng refresh", HttpStatus.CONFLICT),
    REFRESH_TOKEN_EXPIRED(4424, "Refresh token đã hết hạn - vui lòng login lại", HttpStatus.GONE),
    REFRESH_TOKEN_INVALID(4425, "Refresh token không hợp lệ", HttpStatus.GONE),
    USER_BANNED(4426, "Tài khoản bị ban - không thể đăng nhập", HttpStatus.FORBIDDEN),
    USER_INACTIVE(4427, "Tài khoản chưa được kích hoạt", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED(4428, "Tài khoản đã bị vô hiệu hóa - vui lòng liên hệ admin", HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(4429, "Tài khoản đã bị cấm - không thể đăng nhập", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_ACTIVATED(4430, "Tài khoản chưa được kích hoạt", HttpStatus.FORBIDDEN),
    ACCOUNT_ALREADY_ACTIVATED(4431, "Tài khoản đã được kích hoạt trước đó", HttpStatus.BAD_REQUEST),
    
    // 2FA errors (4890-4899)
    INVALID_OTP(4890, "Mã OTP không hợp lệ", HttpStatus.UNAUTHORIZED),
    TWO_FACTOR_REQUIRED(4891, "Yêu cầu bật 2FA cho tài khoản staff", HttpStatus.FORBIDDEN),
    
    // Review errors (4440-4459)
    REVIEW_ALREADY_REPLIED(4440, "Đánh giá này đã được shop phản hồi rồi", HttpStatus.BAD_REQUEST),
    REVIEW_NOT_FOUND(4441, "Không tìm thấy đánh giá", HttpStatus.NOT_FOUND),
    
    // External API errors (4460-4479)
    EXTERNAL_API_ERROR(4460, "Lỗi khi gọi API bên ngoài", HttpStatus.BAD_GATEWAY),
    
    // Chat errors (4480-4499)
    CHAT_SESSION_NOT_FOUND(4480, "Không tìm thấy phiên chat", HttpStatus.NOT_FOUND),
    
    INTERNAL_SERVER_ERROR(5000, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATABASE_ERROR(5001, "Database connection error", HttpStatus.INTERNAL_SERVER_ERROR);



    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
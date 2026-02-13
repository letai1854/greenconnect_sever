package com.greenconnect.greenconnect_api.dtos.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.greenconnect.greenconnect_api.enums.PaymentMethod;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    
    // ========== VOUCHER INFORMATION ==========
    /**
     * Danh sách voucher ID được áp dụng (có thể dùng nhiều voucher)
     */
    private List<UUID> voucherIds; // Optional - List of voucher IDs
    
    // ========== DELIVERY INFORMATION ==========
    @NotBlank(message = "Tên người nhận không được để trống")
    @Size(max = 255, message = "Tên người nhận không được vượt quá 255 ký tự")
    private String recipientName;
    
    @NotBlank(message = "Số điện thoại người nhận không được để trống")
    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Số điện thoại không hợp lệ")
    private String recipientPhone;
    
    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    @Size(max = 1000, message = "Địa chỉ giao hàng không được vượt quá 1000 ký tự")
    private String deliveryAddress;


    @Size(max = 1000, message = "Địa chỉ giao hàng không được vượt quá 1000 ký tự")
    private String customerNote;
    
    // @NotBlank(message = "Phương thức giao hàng không được để trống")
    // @Size(max = 255, message = "Tên phương thức giao hàng không được vượt quá 255 ký tự")
    // private String deliveryMethodName;
    
    @NotNull(message = "Ngày giao hàng không được để trống")
    private LocalDate deliveryDate;
    
    // ========== ORDER DETAILS ==========
    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<OrderItemRequest> orderItems;
    
    // ========== FINANCIAL INFORMATION (Frontend calculated) ==========
    @NotNull(message = "Tổng tiền sản phẩm không được để trống")
    @Digits(integer = 13, fraction = 2, message = "Tổng tiền sản phẩm không hợp lệ")
    private BigDecimal totalProductMoney;
    
    @NotNull(message = "Phí vận chuyển không được để trống")
    @DecimalMin(value = "0.0", message = "Phí vận chuyển không được âm")
    @Digits(integer = 13, fraction = 2, message = "Phí vận chuyển không hợp lệ")
    private BigDecimal shippingFee;
    
    @NotNull(message = "Số tiền giảm giá không được để trống")
    @DecimalMin(value = "0.0", message = "Số tiền giảm giá không được âm")
    @Digits(integer = 13, fraction = 2, message = "Số tiền giảm giá không hợp lệ")
    private BigDecimal discountAmount;
    
    @DecimalMin(value = "0.0", message = "Tiền thuế không được âm")
    @Digits(integer = 13, fraction = 2, message = "Tiền thuế không hợp lệ")
    private BigDecimal tax;
    
    @NotNull(message = "Tổng thanh toán không được để trống")
    @DecimalMin(value = "0.0", inclusive = true, message = "Tổng thanh toán không được âm")
    @Digits(integer = 13, fraction = 2, message = "Tổng thanh toán không hợp lệ")
    private BigDecimal totalPayment;
    
    // ========== PAYMENT INFORMATION ==========
    @NotNull(message = "Phương thức thanh toán không được để trống")
    private PaymentMethod paymentMethod;
    
 
    
    // ========== LOYALTY POINTS ==========
    @DecimalMin(value = "0.0", message = "Điểm thưởng không được âm")
    @Digits(integer = 8, fraction = 2, message = "Điểm thưởng không hợp lệ")
    private BigDecimal loyaltyPoints;
    
    // ========== RANK POINTS ==========
    @DecimalMin(value = "0.0", message = "Điểm rank không được âm")
    @Digits(integer = 8, fraction = 2, message = "Điểm rank không hợp lệ")
    private BigDecimal rankPoints;
}
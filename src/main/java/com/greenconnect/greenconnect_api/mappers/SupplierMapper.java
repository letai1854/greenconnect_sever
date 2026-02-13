package com.greenconnect.greenconnect_api.mappers;

import java.util.List;
import java.util.stream.Collectors;

import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierRequest;
import com.greenconnect.greenconnect_api.dtos.request.Suppliers.SupplierUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.SupplierResponse;
import com.greenconnect.greenconnect_api.entities.Supplier;

/**
 * Mapper utility class for Supplier entity conversions.
 * <p>Chuyển đổi giữa các layers: Request DTOs → Entities → Response DTOs</p>
 * <p>Sử dụng static methods để dễ dàng import và sử dụng trong Services.</p>
 */
public final class SupplierMapper {
    
    // Private constructor để ngăn khởi tạo instance
    private SupplierMapper() {
        throw new UnsupportedOperationException("Utility class - không thể khởi tạo");
    }
    
    /**
     * Chuyển đổi SupplierRequest thành Supplier entity.
     * <p>Áp dụng các business rules:</p>
     * <ul>
     *   <li>Set trạng thái mặc định isActive = true nếu không có trong request</li>
     *   <li>Validate dữ liệu đầu vào</li>
     * </ul>
     *
     * @param request SupplierRequest từ client
     * @return Supplier entity để lưu vào database
     * @throws IllegalArgumentException nếu request null hoặc thiếu thông tin bắt buộc
     */
    public static Supplier toEntity(SupplierRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("SupplierRequest không được null");
        }
        
        return Supplier.builder()
                .name(request.getName())
                .description(request.getDescription())
                .logoUrl(request.getLogoUrl())
                .address(request.getAddress())
                .email(request.getEmail())
                .phoneNumber(request.getPhoneNumber())
                .isActive(Boolean.TRUE.equals(request.getIsActive()))
                .build();
    }
    
    /**
     * Chuyển đổi Supplier entity thành SupplierResponse.
     * <p>Bao gồm tất cả thông tin cần thiết cho client mà không có thông tin nhạy cảm.</p>
     * <p>Thêm timestamp để client biết thời gian tạo/cập nhật.</p>
     *
     * @param supplier Supplier entity từ database
     * @return SupplierResponse để trả về client
     * @throws IllegalArgumentException nếu supplier null
     */
    public static SupplierResponse toResponse(Supplier supplier) {
        if (supplier == null) {
            throw new IllegalArgumentException("Supplier entity không được null");
        }
        
        return SupplierResponse.builder()
                .id(supplier.getId())
                .name(supplier.getName())
                .description(supplier.getDescription())
                .logoUrl(supplier.getLogoUrl())
                .address(supplier.getAddress())
                .email(supplier.getEmail())
                .phoneNumber(supplier.getPhoneNumber())
                .isActive(supplier.getIsActive())
                .createdAt(supplier.getCreatedAt())
                .updatedAt(supplier.getUpdatedAt())
                .build();
    }
    
    /**
     * Cập nhật Supplier entity từ SupplierRequest.
     * <p>Chỉ cập nhật các field không null từ request, giữ nguyên các field khác.</p>
     * <p>Phương thức này hữu ích cho UPDATE operations.</p>
     *
     * @param existingSupplier Supplier entity hiện tại từ database
     * @param request SupplierRequest chứa dữ liệu cập nhật
     * @return Supplier entity đã được cập nhật
     * @throws IllegalArgumentException nếu existingSupplier hoặc request null
     */
    public static Supplier updateEntity(Supplier existingSupplier, SupplierUpdateRequest request) {
        if (existingSupplier == null) {
            throw new IllegalArgumentException("Supplier entity hiện tại không được null");
        }
        if (request == null) {
            throw new IllegalArgumentException("SupplierRequest không được null");
        }
        
        // Cập nhật các field từ request (chỉ cập nhật nếu không null)
        if (request.getName() != null) {
            existingSupplier.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingSupplier.setDescription(request.getDescription());
        }
        if (request.getLogoUrl() != null) {
            existingSupplier.setLogoUrl(request.getLogoUrl());
        }
        if (request.getAddress() != null) {
            existingSupplier.setAddress(request.getAddress());
        }
        if (request.getEmail() != null) {
            existingSupplier.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            existingSupplier.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getIsActive() != null) {
            existingSupplier.setIsActive(request.getIsActive());
        }
        
        return existingSupplier;
    }
    public static List<SupplierResponse> toResponseList(List<Supplier> suppliers) {
        if (suppliers == null) {
            throw new IllegalArgumentException("Danh sách Supplier entities không được null");
        }
        
        return suppliers.stream()
                .map(SupplierMapper::toResponse)
                .collect(Collectors.toList());
    }
}
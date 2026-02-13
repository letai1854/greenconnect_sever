package com.greenconnect.greenconnect_api.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.VoucherCreateRequest;
import com.greenconnect.greenconnect_api.dtos.request.VoucherUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.response.VoucherResponse;
import com.greenconnect.greenconnect_api.dtos.response.UserVoucherResponse;
import com.greenconnect.greenconnect_api.entities.Voucher;
import com.greenconnect.greenconnect_api.entities.VoucherUsage;
import com.greenconnect.greenconnect_api.enums.VoucherStatus;
import com.greenconnect.greenconnect_api.exceptions.AppException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.VoucherRepository;
import com.greenconnect.greenconnect_api.repositories.VoucherUsageRepository;
import com.greenconnect.greenconnect_api.services.VoucherService;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    public VoucherResponse createVoucher(VoucherCreateRequest request) {
        log.info("Tạo voucher mới với code: {}", request.getVoucherCode());
        
        try {
            // Validate business rules
            validateVoucherRequest(request.getVoucherCode(), request.getStartDate(), request.getEndDate());
            
            // Check if voucher code already exists
            if (voucherRepository.findAll((Specification<Voucher>) (root, query, cb) -> 
                    cb.equal(root.get("voucherCode"), request.getVoucherCode())).size() > 0) {
                throw new AppException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, "Mã voucher đã tồn tại: " + request.getVoucherCode());
            }
        } catch (Exception e) {
            log.error("Error during voucher validation: {}", e.getMessage(), e);
            throw e;
        }
        
        try {
            Voucher voucher = Voucher.builder()
                    .voucherCode(request.getVoucherCode())
                    .description(request.getDescription())
                    .voucherType(request.getVoucherType())
                    .discountValue(request.getDiscountValue())
                    .maxDiscountAmount(request.getMaxDiscountAmount())
                    .minOrderValue(request.getMinOrderValue())
                    .maxUsageCount(request.getMaxUsageCount())
                    .usedCount(0)
                    .startDate(request.getStartDate())
                    .endDate(request.getEndDate())
                    .isActive(request.getIsActive())
                    .usageLimitPerUser(request.getUsageLimitPerUser())
                    .build();
            
            log.info("Saving voucher to database...");
            Voucher savedVoucher = voucherRepository.save(voucher);
            log.info("Tạo voucher thành công với ID: {}", savedVoucher.getId());
            
            return mapToVoucherResponse(savedVoucher);
        } catch (Exception e) {
            log.error("Error saving voucher: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi lưu voucher: " + e.getMessage());
        }
    }

    @Override
    public VoucherResponse updateVoucher(UUID voucherId, VoucherUpdateRequest request) {
        log.info("Cập nhật voucher với ID: {}", voucherId);
        
        Voucher existingVoucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher với ID: " + voucherId));
        
        // Validate business rules only for non-null fields
        if (request.getVoucherCode() != null && request.getStartDate() != null && request.getEndDate() != null) {
            validateVoucherRequest(request.getVoucherCode(), request.getStartDate(), request.getEndDate());
        } else if (request.getStartDate() != null && request.getEndDate() != null) {
            validateVoucherRequest(existingVoucher.getVoucherCode(), request.getStartDate(), request.getEndDate());
        } else if (request.getVoucherCode() != null) {
            validateVoucherRequest(request.getVoucherCode(), existingVoucher.getStartDate(), existingVoucher.getEndDate());
        }
        
        // Check if voucher code already exists for other vouchers (only if voucherCode is being updated)
        if (request.getVoucherCode() != null && !request.getVoucherCode().equals(existingVoucher.getVoucherCode())) {
            voucherRepository.findAll((Specification<Voucher>) (root, query, cb) -> {
                Predicate codeEquals = cb.equal(root.get("voucherCode"), request.getVoucherCode());
                Predicate notSameId = cb.notEqual(root.get("id"), voucherId);
                return cb.and(codeEquals, notSameId);
            }).stream().findFirst().ifPresent(v -> {
                throw new AppException(ErrorCode.VOUCHER_CODE_ALREADY_EXISTS, "Mã voucher đã tồn tại: " + request.getVoucherCode());
            });
        }
        
        // Update only non-null fields
        if (request.getVoucherCode() != null) {
            existingVoucher.setVoucherCode(request.getVoucherCode());
        }
        if (request.getDescription() != null) {
            existingVoucher.setDescription(request.getDescription());
        }
        if (request.getVoucherType() != null) {
            existingVoucher.setVoucherType(request.getVoucherType());
        }
        if (request.getDiscountValue() != null) {
            existingVoucher.setDiscountValue(request.getDiscountValue());
        }
        if (request.getMaxDiscountAmount() != null) {
            existingVoucher.setMaxDiscountAmount(request.getMaxDiscountAmount());
        }
        if (request.getMinOrderValue() != null) {
            existingVoucher.setMinOrderValue(request.getMinOrderValue());
        }
        if (request.getMaxUsageCount() != null) {
            existingVoucher.setMaxUsageCount(request.getMaxUsageCount());
        }
        if (request.getStartDate() != null) {
            existingVoucher.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            existingVoucher.setEndDate(request.getEndDate());
        }
        if (request.getIsActive() != null) {
            existingVoucher.setIsActive(request.getIsActive());
        }
        if (request.getUsageLimitPerUser() != null) {
            existingVoucher.setUsageLimitPerUser(request.getUsageLimitPerUser());
        }
        
        Voucher updatedVoucher = voucherRepository.save(existingVoucher);
        log.info("Cập nhật voucher thành công với ID: {}", updatedVoucher.getId());
        
        return mapToVoucherResponse(updatedVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse getVoucherById(UUID voucherId) {
        log.info("Lấy thông tin voucher với ID: {}", voucherId);
        
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher với ID: " + voucherId));
        
        return mapToVoucherResponse(voucher);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchers(String search, Boolean isActive, Pageable pageable) {
        log.info("Lấy danh sách voucher với search: '{}', isActive: {}", search, isActive);
        
        Specification<Voucher> spec = (root, query, cb) -> {
            Predicate predicate = cb.conjunction();
            
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                    cb.like(cb.lower(root.get("voucherCode")), searchPattern),
                    cb.like(cb.lower(root.get("description")), searchPattern)
                );
                predicate = cb.and(predicate, searchPredicate);
            }
            
            if (isActive != null) {
                predicate = cb.and(predicate, cb.equal(root.get("isActive"), isActive));
            }
            
            return predicate;
        };
        
        return voucherRepository.findAll(spec, pageable)
                .map(this::mapToVoucherResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAvailableVouchers(Pageable pageable) {
        log.info("Lấy danh sách voucher có thể sử dụng");
        
        LocalDateTime now = LocalDateTime.now();
        
        Specification<Voucher> spec = (root, query, cb) -> {
            Predicate isActive = cb.equal(root.get("isActive"), true);
            Predicate notExpired = cb.greaterThan(root.get("endDate"), now);
            Predicate hasUsageLeft = cb.lessThan(root.get("usedCount"), root.get("maxUsageCount"));
            
            return cb.and(isActive, notExpired, hasUsageLeft);
        };
        
        return voucherRepository.findAll(spec, pageable)
                .map(this::mapToVoucherResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAllActiveVouchers() {
        log.info("Lấy tất cả voucher đang active");
        
        Specification<Voucher> spec = (root, query, cb) -> {
            return cb.equal(root.get("isActive"), true);
        };
        
        return voucherRepository.findAll(spec)
                .stream()
                .map(this::mapToVoucherResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherResponse findByVoucherCode(String voucherCode) {
        log.info("Tìm voucher theo code: {}", voucherCode);
        
        Voucher voucher = voucherRepository.findAll((Specification<Voucher>) (root, query, cb) -> 
                cb.equal(root.get("voucherCode"), voucherCode))
                .stream()
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher với code: " + voucherCode));
        
        return mapToVoucherResponse(voucher);
    }

    @Override
    public VoucherResponse toggleVoucherStatus(UUID voucherId) {
        log.info("Thay đổi trạng thái voucher với ID: {}", voucherId);
        
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher với ID: " + voucherId));
        
        voucher.setIsActive(!voucher.getIsActive());
        Voucher updatedVoucher = voucherRepository.save(voucher);
        
        log.info("Thay đổi trạng thái voucher thành công: {} -> {}", 
                !updatedVoucher.getIsActive(), updatedVoucher.getIsActive());
        
        return mapToVoucherResponse(updatedVoucher);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserVoucherResponse> getUserVouchers(UUID userId, Pageable pageable) {
        log.info("Lấy danh sách voucher cho user: {}", userId);
        
        LocalDateTime now = LocalDateTime.now();
        
        // Lấy tất cả voucher active, sau đó filter và map trong service
        // Điều này đảm bảo performance tốt hơn với các index đã tạo
        Specification<Voucher> spec = (root, query, cb) -> {
            return cb.equal(root.get("isActive"), true);
        };
        
        return voucherRepository.findAll(spec, pageable)
                .map(voucher -> mapToUserVoucherResponse(voucher, userId, now));
    }

    @Override
    public void deleteVoucher(UUID voucherId) {
        log.info("Xóa voucher với ID: {}", voucherId);
        
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND, "Không tìm thấy voucher với ID: " + voucherId));
        
        voucher.setIsActive(false);
        voucherRepository.save(voucher);
        
        log.info("Xóa voucher thành công với ID: {}", voucherId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getActiveVouchersPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách voucher active với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Voucher> spec = (root, query, cb) -> {
            return cb.equal(root.get("isActive"), true);
        };
        
        return voucherRepository.findAll(spec, pageable)
                .map(this::mapToVoucherResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getInactiveVouchersPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách voucher inactive với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Voucher> spec = (root, query, cb) -> {
            return cb.equal(root.get("isActive"), false);
        };
        
        return voucherRepository.findAll(spec, pageable)
                .map(this::mapToVoucherResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> getAllVouchersPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy tất cả voucher với phân trang - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        return voucherRepository.findAll(pageable)
                .map(this::mapToVoucherResponse);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<VoucherResponse> searchVouchers(String keyword, String tab, int page, int size) {
        log.info("🔍 Tìm kiếm voucher - Từ khóa: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Voucher> spec = (root, query, cb) -> {
            Predicate searchPredicate = cb.or(
                cb.like(cb.lower(root.get("voucherCode")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
            );
            
            Predicate finalPredicate;
            
            switch (tab.toLowerCase()) {
                case "active":
                    finalPredicate = cb.and(searchPredicate, cb.equal(root.get("isActive"), true));
                    log.info("✅ Tìm kiếm trong voucher ACTIVE");
                    break;
                case "inactive":
                    finalPredicate = cb.and(searchPredicate, cb.equal(root.get("isActive"), false));
                    log.info("✅ Tìm kiếm trong voucher INACTIVE");
                    break;
                case "all":
                    finalPredicate = searchPredicate;
                    log.info("✅ Tìm kiếm trong TẤT CẢ voucher");
                    break;
                default:
                    log.warn("⚠️ Tab không hợp lệ: '{}' - Sử dụng 'all' mặc định", tab);
                    finalPredicate = searchPredicate;
                    break;
            }
            
            return finalPredicate;
        };
        
        Page<VoucherResponse> result = voucherRepository.findAll(spec, pageable)
                .map(this::mapToVoucherResponse);
        
        log.info("✅ Tìm thấy {} kết quả", result.getTotalElements());
        
        return result;
    }

    private void validateVoucherRequest(String voucherCode, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new AppException(ErrorCode.VOUCHER_INVALID_DATE_RANGE);
        }
        
        if (voucherCode != null && voucherCode.trim().isEmpty()) {
            throw new AppException(ErrorCode.VOUCHER_EMPTY_CODE);
        }
    }

    private VoucherResponse mapToVoucherResponse(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        boolean isExpired = voucher.getEndDate().isBefore(now);
        boolean isAvailable = voucher.getIsActive() && 
                             !isExpired && 
                             voucher.getStartDate().isBefore(now) &&
                             voucher.getUsedCount() < voucher.getMaxUsageCount();
        
        return VoucherResponse.builder()
                .id(voucher.getId())
                .voucherCode(voucher.getVoucherCode())
                .description(voucher.getDescription())
                .voucherType(voucher.getVoucherType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .maxUsageCount(voucher.getMaxUsageCount())
                .usedCount(voucher.getUsedCount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .isActive(voucher.getIsActive())
                .usageLimitPerUser(voucher.getUsageLimitPerUser())
                .createdDate(voucher.getCreatedDate())
                .isExpired(isExpired)
                .isAvailable(isAvailable)
                .remainingUsage(voucher.getMaxUsageCount() - voucher.getUsedCount())
                .build();
    }

    private UserVoucherResponse mapToUserVoucherResponse(Voucher voucher, UUID userId, LocalDateTime now) {
        // Lấy thông tin sử dụng voucher của user
        List<VoucherUsage> userUsages = voucherUsageRepository.findByVoucherIdAndUserId(voucher.getId(), userId);
        int userUsedCount = userUsages.size();
        LocalDateTime lastUsedTime = userUsages.stream()
                .map(VoucherUsage::getUsageTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        // Xác định trạng thái voucher
        VoucherStatus status = determineVoucherStatus(voucher, userId, userUsedCount, now);
        
        // Xác định xem user có thể sử dụng voucher không
        boolean canUse = canUserUseVoucher(voucher, userId, userUsedCount, now);

        // Tính số ngày còn lại/tới hạn
        Long daysUntilExpiry = null;
        Long daysUntilStart = null;
        
        if (voucher.getEndDate().isAfter(now)) {
            daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), voucher.getEndDate().toLocalDate());
        }
        
        if (voucher.getStartDate().isAfter(now)) {
            daysUntilStart = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), voucher.getStartDate().toLocalDate());
        }

        return UserVoucherResponse.builder()
                .id(voucher.getId())
                .voucherCode(voucher.getVoucherCode())
                .description(voucher.getDescription())
                .voucherType(voucher.getVoucherType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .usageLimitPerUser(voucher.getUsageLimitPerUser())
                .createdDate(voucher.getCreatedDate())
                .status(status)
                .canUse(canUse)
                .statusMessage(getStatusMessage(status))
                .userUsedCount(userUsedCount)
                .lastUsedTime(lastUsedTime)
                .remainingUsage(voucher.getMaxUsageCount() - voucher.getUsedCount())
                .daysUntilExpiry(daysUntilExpiry)
                .daysUntilStart(daysUntilStart)
                .build();
    }

    private VoucherStatus determineVoucherStatus(Voucher voucher, UUID userId, int userUsedCount, LocalDateTime now) {
        // Không hoạt động
        if (!voucher.getIsActive()) {
            return VoucherStatus.INACTIVE;
        }
        
        // Hết lượt sử dụng chung
        if (voucher.getUsedCount() >= voucher.getMaxUsageCount()) {
            return VoucherStatus.OUT_OF_STOCK;
        }
        
        // Đã hết hạn
        if (voucher.getEndDate().isBefore(now)) {
            return VoucherStatus.EXPIRED;
        }
        
        // Chưa tới thời gian sử dụng
        if (voucher.getStartDate().isAfter(now)) {
            return VoucherStatus.UPCOMING;
        }
        
        // User đã sử dụng hết lượt cá nhân
        if (userUsedCount >= voucher.getUsageLimitPerUser()) {
            return VoucherStatus.USED;
        }
        
        // Có thể sử dụng
        return VoucherStatus.AVAILABLE;
    }

    private boolean canUserUseVoucher(Voucher voucher, UUID userId, int userUsedCount, LocalDateTime now) {
        return voucher.getIsActive() && 
               voucher.getStartDate().isBefore(now) &&
               voucher.getEndDate().isAfter(now) &&
               voucher.getUsedCount() < voucher.getMaxUsageCount() &&
               userUsedCount < voucher.getUsageLimitPerUser();
    }

    private String getStatusMessage(VoucherStatus status) {
        switch (status) {
            case AVAILABLE:
                return "Có thể sử dụng";
            case UPCOMING:
                return "Sắp có thể sử dụng";
            case EXPIRED:
                return "Đã hết hạn";
            case OUT_OF_STOCK:
                return "Đã hết lượt sử dụng";
            case USED:
                return "Đã sử dụng hết lượt";
            case INACTIVE:
                return "Không hoạt động";
            default:
                return "Không xác định";
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse> getActiveVouchersWithUserUsage(UUID userId, Pageable pageable) {
        log.info("Lấy danh sách voucher active với trạng thái sử dụng của user: {}", userId);
        
        // Lấy tất cả voucher active
        Page<Voucher> vouchersPage = voucherRepository.findAll(
            (Specification<Voucher>) (root, query, cb) -> cb.equal(root.get("isActive"), true),
            pageable
        );
        
        LocalDateTime now = LocalDateTime.now();
        
        // Map sang VoucherWithUsageResponse
        return vouchersPage.map(voucher -> {
            // Đếm số lần user đã sử dụng voucher này
            long userUsedCount = voucherUsageRepository.countByVoucherIdAndUserId(voucher.getId(), userId);
            
            // Kiểm tra user đã dùng hết lượt chưa
            boolean isFullyUsedByUser = userUsedCount >= voucher.getUsageLimitPerUser();
            
            // Kiểm tra voucher có còn available không (tổng hợp tất cả điều kiện)
            boolean isAvailable = voucher.getIsActive()
                && now.isAfter(voucher.getStartDate()) 
                && now.isBefore(voucher.getEndDate())
                && voucher.getUsedCount() < voucher.getMaxUsageCount()
                && !isFullyUsedByUser;
            
            return com.greenconnect.greenconnect_api.dtos.response.VoucherWithUsageResponse.builder()
                .id(voucher.getId())
                .voucherCode(voucher.getVoucherCode())
                .description(voucher.getDescription())
                .voucherType(voucher.getVoucherType())
                .discountValue(voucher.getDiscountValue())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .minOrderValue(voucher.getMinOrderValue())
                .maxUsageCount(voucher.getMaxUsageCount())
                .usedCount(voucher.getUsedCount())
                .startDate(voucher.getStartDate())
                .endDate(voucher.getEndDate())
                .isActive(voucher.getIsActive())
                .usageLimitPerUser(voucher.getUsageLimitPerUser())
                .userUsedCount((int) userUsedCount)
                .isFullyUsedByUser(isFullyUsedByUser)
                .isAvailable(isAvailable)
                .createdDate(voucher.getCreatedDate())
                .build();
        });
    }
}
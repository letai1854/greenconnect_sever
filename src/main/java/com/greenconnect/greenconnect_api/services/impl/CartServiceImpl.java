package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.AddToCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.BatchUpdateCartRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateCartItemRequest;
import com.greenconnect.greenconnect_api.dtos.response.BatchUpdateCartResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartItemResponse;
import com.greenconnect.greenconnect_api.dtos.response.CartResponse;
import com.greenconnect.greenconnect_api.entities.CartItem;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.User;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.repositories.CartItemRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.repositories.UserRepository;
import com.greenconnect.greenconnect_api.services.CartService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation của CartService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {
    
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional
    public CartItemResponse addToCart(UUID userId, AddToCartRequest request) {
        log.info("Thêm sản phẩm vào giỏ hàng - User: {}, Variant: {}, Quantity: {}", 
                userId, request.getProductVariantId(), request.getQuantity());
        
        // 1. Validate user tồn tại
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy user với ID: {}", userId);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });
        
        // 2. Validate product variant tồn tại và active
        ProductVariant productVariant = productVariantRepository.findById(request.getProductVariantId())
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy product variant với ID: {}", request.getProductVariantId());
                    return new BusinessException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
                });
        
        if (!productVariant.getIsActive() || !productVariant.getProduct().getIsActive()) {
            log.warn("Product variant hoặc product không active - Variant ID: {}", request.getProductVariantId());
            throw new BusinessException(ErrorCode.PRODUCT_NOT_AVAILABLE);
        }
        
        // 3. Kiểm tra tồn kho
        if (productVariant.getStockQuantity() < request.getQuantity()) {
            log.warn("Không đủ tồn kho - Variant ID: {}, Stock: {}, Requested: {}", 
                    request.getProductVariantId(), productVariant.getStockQuantity(), request.getQuantity());
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
        // 4. Kiểm tra sản phẩm đã có trong giỏ hàng chưa
        Optional<CartItem> existingCartItem = cartItemRepository
                .findByUserIdAndProductVariantId(userId, request.getProductVariantId());
        
        CartItem cartItem;
        if (existingCartItem.isPresent()) {
            // Cập nhật quantity nếu đã có
            cartItem = existingCartItem.get();
            int newQuantity = cartItem.getQuantity() + request.getQuantity();
            
            // Kiểm tra tồn kho với quantity mới
            if (productVariant.getStockQuantity() < newQuantity) {
                log.warn("Không đủ tồn kho cho quantity mới - Variant ID: {}, Stock: {}, New Quantity: {}", 
                        request.getProductVariantId(), productVariant.getStockQuantity(), newQuantity);
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
            }
            
            cartItem.setQuantity(newQuantity);
            log.info("Cập nhật quantity cho cart item có sẵn - ID: {}, New Quantity: {}", 
                    cartItem.getId(), newQuantity);
        } else {
            // Tạo cart item mới
            cartItem = CartItem.builder()
                    .user(user)
                    .productVariant(productVariant)
                    .quantity(request.getQuantity())
                    .build();
            log.info("Tạo cart item mới cho user: {}", userId);
        }
        
        CartItem savedCartItem = cartItemRepository.save(cartItem);
        return buildCartItemResponse(savedCartItem);
    }
    
    @Override
    @Transactional
    public CartItemResponse updateCartItem(UUID userId, UpdateCartItemRequest request) {
        log.info("Cập nhật cart item - User: {}, CartItem: {}, Quantity: {}", 
                userId, request.getCartItemId(), request.getQuantity());
        
        // 1. Validate cart item tồn tại và thuộc về user
        CartItem cartItem = cartItemRepository.findById(request.getCartItemId())
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy cart item với ID: {}", request.getCartItemId());
                    return new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
                });
        
        if (!cartItem.getUser().getId().equals(userId)) {
            log.warn("Cart item không thuộc về user - CartItem: {}, User: {}", 
                    request.getCartItemId(), userId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
        
        // 2. Nếu quantity = 0, xóa cart item
        if (request.getQuantity() == 0) {
            cartItemRepository.delete(cartItem);
            log.info("Đã xóa cart item với quantity = 0 - ID: {}", request.getCartItemId());
            return null; // Trả về null để indicate đã xóa
        }
        
        // 3. Kiểm tra tồn kho
        ProductVariant productVariant = cartItem.getProductVariant();
        if (productVariant.getStockQuantity() < request.getQuantity()) {
            log.warn("Không đủ tồn kho - Variant ID: {}, Stock: {}, Requested: {}", 
                    productVariant.getId(), productVariant.getStockQuantity(), request.getQuantity());
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
        // 4. Cập nhật quantity
        cartItem.setQuantity(request.getQuantity());
        CartItem savedCartItem = cartItemRepository.save(cartItem);
        
        return buildCartItemResponse(savedCartItem);
    }
    
    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartByUserId(UUID userId) {
        log.info("Lấy giỏ hàng của user: {}", userId);
        
        // Lấy tất cả cart items của user với eager loading
        List<CartItem> cartItems = cartItemRepository.findActiveByUserIdWithDetails(userId);
        
        // Convert sang response DTOs
        List<CartItemResponse> cartItemResponses = cartItems.stream()
                .map(this::buildCartItemResponse)
                .collect(Collectors.toList());
        
        // Tính toán tổng kết
        return buildCartResponse(cartItemResponses);
    }
    
    @Override
    @Transactional
    public void removeCartItem(UUID userId, UUID cartItemId) {
        log.info("Xóa cart item - User: {}, CartItem: {}", userId, cartItemId);
        
        // Validate cart item tồn tại và thuộc về user
        if (!cartItemRepository.existsByIdAndUserId(cartItemId, userId)) {
            log.warn("Cart item không tồn tại hoặc không thuộc về user - CartItem: {}, User: {}", 
                    cartItemId, userId);
            throw new BusinessException(ErrorCode.CART_ITEM_NOT_FOUND);
        }
        
        cartItemRepository.deleteById(cartItemId);
        log.info("Đã xóa cart item: {}", cartItemId);
    }
    
    @Override
    @Transactional
    public void clearCart(UUID userId) {
        log.info("Xóa tất cả cart items của user: {}", userId);
        cartItemRepository.deleteByUserId(userId);
        log.info("Đã xóa tất cả cart items của user: {}", userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public long getCartItemCount(UUID userId) {
        return cartItemRepository.countByUserId(userId);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Integer getTotalQuantity(UUID userId) {
        return cartItemRepository.getTotalQuantityByUserId(userId);
    }
    
    /**
     * Build CartItemResponse từ CartItem entity
     */
    private CartItemResponse buildCartItemResponse(CartItem cartItem) {
        ProductVariant variant = cartItem.getProductVariant();
        
        // Tính giá với discountPercentage
        BigDecimal totalPrice = variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        BigDecimal discountedPrice = variant.getDiscountedPrice(); // Sử dụng method từ entity
        BigDecimal totalDiscountedPrice = discountedPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));
        BigDecimal totalDiscountAmount = totalPrice.subtract(totalDiscountedPrice);
        
        return CartItemResponse.builder()
                .id(cartItem.getId())
                .quantity(cartItem.getQuantity())
                .addedDate(cartItem.getAddedDate())
                .updatedDate(cartItem.getUpdatedDate())
                .totalPrice(totalPrice)
                .totalDiscountedPrice(totalDiscountedPrice)
                .totalDiscountAmount(totalDiscountAmount)
                .productVariant(CartItemResponse.ProductVariantInfo.builder()
                        .id(variant.getId())
                        .name(variant.getName())
                        .sku(variant.getSku())
                        .price(variant.getPrice())
                        .discountPercentage(variant.getDiscountPercentage())
                        .discountedPrice(variant.getDiscountedPrice())
                        .discountAmount(variant.getDiscountAmount())
                        .stockQuantity(variant.getStockQuantity())
                        .unit(variant.getUnit())
                        .mainImageUrl(variant.getMainImageUrl())
                        .isActive(variant.getIsActive())
                        .product(CartItemResponse.ProductVariantInfo.ProductInfo.builder()
                                .id(variant.getProduct().getId())
                                .name(variant.getProduct().getName())
                                .slug(variant.getProduct().getSlug())
                                .isActive(variant.getProduct().getIsActive())
                                .build())
                        .build())
                .build();
    }
    
    /**
     * Build CartResponse từ danh sách CartItemResponse
     */
    private CartResponse buildCartResponse(List<CartItemResponse> cartItemResponses) {
        if (cartItemResponses.isEmpty()) {
            return CartResponse.builder()
                    .items(cartItemResponses)
                    .totalItems(0)
                    .totalQuantity(0)
                    .totalAmount(BigDecimal.ZERO)
                    .originalAmount(BigDecimal.ZERO)
                    .totalDiscount(BigDecimal.ZERO)
                    .build();
        }
        
        int totalItems = cartItemResponses.size();
        int totalQuantity = cartItemResponses.stream()
                .mapToInt(CartItemResponse::getQuantity)
                .sum();
        
        BigDecimal originalAmount = cartItemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalAmount = cartItemResponses.stream()
                .map(CartItemResponse::getTotalDiscountedPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        BigDecimal totalDiscount = originalAmount.subtract(totalAmount);
        
        return CartResponse.builder()
                .items(cartItemResponses)
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .totalAmount(totalAmount)
                .originalAmount(originalAmount)
                .totalDiscount(totalDiscount)
                .build();
    }
    
    @Override
    public boolean isItemInCart(UUID userId, UUID productVariantId) {
        log.info("Kiểm tra sản phẩm có trong giỏ hàng - User: {}, Variant: {}", userId, productVariantId);
        return cartItemRepository.findByUserIdAndProductVariantId(userId, productVariantId).isPresent();
    }
    
    @Override
    public Optional<CartItem> getCartItemByUserAndVariant(UUID userId, UUID productVariantId) {
        log.info("Lấy CartItem - User: {}, Variant: {}", userId, productVariantId);
        return cartItemRepository.findByUserIdAndProductVariantId(userId, productVariantId);
    }
    
    @Override
    @Transactional
    public BatchUpdateCartResponse batchUpdateCartItems(UUID userId, BatchUpdateCartRequest request) {
        log.info("Batch update cart items - User: {}, Items count: {}", userId, request.getItems().size());
        
        List<BatchUpdateCartResponse.ItemUpdateResult> results = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int deletedCount = 0;
        
        for (BatchUpdateCartRequest.CartItemUpdate itemUpdate : request.getItems()) {
            BatchUpdateCartResponse.ItemUpdateResult result = processCartItemUpdate(userId, itemUpdate);
            results.add(result);
            
            if (result.isSuccess()) {
                if ("DELETED".equals(result.getStatus())) {
                    deletedCount++;
                } else {
                    successCount++;
                }
            } else {
                failedCount++;
            }
        }
        
        log.info("Batch update completed - Success: {}, Failed: {}, Deleted: {}", 
                successCount, failedCount, deletedCount);
        
        return BatchUpdateCartResponse.builder()
                .results(results)
                .successCount(successCount)
                .failedCount(failedCount)
                .deletedCount(deletedCount)
                .build();
    }
    
    /**
     * Xử lý cập nhật cho từng cart item trong batch
     * Tự động điều chỉnh quantity nếu vượt quá stock (Optimistic UI rollback)
     */
    private BatchUpdateCartResponse.ItemUpdateResult processCartItemUpdate(
            UUID userId, 
            BatchUpdateCartRequest.CartItemUpdate itemUpdate) {
        
        UUID cartItemId = itemUpdate.getCartItemId();
        int requestedQuantity = itemUpdate.getQuantity();
        
        try {
            // 1. Validate cart item tồn tại và thuộc về user
            Optional<CartItem> optionalCartItem = cartItemRepository.findById(cartItemId);
            
            if (optionalCartItem.isEmpty()) {
                return BatchUpdateCartResponse.ItemUpdateResult.builder()
                        .cartItemId(cartItemId)
                        .success(false)
                        .status("FAILED")
                        .message("Không tìm thấy sản phẩm trong giỏ hàng")
                        .requestedQuantity(requestedQuantity)
                        .build();
            }
            
            CartItem cartItem = optionalCartItem.get();
            
            if (!cartItem.getUser().getId().equals(userId)) {
                return BatchUpdateCartResponse.ItemUpdateResult.builder()
                        .cartItemId(cartItemId)
                        .success(false)
                        .status("FAILED")
                        .message("Không có quyền truy cập sản phẩm này")
                        .requestedQuantity(requestedQuantity)
                        .build();
            }
            
            // 2. Nếu quantity = 0, xóa cart item
            if (requestedQuantity == 0) {
                cartItemRepository.delete(cartItem);
                log.info("Batch: Đã xóa cart item với quantity = 0 - ID: {}", cartItemId);
                
                return BatchUpdateCartResponse.ItemUpdateResult.builder()
                        .cartItemId(cartItemId)
                        .success(true)
                        .status("DELETED")
                        .message("Đã xóa sản phẩm khỏi giỏ hàng")
                        .requestedQuantity(requestedQuantity)
                        .actualQuantity(0)
                        .build();
            }
            
            // 3. Kiểm tra tồn kho và điều chỉnh quantity nếu cần
            ProductVariant productVariant = cartItem.getProductVariant();
            int stockQuantity = productVariant.getStockQuantity();
            int actualQuantity = requestedQuantity;
            String message = "Cập nhật thành công";
            
            // Nếu yêu cầu vượt quá tồn kho -> điều chỉnh về max stock
            if (requestedQuantity > stockQuantity) {
                actualQuantity = stockQuantity;
                message = String.format("Số lượng tồn kho không đủ. Đã điều chỉnh từ %d về %d", 
                        requestedQuantity, stockQuantity);
                log.warn("Batch: Điều chỉnh quantity - CartItem: {}, Requested: {}, Adjusted: {}, Stock: {}", 
                        cartItemId, requestedQuantity, actualQuantity, stockQuantity);
            }
            
            // 4. Cập nhật quantity
            cartItem.setQuantity(actualQuantity);
            CartItem savedCartItem = cartItemRepository.save(cartItem);
            
            return BatchUpdateCartResponse.ItemUpdateResult.builder()
                    .cartItemId(cartItemId)
                    .success(true)
                    .status("UPDATED")
                    .message(message)
                    .requestedQuantity(requestedQuantity)
                    .actualQuantity(actualQuantity)
                    .stockQuantity(stockQuantity)
                    .updatedItem(buildCartItemResponse(savedCartItem))
                    .build();
                    
        } catch (Exception e) {
            log.error("Batch: Lỗi khi cập nhật cart item {} - {}", cartItemId, e.getMessage());
            
            return BatchUpdateCartResponse.ItemUpdateResult.builder()
                    .cartItemId(cartItemId)
                    .success(false)
                    .status("FAILED")
                    .message("Lỗi hệ thống: " + e.getMessage())
                    .requestedQuantity(requestedQuantity)
                    .build();
        }
    }
}
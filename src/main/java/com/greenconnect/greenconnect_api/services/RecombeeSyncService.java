package com.greenconnect.greenconnect_api.services;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.entities.Order;
import com.greenconnect.greenconnect_api.entities.OrderDetail;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.repositories.OrderRepository;
import com.recombee.api_client.RecombeeClient;
import com.recombee.api_client.api_requests.AddBookmark;
import com.recombee.api_client.api_requests.AddCartAddition;
import com.recombee.api_client.api_requests.AddDetailView;
import com.recombee.api_client.api_requests.AddPurchase;
import com.recombee.api_client.api_requests.AddRating;
import com.recombee.api_client.api_requests.Batch;
import com.recombee.api_client.api_requests.Request;
import com.recombee.api_client.api_requests.SetItemValues;

import lombok.extern.slf4j.Slf4j;

/**
 * RecombeeSyncService - Giao tiếp với Recombee Recommendation Engine
 * 
 * ⚡ TẤT CẢ methods đều ASYNCHRONOUS (@Async) để không chặn luồng chính
 * ⚠️  Recombee chết → App vẫn sống (chỉ log error)
 * 🔑 RecombeeClient là OPTIONAL - nếu không có, service vẫn tạo nhưng không làm gì
 */
@Service
@Slf4j
public class RecombeeSyncService {
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeClient recombeeClient;
    
    @Autowired(required = false)
    private OrderRepository orderRepository;
    
    @PostConstruct
    public void init() {
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE SERVICE] RecombeeClient is NULL - All tracking features will be DISABLED");
            log.warn("⚠️ [RECOMBEE SERVICE] Check RecombeeConfig initialization logs for details");
        } else {
            log.info("✅ [RECOMBEE SERVICE] RecombeeSyncService initialized successfully with RecombeeClient");
            log.info("   → Product sync: ENABLED");
            log.info("   → User tracking: ENABLED (detail view, cart, bookmark, rating, purchase)");
        }
    }
    
    // ========== CATALOG SYNC ==========
    
    /**
     * Đồng bộ sản phẩm lên Recombee
     * Gửi metadata: name, category, supplier, price, image, etc.
     * 
     * @param product Product entity
     */
    @Async
    public void syncProductToRecombee(Product product) {
        if (recombeeClient == null) {
            log.debug("⚠️ [RECOMBEE] Client not available - Sync skipped");
            return;
        }
        
        if (product == null) {
            log.warn("⚠️ syncProductToRecombee: product is null");
            return;
        }
        
        try {
            String itemId = product.getId().toString();
            
            // Lấy giá trung bình từ variants
            BigDecimal avgPrice = calculateAveragePrice(product.getProductVariants());
            
            // Create HashMap for product metadata (Map.of() only supports 10 pairs)
            java.util.Map<String, Object> values = new java.util.HashMap<>();
            values.put("name", product.getName());
            values.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
            values.put("categoryId", product.getCategory() != null ? product.getCategory().getId().toString() : "");
            values.put("supplier", product.getSupplier() != null ? product.getSupplier().getName() : "");
            values.put("supplierId", product.getSupplier() != null ? product.getSupplier().getId().toString() : "");
            values.put("price", avgPrice.doubleValue());
            values.put("imageUrl", product.getMainImageUrlResolved() != null ? product.getMainImageUrlResolved() : "");
            values.put("rating", product.getAverageRating().doubleValue());
            values.put("reviewCount", product.getReviewCount());
            values.put("isActive", product.getIsActive());
            values.put("isFeatured", product.getIsFeatured());
            values.put("slug", product.getSlug());
            
            SetItemValues request = new SetItemValues(itemId, values)
                .setCascadeCreate(true); // Tự động tạo item nếu chưa tồn tại
            
            recombeeClient.send(request);
            log.debug("✅ Synced product {} to Recombee", itemId);
            
        } catch (Exception e) {
            log.error("❌ Failed to sync product {} to Recombee: {}", 
                product.getId(), e.getMessage());
        }
    }
    
    /**
     * Đồng bộ nhiều sản phẩm (dùng Batch request - hiệu quả hơn)
     * Được sử dụng khi import Excel
     * 
     * @param products List of Product entities
     */
    @Async
    public void syncProductsBatch(List<Product> products) {
        if (recombeeClient == null) {
            log.debug("⚠️ [RECOMBEE] Client not available - Batch sync skipped");
            return;
        }
        
        if (products == null || products.isEmpty()) {
            log.warn("⚠️ syncProductsBatch: empty product list");
            return;
        }
        
        try {
            // Chia thành batch 100 items
            int batchSize = 100;
            for (int i = 0; i < products.size(); i += batchSize) {
                int end = Math.min(i + batchSize, products.size());
                List<Product> batch = products.subList(i, end);
                
                Request[] requests = batch.stream()
                    .map(product -> {
                        String itemId = product.getId().toString();
                        BigDecimal avgPrice = calculateAveragePrice(product.getProductVariants());
                        
                        // Create HashMap for product metadata
                        java.util.Map<String, Object> values = new java.util.HashMap<>();
                        values.put("name", product.getName());
                        values.put("category", product.getCategory() != null ? product.getCategory().getName() : "");
                        values.put("categoryId", product.getCategory() != null ? product.getCategory().getId().toString() : "");
                        values.put("supplier", product.getSupplier() != null ? product.getSupplier().getName() : "");
                        values.put("supplierId", product.getSupplier() != null ? product.getSupplier().getId().toString() : "");
                        values.put("price", avgPrice.doubleValue());
                        values.put("imageUrl", product.getMainImageUrlResolved() != null ? product.getMainImageUrlResolved() : "");
                        values.put("rating", product.getAverageRating().doubleValue());
                        values.put("reviewCount", product.getReviewCount());
                        values.put("isActive", product.getIsActive());
                        values.put("isFeatured", product.getIsFeatured());
                        values.put("slug", product.getSlug());
                        
                        return new SetItemValues(itemId, values)
                            .setCascadeCreate(true);
                    })
                    .toArray(Request[]::new);
                
                recombeeClient.send(new Batch(requests));
                log.info("✅ Synced batch {}-{} ({} products) to Recombee", 
                    i, end, batch.size());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to sync product batch to Recombee: {}", e.getMessage());
        }
    }
    
    /**
     * Xóa sản phẩm khỏi Recombee (hoặc đánh dấu unavailable)
     * 
     * @param productId Product UUID
     */
    @Async
    public void deleteProductFromRecombee(UUID productId) {
        if (recombeeClient == null) {
            log.debug("⚠️ [RECOMBEE] Client not available - Delete skipped");
            return;
        }
        
        try {
            String itemId = productId.toString();
            
            // Option 1: Delete permanently
            // recombeeClient.send(new DeleteItem(itemId));
            
            // Option 2: Đánh dấu unavailable (khuyến nghị - giữ lịch sử)
            java.util.Map<String, Object> values = new java.util.HashMap<>();
            values.put("isActive", false);
            
            recombeeClient.send(
                new SetItemValues(itemId, values)
            );
            
            log.debug("✅ Marked product {} as inactive in Recombee", itemId);
            
        } catch (Exception e) {
            log.error("❌ Failed to delete product {} from Recombee: {}", 
                productId, e.getMessage());
        }
    }
    
    // ========== USER INTERACTIONS ==========
    
    /**
     * Gửi sự kiện "Detail View" - User xem chi tiết sản phẩm
     * 
     * @param userId User UUID
     * @param productId Product UUID
     */
    @Async
    public void trackDetailView(UUID userId, UUID productId) {
        log.debug("🔍 [RECOMBEE TRACKING] trackDetailView called → User: {} | Product: {}", userId, productId);
        
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE TRACKING] Client not available - Detail view tracking skipped for User: {} | Product: {}", userId, productId);
            return;
        }
        
        try {
            log.debug("   → Sending AddDetailView request to Recombee...");
            recombeeClient.send(
                new AddDetailView(userId.toString(), productId.toString())
                    .setTimestamp(new Date())
                    .setCascadeCreate(true)
            );
            
            log.info("✅ [RECOMBEE TRACKING] Detail View tracked → User: {} | Product: {}", userId, productId);
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE TRACKING] Failed to track detail view for User: {} | Product: {} | Error: {}", 
                userId, productId, e.getMessage(), e);
        }
    }
    
    /**
     * Gửi sự kiện "Cart Addition" - User thêm sản phẩm vào giỏ hàng
     * 
     * @param userId User UUID
     * @param productId Product UUID
     * @param amount Số lượng thêm vào
     */
    @Async
    public void trackCartAddition(UUID userId, UUID productId, Integer amount) {
        log.debug("🔍 [RECOMBEE TRACKING] trackCartAddition called → User: {} | Product: {} | Amount: {}", userId, productId, amount);
        
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE TRACKING] Client not available - Cart addition tracking skipped for User: {} | Product: {}", userId, productId);
            return;
        }
        
        try {
            log.debug("   → Sending AddCartAddition request to Recombee...");
            recombeeClient.send(
                new AddCartAddition(userId.toString(), productId.toString())
                    .setTimestamp(new Date())
                    .setAmount(amount)
                    .setCascadeCreate(true)
            );
            
            log.info("✅ [RECOMBEE TRACKING] Cart Addition tracked → User: {} | Product: {} | Amount: {}", 
                userId, productId, amount);
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE TRACKING] Failed to track cart addition for User: {} | Product: {} | Amount: {} | Error: {}", 
                userId, productId, amount, e.getMessage(), e);
        }
    }
    
    /**
     * Gửi sự kiện "Bookmark" - User thêm sản phẩm vào yêu thích
     * 
     * @param userId User UUID
     * @param productId Product UUID
     */
    @Async
    public void trackBookmark(UUID userId, UUID productId) {
        log.debug("🔍 [RECOMBEE TRACKING] trackBookmark called → User: {} | Product: {}", userId, productId);
        
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE TRACKING] Client not available - Bookmark tracking skipped for User: {} | Product: {}", userId, productId);
            return;
        }
        
        try {
            log.debug("   → Sending AddBookmark request to Recombee...");
            recombeeClient.send(
                new AddBookmark(userId.toString(), productId.toString())
                    .setTimestamp(new Date())
                    .setCascadeCreate(true)
            );
            
            log.info("✅ [RECOMBEE TRACKING] Bookmark tracked → User: {} | Product: {}", userId, productId);
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE TRACKING] Failed to track bookmark for User: {} | Product: {} | Error: {}", 
                userId, productId, e.getMessage(), e);
        }
    }
    
    /**
     * Gửi sự kiện "Rating" - User đánh giá sản phẩm
     * 
     * Quy đổi: 1 sao -> -1.0, 2 sao -> -0.5, 3 sao -> 0.0, 4 sao -> 0.5, 5 sao -> 1.0
     * 
     * @param userId User UUID
     * @param productId Product UUID
     * @param starRating Rating (1-5 sao)
     */
    @Async
    public void trackRating(UUID userId, UUID productId, Integer starRating) {
        log.debug("🔍 [RECOMBEE TRACKING] trackRating called → User: {} | Product: {} | Stars: {}", userId, productId, starRating);
        
        if (recombeeClient == null) {
            log.warn("⚠️ [RECOMBEE TRACKING] Client not available - Rating tracking skipped for User: {} | Product: {}", userId, productId);
            return;
        }
        
        try {
            // Quy đổi từ 1-5 sao sang -1.0 đến 1.0
            double normalizedRating = (starRating - 3.0) * 0.5;
            
            log.debug("   → Sending AddRating request to Recombee (normalized: {})...", normalizedRating);
            recombeeClient.send(
                new AddRating(userId.toString(), productId.toString(), normalizedRating)
                    .setTimestamp(new Date())
                    .setCascadeCreate(true)
            );
            
            log.info("✅ [RECOMBEE TRACKING] Rating tracked → User: {} | Product: {} | Stars: {} | Normalized: {}", 
                userId, productId, starRating, normalizedRating);
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE TRACKING] Failed to track rating for User: {} | Product: {} | Stars: {} | Error: {}", 
                userId, productId, starRating, e.getMessage(), e);
        }
    }
    
    /**
     * Gửi sự kiện "Purchase" - User mua hàng
     * 
     * ⚠️ GỌI KHI: 
     * - VNPay callback thành công
     * - COD payment confirmed
     * - Order delivered (nếu chưa gửi ở bước thanh toán)
     * 
     * @param orderId Order UUID
     */
    @Async
    @Transactional(readOnly = true)
    public void trackPurchase(UUID orderId) {
        if (recombeeClient == null) {
            log.debug("⚠️ [RECOMBEE] Client not available - Purchase tracking skipped");
            return;
        }
        
        if (orderRepository == null) {
            log.error("❌ OrderRepository not available for trackPurchase");
            return;
        }
        
        try {
            // Query DB lấy chi tiết đơn hàng
            Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
            
            UUID userId = order.getUser().getId();
            Date timestamp = Date.from(order.getOrderDate().atZone(
                java.time.ZoneId.systemDefault()).toInstant());
            
            // Gửi Purchase cho từng OrderDetail
            List<OrderDetail> orderDetails = order.getOrderDetails();
            
            if (orderDetails == null || orderDetails.isEmpty()) {
                log.warn("⚠️ Order {} has no order details", orderId);
                return;
            }
            
            for (OrderDetail detail : orderDetails) {
                ProductVariant variant = detail.getVariant();
                if (variant == null || variant.getProduct() == null) {
                    log.warn("⚠️ OrderDetail {} has no variant/product", detail.getId());
                    continue;
                }
                
                UUID productId = variant.getProduct().getId();
                BigDecimal price = detail.getSellingPricePerUnit();
                Integer quantity = detail.getQuantity();
                
                recombeeClient.send(
                    new AddPurchase(userId.toString(), productId.toString())
                        .setTimestamp(timestamp)
                        .setPrice(price.doubleValue())
                        .setAmount(quantity)
                        .setCascadeCreate(true)
                );
                
                log.info("💰 [RECOMBEE] Purchase tracked → User: {} | Product: {} | Price: {} | Qty: {}", 
                    userId, productId, price, quantity);
            }
            
            log.info("✅ [RECOMBEE] All purchases tracked successfully → Order: {} | Total items: {}", 
                orderId, orderDetails.size());
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE] Failed to track purchase → Order: {} | Error: {}", 
                orderId, e.getMessage());
        }
    }
    
    /**
     * Gửi sự kiện "Purchase" bằng orderCode - User mua hàng
     * 
     * Wrapper method cho VNPay callback (chỉ có orderCode, không có orderId)
     * 
     * @param orderCode Order code (vnp_TxnRef)
     */
    @Async
    @Transactional(readOnly = true)
    public void trackPurchaseByOrderCode(String orderCode) {
        if (recombeeClient == null) {
            log.debug("⚠️ [RECOMBEE] Client not available - Purchase tracking by order code skipped");
            return;
        }
        
        if (orderRepository == null) {
            log.error("❌ OrderRepository not available for trackPurchaseByOrderCode");
            return;
        }
        
        try {
            log.info("🔍 [RECOMBEE] Looking up order by code: {}", orderCode);
            
            // Query DB lấy order theo orderCode
            Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found with code: " + orderCode));
            
            // Delegate to trackPurchase
            trackPurchase(order.getId());
            
        } catch (Exception e) {
            log.error("❌ [RECOMBEE] Failed to track purchase by order code → Code: {} | Error: {}", 
                orderCode, e.getMessage());
        }
    }
    
    // ========== RECOMMENDATION QUERIES ==========
    
    /**
     * Lấy danh sách sản phẩm gợi ý cá nhân hóa cho user
     * 
     * @param userId User UUID
     * @param count Số lượng sản phẩm cần lấy
     * @return List<String> product IDs (UUID as String)
     */
    public List<String> getPersonalRecommendations(UUID userId, int count) {
        log.info("🎯 [RECOMBEE] Fetching personal recommendations → User: {} | Count: {}", userId, count);
        
        try {
            com.recombee.api_client.api_requests.RecommendItemsToUser request = 
                new com.recombee.api_client.api_requests.RecommendItemsToUser(
                    userId.toString(), 
                    count
                )
                .setScenario("homepage-personal")
                .setReturnProperties(true);
            
            com.recombee.api_client.bindings.RecommendationResponse response = 
                recombeeClient.send(request);
            
            List<String> productIds = new java.util.ArrayList<>();
            for (com.recombee.api_client.bindings.Recommendation rec : response.getRecomms()) {
                productIds.add(rec.getId());
            }
            
            log.info("✅ [RECOMBEE] Personal recommendations fetched → User: {} | Count: {} | Products: {}", 
                userId, productIds.size(), productIds);
            return productIds;
            
        } catch (Exception e) {
            log.warn("❌ [RECOMBEE] Failed to fetch personal recommendations → User: {} | Count: {} | Error: {} | Returning empty list", 
                userId, count, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * Lấy danh sách sản phẩm tương tự
     * 
     * @param productId Product UUID
     * @param userId User UUID (có thể null nếu chưa đăng nhập)
     * @param count Số lượng sản phẩm cần lấy
     * @return List<String> product IDs (UUID as String)
     */
    public List<String> getRelatedRecommendations(UUID productId, UUID userId, int count) {
        log.info("🔗 [RECOMBEE] Fetching related recommendations → Product: {} | User: {} | Count: {}", 
            productId, userId != null ? userId : "anonymous", count);
        
        try {
            com.recombee.api_client.api_requests.RecommendItemsToItem request = 
                new com.recombee.api_client.api_requests.RecommendItemsToItem(
                    productId.toString(),
                    userId != null ? userId.toString() : null,
                    count
                )
                .setScenario("related-items")
                .setReturnProperties(true);
            
            com.recombee.api_client.bindings.RecommendationResponse response = 
                recombeeClient.send(request);
            
            List<String> productIds = new java.util.ArrayList<>();
            for (com.recombee.api_client.bindings.Recommendation rec : response.getRecomms()) {
                productIds.add(rec.getId());
            }
            
            log.info("✅ [RECOMBEE] Related recommendations fetched → Product: {} | Count: {} | Related: {}", 
                productId, productIds.size(), productIds);
            return productIds;
            
        } catch (Exception e) {
            log.warn("❌ [RECOMBEE] Failed to fetch related recommendations → Product: {} | User: {} | Count: {} | Error: {} | Returning empty list", 
                productId, userId != null ? userId : "anonymous", count, e.getMessage());
            return java.util.Collections.emptyList();
        }
    }
    
    // ========== HELPER METHODS ==========
    
    /**
     * Tính giá trung bình từ danh sách variants
     */
    private BigDecimal calculateAveragePrice(List<ProductVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = variants.stream()
            .map(v -> v.getDiscountedPrice() != null ? v.getDiscountedPrice() : v.getPrice())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(variants.size()), 2, java.math.RoundingMode.HALF_UP);
    }
}

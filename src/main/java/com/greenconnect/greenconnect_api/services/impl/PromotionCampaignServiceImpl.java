package com.greenconnect.greenconnect_api.services.impl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignCreateRequest;
import com.greenconnect.greenconnect_api.dtos.response.PromotionCampaignResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductInCampaignResponse;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.PromotionCampaign;
import com.greenconnect.greenconnect_api.entities.PromotionProduct;
import com.greenconnect.greenconnect_api.entities.PromotionProductId;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.PromotionCampaignRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.services.PromotionCampaignService;
import com.greenconnect.greenconnect_api.services.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromotionCampaignServiceImpl implements PromotionCampaignService {

    private final PromotionCampaignRepository campaignRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductService productService;

    @Override
    @Transactional
    public PromotionCampaignResponse createCampaign(PromotionCampaignCreateRequest request) {
        // 1. Kiểm tra nghiệp vụ cơ bản
        if (campaignRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug '" + request.getSlug() + "' đã tồn tại.");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc không được trước ngày bắt đầu.");
        }

        // 2. Tự động tính displayOrder nếu không được cung cấp
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            // Lấy displayOrder lớn nhất hiện tại và +1
            Integer maxOrder = campaignRepository.findMaxDisplayOrder();
            displayOrder = (maxOrder != null) ? maxOrder + 1 : 1;
            log.info("✅ Tự động set displayOrder = {} (max hiện tại: {})", displayOrder, maxOrder);
        }

        // 3. Tạo entity cha (PromotionCampaign) and persist to generate id
        PromotionCampaign campaign = PromotionCampaign.builder()
                .campaignName(request.getCampaignName())
                .description(request.getDescription())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive())
                .slug(request.getSlug())
                .coverBannerUrl(request.getCoverBannerUrl())
                .urlViewAll(request.getUrlViewAll())
                .displayOrder(displayOrder)
                .campaignType(request.getCampaignType())
                .discountPercentage(request.getDiscountPercentage())
                .build();

        // Save early so we have campaign.getId() for embedded keys
        PromotionCampaign initialSaved = campaignRepository.save(campaign);
        final UUID savedCampaignId = initialSaved.getId();

        // 3. Xác thực và chuẩn bị các sản phẩm
        Set<UUID> requestedProductIds = request.getProducts().stream()
                .map(PromotionCampaignCreateRequest.ProductDiscountRequest::getProductId)
                .collect(Collectors.toSet());

        List<Product> foundProducts = productRepository.findAllById(requestedProductIds);
        if (foundProducts.size() != requestedProductIds.size()) {
            throw new IllegalArgumentException("Một hoặc nhiều ID sản phẩm không hợp lệ.");
        }
        Map<UUID, Product> productMap = foundProducts.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        // 4. Tạo các entity con (PromotionProduct) và liên kết chúng
        List<PromotionProduct> promotionProducts = request.getProducts().stream()
                .map(prodRequest -> PromotionProduct.builder()
                        .id(new PromotionProductId(savedCampaignId, prodRequest.getProductId()))
                        .campaign(initialSaved)
                        .product(productMap.get(prodRequest.getProductId()))
                        .discountType(prodRequest.getDiscountType().toString()) // Chuyển Enum thành String
                        .discountValue(prodRequest.getDiscountValue())
                        .build())
                .collect(Collectors.toList());

        initialSaved.setPromotionProducts(promotionProducts);

        // 5. Lưu lại campaign cùng các promotion products (Cascade)
        PromotionCampaign persistedCampaign = campaignRepository.save(initialSaved);

        // 6. TỰ ĐỘNG apply discount cho variants ngay sau khi tạo campaign
        if (persistedCampaign.getDiscountPercentage() != null && 
            persistedCampaign.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
            log.info("🔄 Tự động apply discount {}% cho variants của campaign mới tạo", persistedCampaign.getDiscountPercentage());
            applyDiscountToProductVariants(persistedCampaign.getId());
        }

        // 7. Map kết quả sang DTO để trả về
        return mapToResponse(persistedCampaign);
    }

        @Override
        @Transactional(readOnly = true)
        public PromotionCampaignResponse getCampaignById(java.util.UUID campaignId) {
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                                .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));
                return mapToResponse(campaign);
        }
        
        @Override
        @Transactional(readOnly = true)
        public com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse getCampaignWithCountById(java.util.UUID campaignId) {
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                                .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));
                return mapToResponseWithCount(campaign);
        }

            @Override
            @Transactional
            public PromotionCampaignResponse updateCampaignPartial(java.util.UUID campaignId, com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest request) {
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                // Update non-null fields
                if (request.getCampaignName() != null) campaign.setCampaignName(request.getCampaignName());
                if (request.getDescription() != null) campaign.setDescription(request.getDescription());
                if (request.getStartDate() != null) campaign.setStartDate(request.getStartDate());
                if (request.getEndDate() != null) campaign.setEndDate(request.getEndDate());
                if (request.getIsActive() != null) campaign.setIsActive(request.getIsActive());
                if (request.getSlug() != null) campaign.setSlug(request.getSlug());
                if (request.getCoverBannerUrl() != null) campaign.setCoverBannerUrl(request.getCoverBannerUrl());
                if (request.getUrlViewAll() != null) campaign.setUrlViewAll(request.getUrlViewAll());
                if (request.getDisplayOrder() != null) campaign.setDisplayOrder(request.getDisplayOrder());
                if (request.getCampaignType() != null) campaign.setCampaignType(request.getCampaignType());
                if (request.getDiscountPercentage() != null) campaign.setDiscountPercentage(request.getDiscountPercentage());

                // Append new promotion products if provided
                if (request.getProductsToAdd() != null && !request.getProductsToAdd().isEmpty()) {
                    java.util.Set<java.util.UUID> ids = request.getProductsToAdd().stream().map(com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem::getProductId).collect(java.util.stream.Collectors.toSet());
                    java.util.List<com.greenconnect.greenconnect_api.entities.Product> products = productRepository.findAllById(ids);
                    if (products.size() != ids.size()) throw new IllegalArgumentException("One or more product ids invalid");
                    java.util.Map<java.util.UUID, com.greenconnect.greenconnect_api.entities.Product> prodMap = products.stream().collect(java.util.stream.Collectors.toMap(com.greenconnect.greenconnect_api.entities.Product::getId, p -> p));

                    // ✅ Lọc ra những productIds chưa có trong campaign để tránh duplicate
                    java.util.Set<java.util.UUID> existingProductIds = campaign.getPromotionProducts() != null 
                        ? campaign.getPromotionProducts().stream()
                            .map(pp -> pp.getProduct().getId())
                            .collect(java.util.stream.Collectors.toSet())
                        : java.util.Collections.emptySet();

                    java.util.List<com.greenconnect.greenconnect_api.entities.PromotionProduct> toAdd = request.getProductsToAdd().stream()
                            .filter(p -> !existingProductIds.contains(p.getProductId())) // Skip duplicates
                            .map(p -> com.greenconnect.greenconnect_api.entities.PromotionProduct.builder()
                                    .id(new com.greenconnect.greenconnect_api.entities.PromotionProductId(campaign.getId(), p.getProductId()))
                                    .campaign(campaign)
                                    .product(prodMap.get(p.getProductId()))
                                    .discountType(p.getDiscountType() == null ? null : p.getDiscountType().toString())
                                    .discountValue(p.getDiscountValue())
                                    .build())
                            .collect(java.util.stream.Collectors.toList());

                    // ✅ CÁCH ĐÚNG: Add vào collection hiện tại thay vì tạo list mới
                    if (campaign.getPromotionProducts() == null) {
                        campaign.setPromotionProducts(new java.util.ArrayList<>());
                    }
                    campaign.getPromotionProducts().addAll(toAdd);
                }

                PromotionCampaign saved = campaignRepository.save(campaign);
                
                // TỰ ĐỘNG update discount cho variants sau khi update campaign
                boolean shouldApplyDiscount = false;
                
                // Nếu có update discountPercentage
                if (request.getDiscountPercentage() != null) {
                    if (request.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        shouldApplyDiscount = true;
                        log.info("🔄 Tự động apply discount {}% cho variants sau update", request.getDiscountPercentage());
                    } else {
                        // Remove discount nếu set về 0
                        log.info("🔄 Tự động remove discount từ variants (discount = 0)");
                        removeDiscountFromProductVariants(saved.getId());
                    }
                }
                
                // Nếu có thêm products mới
                if (request.getProductsToAdd() != null && !request.getProductsToAdd().isEmpty()) {
                    shouldApplyDiscount = true;
                    log.info("🔄 Tự động apply discount cho {} products mới thêm", request.getProductsToAdd().size());
                }
                
                // Apply discount nếu cần
                if (shouldApplyDiscount && saved.getDiscountPercentage() != null && 
                    saved.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    applyDiscountToProductVariants(saved.getId());
                }
                
                return mapToResponse(saved);
            }

            @Override
            @Transactional
            public PromotionCampaignResponse addProductsToCampaign(java.util.UUID campaignId, java.util.List<com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem> products) {
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                java.util.Set<java.util.UUID> ids = products.stream().map(com.greenconnect.greenconnect_api.dtos.request.PromotionCampaignUpdateRequest.ProductDiscountItem::getProductId).collect(java.util.stream.Collectors.toSet());
                java.util.List<com.greenconnect.greenconnect_api.entities.Product> found = productRepository.findAllById(ids);
                if (found.size() != ids.size()) throw new IllegalArgumentException("One or more product ids invalid");
                java.util.Map<java.util.UUID, com.greenconnect.greenconnect_api.entities.Product> prodMap = found.stream().collect(java.util.stream.Collectors.toMap(com.greenconnect.greenconnect_api.entities.Product::getId, p -> p));

                // ✅ Lọc ra những productIds chưa có trong campaign để tránh duplicate
                java.util.Set<java.util.UUID> existingProductIds = campaign.getPromotionProducts() != null 
                    ? campaign.getPromotionProducts().stream()
                        .map(pp -> pp.getProduct().getId())
                        .collect(java.util.stream.Collectors.toSet())
                    : java.util.Collections.emptySet();

                java.util.List<com.greenconnect.greenconnect_api.entities.PromotionProduct> toAdd = products.stream()
                        .filter(p -> !existingProductIds.contains(p.getProductId())) // Skip duplicates
                        .map(p -> com.greenconnect.greenconnect_api.entities.PromotionProduct.builder()
                                .id(new com.greenconnect.greenconnect_api.entities.PromotionProductId(campaign.getId(), p.getProductId()))
                                .campaign(campaign)
                                .product(prodMap.get(p.getProductId()))
                                .discountType(p.getDiscountType() == null ? null : p.getDiscountType().toString())
                                .discountValue(p.getDiscountValue())
                                .build())
                        .collect(java.util.stream.Collectors.toList());

                // ✅ CÁCH ĐÚNG: Add vào collection hiện tại thay vì tạo list mới
                if (campaign.getPromotionProducts() == null) {
                    campaign.setPromotionProducts(new java.util.ArrayList<>());
                }
                campaign.getPromotionProducts().addAll(toAdd);

                PromotionCampaign saved = campaignRepository.save(campaign);
                
                // TỰ ĐỘNG apply discount cho variants của products mới thêm
                if (saved.getDiscountPercentage() != null && 
                    saved.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    log.info("🔄 Tự động apply discount cho variants của {} products mới thêm", products.size());
                    applyDiscountToProductVariants(saved.getId());
                }
                
                return mapToResponse(saved);
            }

            @Override
            @Transactional
            public PromotionCampaignResponse addProductsByIds(java.util.UUID campaignId, java.util.List<java.util.UUID> productIds) {
                log.info("Thêm {} products vào campaign {}", productIds.size(), campaignId);
                
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                java.util.List<com.greenconnect.greenconnect_api.entities.Product> found = productRepository.findAllById(productIds);
                if (found.size() != productIds.size()) {
                    throw new IllegalArgumentException("One or more product ids invalid");
                }

                // ✅ Lọc ra những productIds chưa có trong campaign để tránh duplicate
                java.util.Set<java.util.UUID> existingProductIds = campaign.getPromotionProducts() != null 
                    ? campaign.getPromotionProducts().stream()
                        .map(pp -> pp.getProduct().getId())
                        .collect(java.util.stream.Collectors.toSet())
                    : java.util.Collections.emptySet();

                // Tạo PromotionProduct với campaign discount mặc định
                java.util.List<com.greenconnect.greenconnect_api.entities.PromotionProduct> toAdd = found.stream()
                        .filter(product -> !existingProductIds.contains(product.getId())) // Skip duplicates
                        .map(product -> com.greenconnect.greenconnect_api.entities.PromotionProduct.builder()
                                .id(new com.greenconnect.greenconnect_api.entities.PromotionProductId(campaign.getId(), product.getId()))
                                .campaign(campaign)
                                .product(product)
                                .discountType("PERCENTAGE")
                                .discountValue(null) // Sử dụng campaign discount
                                .build())
                        .collect(java.util.stream.Collectors.toList());

                // ✅ CÁCH ĐÚNG: Add vào collection hiện tại thay vì tạo list mới
                if (campaign.getPromotionProducts() == null) {
                    campaign.setPromotionProducts(new java.util.ArrayList<>());
                }
                campaign.getPromotionProducts().addAll(toAdd);

                PromotionCampaign saved = campaignRepository.save(campaign);
                log.info("✅ Đã thêm {} products vào campaign", productIds.size());
                
                // TỰ ĐỘNG apply discount cho variants của products mới thêm
                if (saved.getDiscountPercentage() != null && 
                    saved.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    log.info("🔄 Tự động apply discount cho variants của {} products mới thêm", productIds.size());
                    applyDiscountToProductVariants(saved.getId());
                }
                
                return mapToResponse(saved);
            }

            @Override
            @Transactional
            public void removeProductsByIds(java.util.UUID campaignId, java.util.List<java.util.UUID> productIds) {
                log.info("Xóa {} products khỏi campaign {}", productIds.size(), campaignId);
                
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                if (campaign.getPromotionProducts() == null || campaign.getPromotionProducts().isEmpty()) {
                    log.warn("Campaign không có products để xóa");
                    return;
                }

                java.util.Set<java.util.UUID> idsToRemove = new java.util.HashSet<>(productIds);
                
                // ✅ Đếm số lượng trước khi xóa
                int beforeCount = campaign.getPromotionProducts().size();
                
                // ✅ CÁCH ĐÚNG: Modify existing collection thay vì tạo list mới
                // Điều này giúp JPA/Hibernate detect được thay đổi và trigger orphanRemoval
                campaign.getPromotionProducts().removeIf(pp -> idsToRemove.contains(pp.getProduct().getId()));
                
                int afterCount = campaign.getPromotionProducts().size();
                int actualRemoved = beforeCount - afterCount;
                
                log.info("📊 Before: {}, After: {}, Actually removed: {}", beforeCount, afterCount, actualRemoved);
                
                // ✅ Flush ngay để đảm bảo DELETE được execute
                campaignRepository.saveAndFlush(campaign);
                
                log.info("✅ Đã xóa {} products khỏi campaign (requested: {})", actualRemoved, productIds.size());
                
                // TỰ ĐỘNG recalculate discount cho các products còn lại
                if (campaign.getDiscountPercentage() != null && 
                    campaign.getDiscountPercentage().compareTo(java.math.BigDecimal.ZERO) > 0 &&
                    !campaign.getPromotionProducts().isEmpty()) {
                    log.info("🔄 Tự động recalculate discount cho {} products còn lại", campaign.getPromotionProducts().size());
                    applyDiscountToProductVariants(campaignId);
                }
            }

            @Override
            @Transactional
            public void removeAllProductsFromCampaign(java.util.UUID campaignId) {
                PromotionCampaign campaign = campaignRepository.findById(campaignId)
                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));
                
                // TỰ ĐỘNG remove discount từ variants TRƯỚC KHI xóa products
                if (campaign.getPromotionProducts() != null && !campaign.getPromotionProducts().isEmpty()) {
                    log.info("🔄 Tự động remove discount từ variants trước khi xóa ALL products");
                    removeDiscountFromProductVariants(campaignId);
                    
                    // ✅ CÁCH ĐÚNG: Clear collection hiện tại thay vì set list mới
                    campaign.getPromotionProducts().clear();
                }
                
                campaignRepository.saveAndFlush(campaign);
            }

                @Override
                @Transactional
                public void removeProductFromCampaign(java.util.UUID campaignId, java.util.UUID productId) {
                        PromotionCampaign campaign = campaignRepository.findById(campaignId)
                                        .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

                        if (campaign.getPromotionProducts() == null || campaign.getPromotionProducts().isEmpty()) {
                                // nothing to remove
                                return;
                        }

                        // ✅ CÁCH ĐÚNG: Modify existing collection thay vì tạo list mới
                        campaign.getPromotionProducts().removeIf(pp -> pp.getProduct().getId().equals(productId));
                        
                        campaignRepository.saveAndFlush(campaign);
                }

    @Override
    @Transactional
    public void applyDiscountToProductVariants(UUID campaignId) {
        PromotionCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

        if (campaign.getPromotionProducts() == null || campaign.getPromotionProducts().isEmpty()) {
            throw new IllegalArgumentException("Campaign has no products to apply discount");
        }

        // Lấy discount percentage từ campaign hoặc từ promotion product
        java.math.BigDecimal campaignDiscount = campaign.getDiscountPercentage();

        for (PromotionProduct pp : campaign.getPromotionProducts()) {
            Product product = pp.getProduct();
            
            // Xác định discount percentage để apply
            java.math.BigDecimal discountToApply = campaignDiscount;
            
            // Nếu promotion product có discount riêng (discountType = PERCENTAGE), ưu tiên dùng nó
            if ("PERCENTAGE".equals(pp.getDiscountType()) && pp.getDiscountValue() != null) {
                discountToApply = pp.getDiscountValue();
            }
            
            if (discountToApply == null || discountToApply.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                continue; // Bỏ qua nếu không có discount hợp lệ
            }

            // Apply discount cho TẤT CẢ variants của product
            List<com.greenconnect.greenconnect_api.entities.ProductVariant> variants = 
                productVariantRepository.findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            
            for (com.greenconnect.greenconnect_api.entities.ProductVariant variant : variants) {
                variant.setDiscountPercentage(discountToApply);
                productVariantRepository.save(variant);
            }
        }
    }

    @Override
    @Transactional
    public void removeDiscountFromProductVariants(UUID campaignId) {
        PromotionCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new IllegalArgumentException("PromotionCampaign not found: " + campaignId));

        if (campaign.getPromotionProducts() == null || campaign.getPromotionProducts().isEmpty()) {
            return; // Không có gì để remove
        }

        for (PromotionProduct pp : campaign.getPromotionProducts()) {
            Product product = pp.getProduct();

            // Remove discount từ TẤT CẢ variants của product
            List<com.greenconnect.greenconnect_api.entities.ProductVariant> variants = 
                productVariantRepository.findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            
            for (com.greenconnect.greenconnect_api.entities.ProductVariant variant : variants) {
                variant.setDiscountPercentage(java.math.BigDecimal.ZERO);
                productVariantRepository.save(variant);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getActiveCampaignsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách campaigns active với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromotionCampaign> campaigns = campaignRepository.findByIsActiveTrue(pageable);
        return campaigns.map(this::mapToResponseWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getInactiveCampaignsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách campaigns inactive với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromotionCampaign> campaigns = campaignRepository.findByIsActiveFalse(pageable);
        return campaigns.map(this::mapToResponseWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getAllCampaignsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy tất cả campaigns với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PromotionCampaign> campaigns = campaignRepository.findAll(pageable);
        return campaigns.map(this::mapToResponseWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getValidCampaignsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy campaigns còn hiệu lực với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Page<PromotionCampaign> campaigns = campaignRepository.findByIsActiveTrueAndStartDateBeforeAndEndDateAfter(now, now, pageable);
        return campaigns.map(this::mapToResponseWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse> getExpiredCampaignsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy campaigns đã hết hạn với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        Page<PromotionCampaign> campaigns = campaignRepository.findByEndDateBefore(now, pageable);
        return campaigns.map(this::mapToResponseWithCount);
    }

    @Override
    @Transactional(readOnly = true)
    public com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse getLatestValidBulkPurchaseCampaign() {
        log.info("🛒 Lấy campaign BULK_PURCHASE mới nhất còn hiệu lực");
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        PromotionCampaign campaign = campaignRepository.findLatestValidCampaignByType(
            com.greenconnect.greenconnect_api.enums.CampaignType.BULK_PURCHASE, now)
            .orElseThrow(() -> new com.greenconnect.greenconnect_api.exceptions.DataNotFoundException(
                "Không tìm thấy campaign ưu đãi mua nhiều nào đang còn hiệu lực"));
        
        log.info("✅ Tìm thấy campaign BULK_PURCHASE: {} - {}", campaign.getId(), campaign.getCampaignName());
        return mapToResponseWithCount(campaign);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PromotionCampaignResponse> searchCampaigns(String keyword, String tab, int page, int size) {
        log.info("🔍 Tìm kiếm campaigns - Từ khóa: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<PromotionCampaign> campaigns;
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        switch (tab.toLowerCase()) {
            case "active":
                campaigns = campaignRepository.searchActiveCampaignsByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong campaigns ACTIVE - Tìm thấy {} kết quả", campaigns.getTotalElements());
                break;
            case "inactive":
                campaigns = campaignRepository.searchInactiveCampaignsByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong campaigns INACTIVE - Tìm thấy {} kết quả", campaigns.getTotalElements());
                break;
            case "valid":
                campaigns = campaignRepository.searchValidCampaignsByKeyword(keyword, now, pageable);
                log.info("✅ Tìm kiếm trong campaigns VALID (đang chạy) - Tìm thấy {} kết quả", campaigns.getTotalElements());
                break;
            case "expired":
                campaigns = campaignRepository.searchExpiredCampaignsByKeyword(keyword, now, pageable);
                log.info("✅ Tìm kiếm trong campaigns EXPIRED - Tìm thấy {} kết quả", campaigns.getTotalElements());
                break;
            case "all":
                campaigns = campaignRepository.searchAllCampaignsByKeyword(keyword, pageable);
                log.info("✅ Tìm kiếm trong TẤT CẢ campaigns - Tìm thấy {} kết quả", campaigns.getTotalElements());
                break;
            default:
                log.warn("⚠️ Tab không hợp lệ: '{}' - Sử dụng 'all' mặc định", tab);
                campaigns = campaignRepository.searchAllCampaignsByKeyword(keyword, pageable);
                break;
        }
        
        return campaigns.map(this::mapToResponse);
    }

    private PromotionCampaignResponse mapToResponse(PromotionCampaign campaign) {
        List<PromotionCampaignResponse.ProductDiscountResponse> productDiscounts = 
            (campaign.getPromotionProducts() != null) 
                ? campaign.getPromotionProducts().stream()
                    .map(pp -> PromotionCampaignResponse.ProductDiscountResponse.builder()
                        .productId(pp.getProduct().getId())
                        .discountType(pp.getDiscountType() != null ? 
                            com.greenconnect.greenconnect_api.enums.DiscountType.valueOf(pp.getDiscountType()) : null)
                        .discountValue(pp.getDiscountValue())
                        .build())
                    .collect(java.util.stream.Collectors.toList())
                : new java.util.ArrayList<>();

        return PromotionCampaignResponse.builder()
                .id(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .description(campaign.getDescription())
                .slug(campaign.getSlug())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .isActive(campaign.getIsActive())
                .coverBannerUrl(campaign.getCoverBannerUrl())
                .urlViewAll(campaign.getUrlViewAll())
                .campaignType(campaign.getCampaignType())
                .displayOrder(campaign.getDisplayOrder())
                .discountPercentage(campaign.getDiscountPercentage())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .promotionProducts(productDiscounts)
                .build();
    }
    
    /**
     * Map entity sang response DTO VỚI product count
     */
    private com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse mapToResponseWithCount(PromotionCampaign campaign) {
        // Đếm số lượng products trong campaign
        int productCount = (campaign.getPromotionProducts() != null) 
            ? campaign.getPromotionProducts().size() 
            : 0;
        
        return com.greenconnect.greenconnect_api.dtos.response.PromotionCampainCountResponse.builder()
                .id(campaign.getId())
                .campaignName(campaign.getCampaignName())
                .description(campaign.getDescription())
                .slug(campaign.getSlug())
                .startDate(campaign.getStartDate())
                .endDate(campaign.getEndDate())
                .isActive(campaign.getIsActive())
                .coverBannerUrl(campaign.getCoverBannerUrl())
                .urlViewAll(campaign.getUrlViewAll())
                .campaignType(campaign.getCampaignType())
                .displayOrder(campaign.getDisplayOrder())
                .discountPercentage(campaign.getDiscountPercentage())
                .createdAt(campaign.getCreatedAt())
                .updatedAt(campaign.getUpdatedAt())
                .productCount(productCount) // ⭐ Số lượng products
                .build();
    }
    
    /**
     * Lấy danh sách products với phân trang, đánh dấu products đã có trong campaign
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductInCampaignResponse> getProductsWithCampaignStatus(
            UUID campaignId, 
            UUID categoryId, 
            int page, 
            int size) {
        
        log.info("📦 Lấy danh sách products cho campaign {} - category: {}, page: {}, size: {}", 
            campaignId, categoryId, page, size);
        
        // 1. Validate campaign tồn tại
        PromotionCampaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign không tồn tại: " + campaignId));
        
        // 2. Lấy danh sách product IDs đã có trong campaign
        Set<UUID> productIdsInCampaign = campaign.getPromotionProducts().stream()
            .map(pp -> pp.getProduct().getId())
            .collect(Collectors.toSet());
        
        // 3. Tạo Map để lookup PromotionProduct info nhanh
        Map<UUID, PromotionProduct> promotionProductMap = campaign.getPromotionProducts().stream()
            .collect(Collectors.toMap(
                pp -> pp.getProduct().getId(),
                Function.identity()
            ));
        
        log.info("✅ Campaign có {} products", productIdsInCampaign.size());
        
        // 4. Lấy products theo category (hoặc all) với phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Product> productPage;
        if (categoryId != null) {
            // Filter by category using Specification
            org.springframework.data.jpa.domain.Specification<Product> spec = 
                (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
            productPage = productRepository.findAll(spec, pageable);
            log.info("📂 Lọc theo category: {} - Tìm thấy {} products", categoryId, productPage.getTotalElements());
        } else {
            // All categories
            productPage = productRepository.findAll(pageable);
            log.info("📂 Lấy tất cả categories - Tìm thấy {} products", productPage.getTotalElements());
        }
        
        // 5. Convert sang ProductInCampaignResponse
        Page<ProductInCampaignResponse> result = productPage.map(product -> {
            // Get full ProductResponse từ ProductService
            ProductResponse productResponse = productService.getProductById(product.getId());
            
            // Check xem product có trong campaign không
            boolean isInCampaign = productIdsInCampaign.contains(product.getId());
            
            // Lấy thông tin PromotionProduct nếu có
            UUID promotionProductId = null;
            java.math.BigDecimal productDiscountValue = null;
            
            if (isInCampaign) {
                PromotionProduct pp = promotionProductMap.get(product.getId());
                if (pp != null) {
                    // PromotionProductId là composite key (campaignId + productId)
                    // Không có promotionProductId riêng, dùng productId
                    promotionProductId = pp.getId().getProductId();
                    productDiscountValue = pp.getDiscountValue();
                }
            }
            
            // Tạo ProductInCampaignResponse
            return new ProductInCampaignResponse(
                productResponse,
                isInCampaign,
                promotionProductId,
                productDiscountValue
            );
        });
        
        log.info("✅ Trả về {} products (trong đó {} products đã trong campaign)", 
            result.getNumberOfElements(), 
            result.getContent().stream().filter(ProductInCampaignResponse::getIsInCampaign).count());
        
        return result;
    }
    
    /**
     * Lấy danh sách TẤT CẢ products với phân trang theo category
     * CHỈ trả về ProductResponse đơn giản theo category hoặc all
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProductsByCategory(
            UUID categoryId, 
            int page, 
            int size) {
        
        log.info("📦 Lấy danh sách TẤT CẢ products - category: {}, page: {}, size: {}", 
            categoryId, page, size);
        
        // Lấy products theo category (hoặc all) với phân trang
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<Product> productPage;
        if (categoryId != null) {
            // Filter theo category
            org.springframework.data.jpa.domain.Specification<Product> spec = (root, query, cb) -> 
                cb.equal(root.get("category").get("id"), categoryId);
            productPage = productRepository.findAll(spec, pageable);
            log.info("✅ Lấy products từ category: {}", categoryId);
        } else {
            // Lấy tất cả products
            productPage = productRepository.findAll(pageable);
            log.info("✅ Lấy tất cả products");
        }
        
        // Convert sang ProductResponse
        Page<ProductResponse> result = productPage.map(product -> 
            productService.getProductById(product.getId())
        );
        
        log.info("✅ Trả về {} products", result.getNumberOfElements());
        
        return result;
    }
}
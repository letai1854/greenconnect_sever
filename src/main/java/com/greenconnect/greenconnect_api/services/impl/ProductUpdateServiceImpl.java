package com.greenconnect.greenconnect_api.services.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRatingRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.entities.Category;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductImage;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.Supplier;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.repositories.CategoryRepository;
import com.greenconnect.greenconnect_api.repositories.ProductImageRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.repositories.SupplierRepository;
import com.greenconnect.greenconnect_api.services.ProductUpdateService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;
import com.greenconnect.greenconnect_api.utils.SlugUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductUpdateServiceImpl implements ProductUpdateService {
    
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        log.info("🔄 Bắt đầu cập nhật sản phẩm: {}", productId);
        
        // 1. Tìm và validate Product exists
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm với ID: {}", productId);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        
        // 2. Update basic product info
        boolean basicChanged = updateBasicProductInfo(product, request);
        if (basicChanged) {
            log.info("✅ Cập nhật thông tin cơ bản thành công");
        }
        
        // 3. Update category if changed
        boolean categoryChanged = updateProductCategory(product, request);
        
        // 4. Update supplier if changed
        boolean supplierChanged = updateProductSupplier(product, request);
        
        // 5. Update main image URL
        if (request.getMainImageUrl() != null && !request.getMainImageUrl().equals(product.getMainImageUrl())) {
            product.setMainImageUrl(request.getMainImageUrl());
            log.info("📸 Main Image updated");
        }
        
        // 6. ⭐ CẬP NHẬT ẢNH - Thao tác trực tiếp trên product.getProductImages()
        if (request.getImages() != null) {
            updateProductImages(product, request.getImages());
        }
        
        // 7. Update variants (QUAN TRỌNG)
        if (request.getVariants() != null) {
            updateProductVariants(product, request);
        }
        
        // 8. 💾 FORCE SAVE AND FLUSH - Ẩp Hibernate đồng bộ ngay lập tức
        // Dùng saveAndFlush để tránh lỗi lost update do lazy write
        Product savedProduct = productRepository.saveAndFlush(product);
        log.info("✅ Đã lưu xuống DB sản phẩm: {}", productId);
        
        // 9. Sync to Recombee (async - không chặn luồng chính)
        // ⚠️ QUAN TRỌNG: Load lazy relationships TRƯỚC KHI gọi async để tránh LazyInitializationException
        if (recombeeSyncService != null) {
            // Force initialize lazy-loaded entities trước khi transaction đóng
            if (savedProduct.getCategory() != null) {
                savedProduct.getCategory().getName(); // Trigger lazy loading
            }
            if (savedProduct.getSupplier() != null) {
                savedProduct.getSupplier().getName(); // Trigger lazy loading
            }
            if (savedProduct.getProductVariants() != null) {
                savedProduct.getProductVariants().size(); // Trigger lazy loading
            }
            
            recombeeSyncService.syncProductToRecombee(savedProduct);
        }
        
        // 10. Load updated product with variants and return response
        return buildProductResponse(savedProduct);
    }
    
    @Override
    @Transactional
    public ProductResponse updateProductRating(UUID productId, UpdateProductRatingRequest request) {
        log.info("Cập nhật rating sản phẩm: {} - Rating: {} - Reviews: {}", 
                productId, request.getAverageRating(), request.getReviewCount());
        
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm với ID: {}", productId);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        
        product.setAverageRating(request.getAverageRating());
        product.setReviewCount(request.getReviewCount());
        
        Product savedProduct = productRepository.save(product);
        log.info("Đã cập nhật rating cho sản phẩm: {}", productId);
        
        // Sync to Recombee (async - không chặn luồng chính)
        // ⚠️ QUAN TRỌNG: Load lazy relationships TRƯỚC KHI gọi async
        if (recombeeSyncService != null) {
            // Force initialize lazy-loaded entities trước khi transaction đóng
            if (savedProduct.getCategory() != null) {
                savedProduct.getCategory().getName(); // Trigger lazy loading
            }
            if (savedProduct.getSupplier() != null) {
                savedProduct.getSupplier().getName(); // Trigger lazy loading
            }
            if (savedProduct.getProductVariants() != null) {
                savedProduct.getProductVariants().size(); // Trigger lazy loading
            }
            
            recombeeSyncService.syncProductToRecombee(savedProduct);
        }
        
        return buildProductResponse(savedProduct);
    }
    
    private boolean updateBasicProductInfo(Product product, UpdateProductRequest request) {
        boolean hasChanges = false;
        
        // Update name
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            String newName = request.getName().trim();
            if (!newName.equals(product.getName())) {
                log.info("✏️ Đổi tên từ '{}' -> '{}'", product.getName(), newName);
                // ⚠️ BỎ VALIDATION TÊN TRÙNG - Cho phép nhiều sản phẩm cùng tên
                // if (productRepository.existsByNameIgnoreCaseAndIdNot(newName, product.getId())) {
                //     throw new BusinessException(ErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
                // }
                product.setName(newName);
                hasChanges = true;
                
                // Auto-update slug if name changed and no custom slug provided
                if (request.getSlug() == null) {
                    String newSlug = SlugUtils.createSlug(newName);
                    if (productRepository.existsBySlug(newSlug)) {
                        newSlug = newSlug + "-" + System.currentTimeMillis() % 10000;
                    }
                    product.setSlug(newSlug);
                }
            }
        }
        
        // Update description (Cho phép set về rỗng nếu user muốn xóa mô tả)
        if (request.getDescription() != null) {
            String newDescription = request.getDescription().trim();
            if (!java.util.Objects.equals(newDescription, product.getDescription())) {
                log.info("📝 Cập nhật mô tả");
                product.setDescription(newDescription);
                hasChanges = true;
            }
        }
        
        // Update custom slug
        if (request.getSlug() != null && !request.getSlug().trim().isEmpty()) {
            String newSlug = request.getSlug().trim();
            if (!newSlug.equals(product.getSlug())) {
                if (productRepository.existsBySlugAndIdNot(newSlug, product.getId())) {
                    throw new BusinessException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
                }
                product.setSlug(newSlug);
                hasChanges = true;
            }
        }
        
        // Update isActive
        if (request.getIsActive() != null && !request.getIsActive().equals(product.getIsActive())) {
            product.setIsActive(request.getIsActive());
            hasChanges = true;
        }
        
        // Update isFeatured
        if (request.getIsFeatured() != null && !request.getIsFeatured().equals(product.getIsFeatured())) {
            product.setIsFeatured(request.getIsFeatured());
            hasChanges = true;
        }
        
        return hasChanges;
    }
    
    private boolean updateProductCategory(Product product, UpdateProductRequest request) {
        if (request.getCategoryId() != null && !request.getCategoryId().equals(product.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> {
                        log.warn("Không tìm thấy category với ID: {}", request.getCategoryId());
                        return new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                    });
            
            product.setCategory(newCategory);
            log.info("Đã thay đổi category của sản phẩm {} từ {} sang {}", 
                    product.getId(), product.getCategory().getName(), newCategory.getName());
            return true;
        }
        return false;
    }
    
    private boolean updateProductSupplier(Product product, UpdateProductRequest request) {
        if (request.getSupplierId() != null && !request.getSupplierId().equals(product.getSupplier().getId())) {
            Supplier newSupplier = supplierRepository.findById(request.getSupplierId())
                    .orElseThrow(() -> {
                        log.warn("Không tìm thấy supplier với ID: {}", request.getSupplierId());
                        return new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND);
                    });
            
            product.setSupplier(newSupplier);
            log.info("Đã thay đổi supplier của sản phẩm {} từ {} sang {}", 
                    product.getId(), product.getSupplier().getName(), newSupplier.getName());
            return true;
        }
        return false;
    }
    
    /**
     * ⭐ UPDATE PRODUCT VARIANTS - Dùng Map để tìm kiếm O(1)
     * Giải quyết "Lost Update" bug do xung đột Hibernate state
     */
    private void updateProductVariants(Product product, UpdateProductRequest request) {
        if (request.getVariants() == null || request.getVariants().isEmpty()) {
            return;
        }
        
        log.info("🛠 Bắt đầu cập nhật {} variants", request.getVariants().size());
        
        List<UpdateProductRequest.UpdateProductVariantRequest> variantRequests = request.getVariants();
        List<ProductVariant> currentVariants = product.getProductVariants();
        
        // --- BƯỚC 1: XÁC ĐỊNH VARIANT CẦN GIỮ LẠI ---
        List<UUID> keepIds = variantRequests.stream()
                .map(UpdateProductRequest.UpdateProductVariantRequest::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        
        log.info("📋 Variant được giữ lại (keepIds): {}", keepIds);
        
        // --- BƯỚC 2: XÓA ORPHAN VARIANTS ---
        int removedCount = currentVariants.size();
        currentVariants.removeIf(v -> !keepIds.contains(v.getId()));
        removedCount = removedCount - currentVariants.size();
        
        if (removedCount > 0) {
            log.info("🗑️ Đã remove {} variants khỏi list (sẽ bị xóa khi save)", removedCount);
        }
        
        // --- BƯỚC 3: RESET TẤT CẢ isDefault VỀ FALSE ---
        currentVariants.forEach(v -> v.setIsDefault(false));
        
        // --- BƯỚC 4: TẠO MAP ĐỂ TRA CỨU NHANH O(1) ---
        java.util.Map<UUID, ProductVariant> currentMap = currentVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, java.util.function.Function.identity()));
        
        boolean hasDefault = false;
        
        // --- BƯỚC 5: CẬP NHẬT HOẶC THÊM MỚI ---
        for (UpdateProductRequest.UpdateProductVariantRequest vReq : variantRequests) {
            boolean reqIsDefault = Boolean.TRUE.equals(vReq.getIsDefault());
            
            if (vReq.getId() != null && currentMap.containsKey(vReq.getId())) {
                // 🔄 UPDATE - Dùng Map.get() thay vì stream().filter()
                ProductVariant existing = currentMap.get(vReq.getId());
                
                if (vReq.getName() != null) existing.setName(vReq.getName());
                if (vReq.getSku() != null) existing.setSku(vReq.getSku());
                if (vReq.getPrice() != null) existing.setPrice(vReq.getPrice());
                if (vReq.getDiscountPercentage() != null) existing.setDiscountPercentage(vReq.getDiscountPercentage());
                if (vReq.getStockQuantity() != null) existing.setStockQuantity(vReq.getStockQuantity());
                if (vReq.getUnit() != null) existing.setUnit(vReq.getUnit());
                if (vReq.getIsActive() != null) existing.setIsActive(vReq.getIsActive());
                
                // Set default (đã reset về false ở bước 3)
                if (reqIsDefault) {
                    existing.setIsDefault(true);
                    hasDefault = true;
                    log.info("⭐ Set variant {} làm default", existing.getId());
                }
                
                log.info("🔄 Updated variant: {} (isDefault: {})", existing.getId(), existing.getIsDefault());
                
            } else if (vReq.getId() == null) {
                // ➕ CREATE NEW
                String sku = vReq.getSku();
                if (sku == null || sku.trim().isEmpty()) {
                    sku = generateSku(product.getName(), vReq.getName());
                }
                
                ProductVariant newVariant = ProductVariant.builder()
                        .product(product)
                        .name(vReq.getName())
                        .sku(sku)
                        .price(vReq.getPrice())
                        .discountPercentage(vReq.getDiscountPercentage())
                        .stockQuantity(vReq.getStockQuantity())
                        .unit(vReq.getUnit())
                        .isActive(vReq.getIsActive() != null ? vReq.getIsActive() : true)
                        .isDefault(reqIsDefault)
                        .build();
                
                currentVariants.add(newVariant);
                if (reqIsDefault) {
                    hasDefault = true;
                    log.info("⭐ Variant mới là default");
                }
                log.info("➕ Added new variant: {} (isDefault: {})", vReq.getName(), reqIsDefault);
            }
        }
        
        // --- BƯỚC 6: FALLBACK DEFAULT ---
        if (!hasDefault && !currentVariants.isEmpty()) {
            currentVariants.get(0).setIsDefault(true);
            log.info("⚠️ Không có default nào được chọn, set variant đầu tiên làm default");
        }
        
        log.info("✅ Hoàn tất update variants (list size: {})", currentVariants.size());
    }
    
    private String generateSku(String productName, String variantName) {
        String sku = (productName + "-" + variantName)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        return sku + "-" + System.currentTimeMillis() % 10000;
    }
    
    private ProductResponse buildProductResponse(Product product) {
        // Build category info
        ProductResponse.CategoryInfo categoryInfo = ProductResponse.CategoryInfo.builder()
                .id(product.getCategory().getId())
                .name(product.getCategory().getName())
                .imageUrl(product.getCategory().getImageUrl())
                .build();
        
        // Build supplier info
        ProductResponse.SupplierInfo supplierInfo = ProductResponse.SupplierInfo.builder()
                .id(product.getSupplier().getId())
                .name(product.getSupplier().getName())
                .contactEmail(product.getSupplier().getEmail())
                .phoneNumber(product.getSupplier().getPhoneNumber())
                .build();
        
        // Get updated variants
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
        
        // Get product images (all variants dùng chung)
        List<ProductImage> productImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        List<ProductResponse.ProductImageInfo> imageInfos = productImages.stream()
                .map(image -> ProductResponse.ProductImageInfo.builder()
                        .id(image.getId())
                        .mediaType(image.getMediaType().name())
                        .mediaUrl(image.getMediaUrl())
                        .displayOrder(image.getDisplayOrder())
                        .isMain(image.getIsMain())
                        .build())
                .collect(Collectors.toList());
        
        List<ProductResponse.ProductVariantInfo> variantInfos = variants.stream()
                .map(variant -> {
                    
                    return ProductResponse.ProductVariantInfo.builder()
                            .id(variant.getId())
                            .name(variant.getName())
                            .sku(variant.getSku())
                            .price(variant.getPrice())
                            .discountPercentage(variant.getDiscountPercentage())
                            .discountedPrice(variant.getDiscountedPrice())
                            .discountAmount(variant.getDiscountAmount())
                            .stockQuantity(variant.getStockQuantity())
                            .unit(variant.getUnit())
                            // ⚠️ mainImageUrl đã chuyển lên product level
                            .isActive(variant.getIsActive())
                            .isDefault(variant.getIsDefault())
                            // ⚠️ images đã chuyển lên product level
                            .build();
                })
                .collect(Collectors.toList());
        
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .slug(product.getSlug())
                .isActive(product.getIsActive())
                .isFeatured(product.getIsFeatured())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .category(categoryInfo)
                .supplier(supplierInfo)
                .mainImageUrl(product.getMainImageUrl()) // ⭐ Main image ở product level
                .images(imageInfos) // ⭐ All images ở product level
                .variants(variantInfos)
                .build();
    }
    
    /**
     * ⭐ UPDATE PRODUCT IMAGES - Thao tác trực tiếp trên product.getProductImages() list
     * Hibernate sẽ tự động đồng bộ với DB thông qua orphanRemoval=true
     */
    private void updateProductImages(Product product, List<UpdateProductRequest.UpdateProductImageRequest> imageRequests) {
        log.info("Updating images via List manipulation for product: {}", product.getId());
        
        // --- BƯỚC 1: XÁC ĐỊNH ẢNH CẦN GIỮ LẠI ---
        // Lấy danh sách ID của các ảnh mà Frontend gửi lên (không bao gồm ảnh mới id=null)
        List<UUID> keepIds = imageRequests.stream()
                .map(UpdateProductRequest.UpdateProductImageRequest::getId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
        
        log.info("📋 Ảnh được giữ lại (keepIds): {}", keepIds);
        
        // --- BƯỚC 2: XÓA ORPHAN (ẢNH THỪA) TRONG JAVA LIST ---
        // Lệnh này sẽ khiến Hibernate tự động DELETE các ảnh bị remove khỏi list khi save Product
        // Do có orphanRemoval=true, không cần gọi repository.delete()
        int removedCount = product.getProductImages().size();
        product.getProductImages().removeIf(img -> !keepIds.contains(img.getId()));
        removedCount = removedCount - product.getProductImages().size();
        
        if (removedCount > 0) {
            log.info("🗑️ Đã remove {} orphan images khỏi list (sẽ bị xóa khi save)", removedCount);
        }
        
        // --- BƯỚC 3: RESET TẤT CẢ isMain VỀ FALSE ---
        product.getProductImages().forEach(img -> img.setIsMain(false));
        
        // --- BƯỚC 4: CẬP NHẬT HOẶC THÊM MỚI ---
        for (UpdateProductRequest.UpdateProductImageRequest req : imageRequests) {
            
            // Lấy cờ isMain từ Frontend (tin tưởng vào Radio Button của Frontend)
            boolean isMain = Boolean.TRUE.equals(req.getIsMain());
            
            if (req.getId() == null) {
                // ➕ A. ẢNH MỚI -> Tạo object và add vào list của Product
                ProductImage newImg = ProductImage.builder()
                        .product(product) // Quan trọng: link ngược lại parent
                        .mediaType(com.greenconnect.greenconnect_api.enums.MediaType.valueOf(req.getMediaType()))
                        .mediaUrl(req.getMediaUrl())
                        .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                        .isMain(isMain)
                        .build();
                
                // Thêm vào list quản lý của Product - Hibernate sẽ INSERT khi save Product
                product.getProductImages().add(newImg);
                log.info("➕ Added new image to list: {} (isMain: {})", req.getMediaUrl(), isMain);
                
            } else {
                // 🔄 B. ẢNH CŨ -> Tìm trong list hiện tại và cập nhật field
                product.getProductImages().stream()
                    .filter(img -> img.getId().equals(req.getId()))
                    .findFirst()
                    .ifPresent(existingImg -> {
                        // Cập nhật các field
                        if (req.getMediaUrl() != null) {
                            existingImg.setMediaUrl(req.getMediaUrl());
                        }
                        if (req.getDisplayOrder() != null) {
                            existingImg.setDisplayOrder(req.getDisplayOrder());
                        }
                        if (req.getMediaType() != null) {
                            existingImg.setMediaType(com.greenconnect.greenconnect_api.enums.MediaType.valueOf(req.getMediaType()));
                        }
                        // ⭐ Cập nhật isMain
                        existingImg.setIsMain(isMain);
                        
                        log.info("🔄 Updated existing image: {} (isMain: {})", req.getId(), isMain);
                    });
            }
        }
        
        // ⚠️ KHÔNG GỌI repository.save() ở đây!
        // Hàm cha (updateProduct) sẽ gọi productRepository.save(product) ở cuối
        // và Hibernate sẽ tự động đồng bộ list changes xuống DB
        
        log.info("✅ Hoàn tất update images (current list size: {})", product.getProductImages().size());
    }
}
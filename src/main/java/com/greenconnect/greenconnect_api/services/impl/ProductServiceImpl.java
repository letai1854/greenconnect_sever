package com.greenconnect.greenconnect_api.services.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.greenconnect.greenconnect_api.dtos.request.BatchPriceUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateProductRequest;
import com.greenconnect.greenconnect_api.dtos.request.ProductFilterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRatingRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.BatchPriceUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductWithTopReviewResponse;
import com.greenconnect.greenconnect_api.entities.Category;
import com.greenconnect.greenconnect_api.entities.Product;
import com.greenconnect.greenconnect_api.entities.ProductImage;
import com.greenconnect.greenconnect_api.entities.ProductReview;
import com.greenconnect.greenconnect_api.entities.ProductVariant;
import com.greenconnect.greenconnect_api.entities.Supplier;
import com.greenconnect.greenconnect_api.enums.MediaType;
import com.greenconnect.greenconnect_api.exceptions.BusinessException;
import com.greenconnect.greenconnect_api.exceptions.ErrorCode;
import com.greenconnect.greenconnect_api.mappers.ProductMapper;
import com.greenconnect.greenconnect_api.repositories.CategoryRepository;
import com.greenconnect.greenconnect_api.repositories.ProductImageRepository;
import com.greenconnect.greenconnect_api.repositories.ProductRepository;
import com.greenconnect.greenconnect_api.repositories.ProductVariantRepository;
import com.greenconnect.greenconnect_api.repositories.SupplierRepository;
import com.greenconnect.greenconnect_api.services.ProductService;
import com.greenconnect.greenconnect_api.services.ProductUpdateService;
import com.greenconnect.greenconnect_api.services.RecombeeSyncService;
import com.greenconnect.greenconnect_api.specifications.ProductSpecification;
import com.greenconnect.greenconnect_api.utils.SlugUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {
    
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ProductUpdateService productUpdateService;
    private final com.greenconnect.greenconnect_api.repositories.FavoriteRepository favoriteRepository;
    
    @Autowired(required = false) // ⭐ OPTIONAL - Recombee có thể không khả dụng
    private RecombeeSyncService recombeeSyncService;
    
    @Override
    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Tạo sản phẩm mới: {}", request.getName());
        
        // 1. Validate và lấy Category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy category với ID: {}", request.getCategoryId());
                    return new BusinessException(ErrorCode.CATEGORY_NOT_FOUND);
                });
        
        // 2. Validate và lấy Supplier
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy supplier với ID: {}", request.getSupplierId());
                    return new BusinessException(ErrorCode.SUPPLIER_NOT_FOUND);
                });
        
        // 3. Validate tên sản phẩm unique
        if (productRepository.existsByNameIgnoreCase(request.getName())) {
            log.warn("Tên sản phẩm đã tồn tại: {}", request.getName());
            throw new BusinessException(ErrorCode.PRODUCT_NAME_ALREADY_EXISTS);
        }
        
        // 4. Generate slug nếu không có
        String slug = request.getSlug();
        if (slug == null || slug.trim().isEmpty()) {
            slug = SlugUtils.createSlug(request.getName());
        }
        
        // 5. Validate slug unique
        if (productRepository.existsBySlug(slug)) {
            log.warn("Slug đã tồn tại: {}", slug);
            throw new BusinessException(ErrorCode.PRODUCT_SLUG_ALREADY_EXISTS);
        }
        
        // 6. Tạo Product entity
        Product product = Product.builder()
                .category(category)
                .supplier(supplier)
                .name(request.getName().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .slug(slug)
                .isActive(request.getIsActive())
                .isFeatured(request.getIsFeatured())
                .averageRating(BigDecimal.ZERO) // Mặc định 0
                .reviewCount(0) // Mặc định 0
                .build();
        
        // 7. Lưu Product
        Product savedProduct = productRepository.save(product);
        log.info("Đã tạo product với ID: {}", savedProduct.getId());
        
        // 8. ⭐ Tạo ProductImages CHO PRODUCT (không phải cho variants)
        List<ProductImage> productImages = createProductImages(request.getImages(), request.getMainImageUrl(), savedProduct);
        
        // 9. Tạo ProductVariants (không có images riêng)
        List<ProductVariant> variants = createProductVariants(request.getVariants(), savedProduct);
        
        // 10. Set relationships
        savedProduct.setProductImages(productImages);
        savedProduct.setProductVariants(variants);
        
        // 11. Sync to Recombee (async - không chặn luồng chính)
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
        
        // 12. Return response
        return buildProductResponse(savedProduct, variants);
    }
    
    private List<ProductVariant> createProductVariants(List<CreateProductRequest.CreateProductVariantRequest> variantRequests, 
                                                      Product product) {
        List<ProductVariant> variants = new ArrayList<>();
        
        // Validate chỉ có 1 default variant
        long defaultCount = variantRequests.stream()
                .mapToLong(v -> Boolean.TRUE.equals(v.getIsDefault()) ? 1 : 0)
                .sum();
        
        if (defaultCount > 1) {
            throw new BusinessException(ErrorCode.MULTIPLE_DEFAULT_VARIANTS);
        }
        
        // Nếu không có default variant, set variant đầu tiên làm default
        if (defaultCount == 0 && !variantRequests.isEmpty()) {
            variantRequests.get(0).setIsDefault(true);
        }
        
        for (CreateProductRequest.CreateProductVariantRequest variantRequest : variantRequests) {
            // Generate SKU nếu không có
            String sku = variantRequest.getSku();
            if (sku == null || sku.trim().isEmpty()) {
                sku = generateSku(product.getName(), variantRequest.getName());
            }
            
            // ⚠️ Cho phép SKU trùng - không check uniqueness
            // SKU có thể giống nhau cho các variants khác nhau
            
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .name(variantRequest.getName().trim())
                    .sku(sku)
                    .price(variantRequest.getPrice())
                    .discountPercentage(variantRequest.getDiscountPercentage())
                    .stockQuantity(variantRequest.getStockQuantity())
                    .unit(variantRequest.getUnit().trim())
                    .isActive(variantRequest.getIsActive())
                    .isDefault(variantRequest.getIsDefault())
                    .build();
            
            variants.add(variant);
        }
        
        return productVariantRepository.saveAll(variants);
    }
    
    /**
     * ⭐ Tạo Product Images - liên kết với Product (không phải Variant)
     */
    private List<ProductImage> createProductImages(
            List<CreateProductRequest.CreateProductImageRequest> imageRequests,
            String mainImageUrl,
            Product product) {
        
        log.info("🖼️ Creating product images - mainImageUrl: {}, imageRequests count: {}", 
                mainImageUrl, imageRequests != null ? imageRequests.size() : 0);
        
        List<ProductImage> productImages = new ArrayList<>();
        
        if (imageRequests == null || imageRequests.isEmpty()) {
            log.warn("⚠️ Không có images nào được cung cấp cho product: {}", product.getId());
            return productImages;
        }
        
        // Sort images by displayOrder
        imageRequests.sort((a, b) -> Integer.compare(a.getDisplayOrder(), b.getDisplayOrder()));
        
        boolean hasMainImage = false;
        for (CreateProductRequest.CreateProductImageRequest imageRequest : imageRequests) {
            log.debug("   Processing image: {} (displayOrder: {})", 
                    imageRequest.getMediaUrl(), imageRequest.getDisplayOrder());
            
            // Đánh dấu ảnh chính dựa vào mainImageUrl hoặc ảnh IMAGE đầu tiên
            boolean isMainImage = false;
            if (mainImageUrl != null && mainImageUrl.equals(imageRequest.getMediaUrl())) {
                isMainImage = true;
                hasMainImage = true;
                log.info("   ✅ Matched mainImageUrl: {}", mainImageUrl);
            } else if (!hasMainImage && "IMAGE".equalsIgnoreCase(imageRequest.getMediaType())) {
                isMainImage = true;
                hasMainImage = true;
                log.info("   ✅ First IMAGE set as main: {}", imageRequest.getMediaUrl());
            }
            
            ProductImage image = ProductImage.builder()
                    .product(product) // ⭐ Liên kết với Product
                    .mediaType(MediaType.valueOf(imageRequest.getMediaType().toUpperCase()))
                    .mediaUrl(imageRequest.getMediaUrl())
                    .displayOrder(imageRequest.getDisplayOrder())
                    .isMain(isMainImage)
                    .build();
            
            productImages.add(image);
        }
        
        if (!productImages.isEmpty()) {
            List<ProductImage> savedImages = productImageRepository.saveAll(productImages);
            
            // ⭐ Sync mainImageUrl vào Product để truy xuất nhanh (cached field)
            String finalMainUrl = savedImages.stream()
                    .filter(ProductImage::getIsMain)
                    .findFirst()
                    .map(ProductImage::getMediaUrl)
                    .orElseGet(() -> savedImages.get(0).getMediaUrl());
            
            product.setMainImageUrl(finalMainUrl);
            productRepository.save(product);
            
            log.info("✅ Đã tạo {} ảnh cho product: {} với main image: {}", 
                    savedImages.size(), product.getId(), finalMainUrl);
            
            return savedImages;
        }
        
        return productImages;
    }
    
    private String generateSku(String productName, String variantName) {
        // Generate SKU từ product name + variant name
        String sku = (productName + "-" + variantName)
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        
        // Thêm random suffix để đảm bảo unique
        return sku + "-" + System.currentTimeMillis() % 10000;
    }
    
    private ProductResponse buildProductResponse(Product product, List<ProductVariant> variants) {
    // Delegate mapping to ProductMapper which computes min/max per variant and product
    return ProductMapper.toProductResponse(product, variants, productImageRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID productId) {
        log.info("Lấy chi tiết sản phẩm với ID: {}", productId);
        
        // Tìm product với các quan hệ đã load sẵn (tối ưu N+1 query)
        Product product = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm với ID: {}", productId);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        
        // Lấy tất cả variants của product (đã có index tối ưu)
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIsDefaultDescNameAsc(productId);
        
        return buildProductResponse(product, variants);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductByIdWithFavoriteStatus(UUID productId, UUID userId) {
        log.info("Lấy chi tiết sản phẩm với ID: {} và trạng thái yêu thích cho user: {}", productId, userId);
        
        // Tìm product với các quan hệ đã load sẵn
        Product product = productRepository.findByIdWithDetails(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm với ID: {}", productId);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        
        // Lấy tất cả variants của product
        List<ProductVariant> variants = productVariantRepository.findByProductIdOrderByIsDefaultDescNameAsc(productId);
        
        // Build response
        ProductResponse response = buildProductResponse(product, variants);
        
        // Set trạng thái yêu thích nếu có userId
        if (userId != null) {
            boolean isFavorited = favoriteRepository.existsByUserIdAndProductId(userId, productId);
            response.setIsFavorited(isFavorited);
            log.debug("Product {} isFavorited: {} for user: {}", productId, isFavorited, userId);
        } else {
            response.setIsFavorited(null);
            log.debug("Product {} isFavorited: null (no userId provided)", productId);
        }
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public ProductResponse getActiveProductById(UUID productId) {
        log.info("Lấy chi tiết sản phẩm active với ID: {}", productId);
        
        // Tìm product active với các quan hệ đã load sẵn
        Product product = productRepository.findActiveByIdWithDetails(productId)
                .orElseThrow(() -> {
                    log.warn("Không tìm thấy sản phẩm active với ID: {}", productId);
                    return new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
                });
        
        // Lấy variants active của product
        List<ProductVariant> variants = productVariantRepository.findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(productId);
        
        return buildProductResponse(product, variants);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        log.info("Cập nhật sản phẩm với ID: {}", productId);
        return productUpdateService.updateProduct(productId, request);
    }

    @Override
    @Transactional
    public ProductResponse updateProductRating(UUID productId, UpdateProductRatingRequest request) {
        log.info("Cập nhật rating sản phẩm với ID: {}", productId);
        return productUpdateService.updateProductRating(productId, request);
    }
    
    @Override
    @Transactional
    public int fixMainImageUrls() {
        log.info("Bắt đầu fix mainImageUrl - không còn cần thiết vì variants dùng chung ảnh của product");
        // Method này không còn cần thiết vì variants đã dùng chung ảnh của product
        // Giữ lại để tương thích với interface
        return 0;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProducts(ProductFilterRequest filter, Pageable pageable) {
        log.info("Tìm kiếm sản phẩm với filter: {}, page: {}, size: {}", 
                filter, pageable.getPageNumber(), pageable.getPageSize());
        
        // Xây dựng Specification từ filter
        Specification<Product> spec = ProductSpecification.buildSpecification(filter);
        
        // Xây dựng Sort từ sortBy trong filter
        Sort sort = buildSort(filter.getSortBy());
        
        // ⭐ ĐẶC BIỆT: Nếu sort theo giá, cần xử lý riêng vì Product không có field price trực tiếp
        boolean sortByPrice = filter.getSortBy() != null && 
                              (filter.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_ASC ||
                               filter.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_DESC);
        
        if (sortByPrice) {
            // Lấy tất cả kết quả không phân trang, sort trong memory, rồi tạo Page mới
            List<Product> allProducts = productRepository.findAll(spec);
            
            // Convert sang ProductResponse và sort theo giá thấp nhất của variants
            List<ProductResponse> productResponses = allProducts.stream()
                .map(product -> {
                    List<ProductVariant> variants = productVariantRepository
                        .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
                    return ProductMapper.toProductResponse(product, variants, productImageRepository);
                })
                .sorted((p1, p2) -> {
                    BigDecimal price1 = p1.getMinValue() != null ? p1.getMinValue() : BigDecimal.ZERO;
                    BigDecimal price2 = p2.getMinValue() != null ? p2.getMinValue() : BigDecimal.ZERO;
                    
                    if (filter.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_ASC) {
                        return price1.compareTo(price2);
                    } else {
                        return price2.compareTo(price1);
                    }
                })
                .toList();
            
            // Tạo Page thủ công từ List đã sort
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), productResponses.size());
            List<ProductResponse> pageContent = start >= productResponses.size() ? 
                List.of() : productResponses.subList(start, end);
            
            return new org.springframework.data.domain.PageImpl<>(
                pageContent, 
                pageable, 
                productResponses.size()
            );
        } else {
            // Sort thông thường (không phải giá) - dùng database sort
            Pageable pageableWithSort = PageRequest.of(
                pageable.getPageNumber(), 
                pageable.getPageSize(), 
                sort
            );
            
            // Tìm kiếm sản phẩm
            Page<Product> productPage = productRepository.findAll(spec, pageableWithSort);
            
            // Chuyển đổi Page<Product> sang Page<ProductResponse>
            return productPage.map(product -> {
                List<ProductVariant> variants = productVariantRepository
                    .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
                return ProductMapper.toProductResponse(product, variants, productImageRepository);
            });
        }
    }
    
    /**
     * Xây dựng Sort từ sortBy enum
     * Lưu ý: PRICE_ASC và PRICE_DESC được xử lý riêng trong searchProducts()
     */
    private Sort buildSort(com.greenconnect.greenconnect_api.enums.ProductSortType sortBy) {
        if (sortBy == null) {
            // Mặc định sắp xếp theo createdAt giảm dần (mới nhất trước)
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        return switch (sortBy) {
            case PRICE_ASC, PRICE_DESC -> Sort.by(Sort.Direction.DESC, "createdAt"); // Fallback, sẽ sort trong memory
            case NAME_ASC -> Sort.by(Sort.Direction.ASC, "name");
            case NAME_DESC -> Sort.by(Sort.Direction.DESC, "name");
            case NEWEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case OLDEST -> Sort.by(Sort.Direction.ASC, "createdAt");
            case RATING_ASC -> Sort.by(Sort.Direction.ASC, "averageRating");
            case RATING_DESC -> Sort.by(Sort.Direction.DESC, "averageRating");
        };
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProductsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách sản phẩm active với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("isActive"), true);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            return ProductMapper.toProductResponse(product, variants, productImageRepository);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getInactiveProductsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách sản phẩm inactive với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("isActive"), false);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            return ProductMapper.toProductResponse(product, variants, productImageRepository);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedProductsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách sản phẩm featured với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Product> spec = (root, query, cb) -> cb.equal(root.get("isFeatured"), true);
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            return ProductMapper.toProductResponse(product, variants, productImageRepository);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProductsPaginatedByCreatedAt(int page, int size) {
        log.info("Lấy danh sách tất cả sản phẩm với phân trang - page: {}, size: {}", page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<Product> productPage = productRepository.findAll(pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            return ProductMapper.toProductResponse(product, variants, productImageRepository);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> searchProductsByTab(String keyword, String tab, int page, int size) {
        log.info("🔍 Tìm kiếm sản phẩm - Từ khóa: '{}', Tab: '{}', Page: {}, Size: {}", keyword, tab, page, size);
        Pageable pageable = PageRequest.of(page, size);
        
        Specification<Product> spec = (root, query, cb) -> {
            // Tìm kiếm trong tên và description
            var keywordPredicate = cb.or(
                cb.like(cb.lower(root.get("name")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("description")), "%" + keyword.toLowerCase() + "%")
            );
            
            // Filter theo tab
            return switch (tab.toLowerCase()) {
                case "active" -> {
                    log.info("✅ Tìm kiếm trong sản phẩm ACTIVE");
                    yield cb.and(keywordPredicate, cb.equal(root.get("isActive"), true));
                }
                case "inactive" -> {
                    log.info("✅ Tìm kiếm trong sản phẩm INACTIVE");
                    yield cb.and(keywordPredicate, cb.equal(root.get("isActive"), false));
                }
                case "featured" -> {
                    log.info("✅ Tìm kiếm trong sản phẩm FEATURED");
                    yield cb.and(keywordPredicate, cb.equal(root.get("isFeatured"), true));
                }
                case "all" -> {
                    log.info("✅ Tìm kiếm trong TẤT CẢ sản phẩm");
                    yield keywordPredicate;
                }
                default -> {
                    log.warn("⚠️ Tab không hợp lệ: '{}' - Sử dụng 'all' mặc định", tab);
                    yield keywordPredicate;
                }
            };
        };
        
        Page<Product> productPage = productRepository.findAll(spec, pageable);
        log.info("✅ Tìm thấy {} sản phẩm", productPage.getTotalElements());
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdOrderByIsDefaultDescNameAsc(product.getId());
            return ProductMapper.toProductResponse(product, variants, productImageRepository);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getLatestActiveProducts(int page, int size) {
        log.info("🆕 Lấy sản phẩm mới nhất (active) - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findLatestActiveProducts(pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
            return buildProductResponse(product, variants);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getBestSellingActiveProducts(int page, int size) {
        log.info("🔥 Lấy sản phẩm bán chạy (active) - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findBestSellingActiveProducts(pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
            return buildProductResponse(product, variants);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFlashSaleProducts(int page, int size) {
        log.info("⚡ Lấy sản phẩm khuyến mãi (FLASH_SALE) - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findFlashSaleProducts(pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
            return buildProductResponse(product, variants);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedActiveProducts(int page, int size) {
        log.info("⭐ Lấy sản phẩm nổi bật (active + featured) - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findFeaturedActiveProducts(pageable);
        
        return productPage.map(product -> {
            List<ProductVariant> variants = productVariantRepository
                .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
            return buildProductResponse(product, variants);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductWithTopReviewResponse> getTopRatedProductsWithTopReview(int page, int size) {
        log.info("🏆 Lấy sản phẩm được xếp hạng cao nhất kèm đánh giá hàng đầu - page: {}, size: {}", page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findTopRatedProductsWithTopReview(pageable);
        
        return productPage.map(this::mapToProductWithTopReviewResponse);
    }
    
    /**
     * Chuyển đổi Product sang ProductWithTopReviewResponse
     * Bao gồm: sản phẩm details + top review + images + user info
     */
    private ProductWithTopReviewResponse mapToProductWithTopReviewResponse(Product product) {
        // Lấy variants active của sản phẩm
        List<ProductVariant> variants = productVariantRepository
            .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
        
        // Lấy base product response
        ProductResponse baseResponse = buildProductResponse(product, variants);
        
        // Chuyển đổi sang ProductWithTopReviewResponse (copy tất cả field từ ProductResponse)
        ProductWithTopReviewResponse response = ProductWithTopReviewResponse.builder()
            .id(baseResponse.getId())
            .name(baseResponse.getName())
            .description(baseResponse.getDescription())
            .slug(baseResponse.getSlug())
            .isActive(baseResponse.getIsActive())
            .isFeatured(baseResponse.getIsFeatured())
            .averageRating(baseResponse.getAverageRating())
            .reviewCount(baseResponse.getReviewCount())
            .sellNumber(baseResponse.getSellNumber()) // ⚡ Số lượng đã bán
            .createdAt(baseResponse.getCreatedAt())
            .updatedAt(baseResponse.getUpdatedAt())
            .minValue(baseResponse.getMinValue())
            .maxValue(baseResponse.getMaxValue())
            .isFavorited(baseResponse.getIsFavorited())
            .build();
        
        // Copy category
        if (baseResponse.getCategory() != null) {
            response.setCategory(
                ProductWithTopReviewResponse.CategoryInfo.builder()
                    .id(baseResponse.getCategory().getId())
                    .name(baseResponse.getCategory().getName())
                    .imageUrl(baseResponse.getCategory().getImageUrl())
                    .build()
            );
        }
        
        // Copy supplier
        if (baseResponse.getSupplier() != null) {
            response.setSupplier(
                ProductWithTopReviewResponse.SupplierInfo.builder()
                    .id(baseResponse.getSupplier().getId())
                    .name(baseResponse.getSupplier().getName())
                    .contactEmail(baseResponse.getSupplier().getContactEmail())
                    .phoneNumber(baseResponse.getSupplier().getPhoneNumber())
                    .build()
            );
        }
        
        // Copy mainImageUrl và images
        response.setMainImageUrl(baseResponse.getMainImageUrl());
        if (baseResponse.getImages() != null) {
            response.setImages(baseResponse.getImages().stream()
                .map(img -> ProductWithTopReviewResponse.ProductImageInfo.builder()
                    .id(img.getId())
                    .mediaType(img.getMediaType())
                    .mediaUrl(img.getMediaUrl())
                    .displayOrder(img.getDisplayOrder())
                    .isMain(img.getIsMain())
                    .build())
                .toList());
        }
        
        // Copy variants
        if (baseResponse.getVariants() != null) {
            response.setVariants(baseResponse.getVariants().stream()
                .map(var -> ProductWithTopReviewResponse.ProductVariantInfo.builder()
                    .id(var.getId())
                    .name(var.getName())
                    .sku(var.getSku())
                    .price(var.getPrice())
                    .discountPercentage(var.getDiscountPercentage())
                    .discountedPrice(var.getDiscountedPrice())
                    .minValue(var.getMinValue())
                    .maxValue(var.getMaxValue())
                    .discountAmount(var.getDiscountAmount())
                    .stockQuantity(var.getStockQuantity())
                    .unit(var.getUnit())
                    .isActive(var.getIsActive())
                    .isDefault(var.getIsDefault())
                    .build())
                .toList());
        }
        
        // Lấy top review (highest rated) của sản phẩm
        // Query đã được tối ưu để tính sẵn và lazy load
        if (product.getProductReviews() != null && !product.getProductReviews().isEmpty()) {
            // Product reviews từ query đã được fetch, lấy cái đầu tiên (top 1)
            // Ưu tiên: có media (ảnh/video) > không có media, sau đó mới đến rating
            // VD: 4⭐ + media > 5⭐ không có gì
            ProductReview topReview = product.getProductReviews().stream()
                .filter(pr -> pr.getIsApproved() != null && pr.getIsApproved())
                .min((r1, r2) -> {
                    // Kiểm tra review có media (ảnh/video) không - ƯU TIÊN CAO NHẤT
                    boolean r1HasMedia = r1.getReviewMedia() != null && !r1.getReviewMedia().isEmpty();
                    boolean r2HasMedia = r2.getReviewMedia() != null && !r2.getReviewMedia().isEmpty();
                    
                    // Ưu tiên review có media trước (quan trọng nhất)
                    if (r1HasMedia && !r2HasMedia) return -1;
                    if (!r1HasMedia && r2HasMedia) return 1;
                    
                    // Cùng có media hoặc cùng không có media -> so sánh rating DESC
                    int ratingCompare = Short.compare(r2.getRating(), r1.getRating());
                    if (ratingCompare != 0) return ratingCompare;
                    
                    // Cùng rating, ưu tiên review có text (comment)
                    boolean r1HasText = r1.getComment() != null && !r1.getComment().trim().isEmpty();
                    boolean r2HasText = r2.getComment() != null && !r2.getComment().trim().isEmpty();
                    if (r1HasText && !r2HasText) return -1;
                    if (!r1HasText && r2HasText) return 1;
                    
                    // Cùng điều kiện, ưu tiên review mới hơn
                    return r2.getReviewTime().compareTo(r1.getReviewTime());
                })
                .orElse(null);
            
            if (topReview != null) {
                // Lấy top review kể cả không có media
                List<ProductWithTopReviewResponse.ReviewMediaInfo> mediaList = null;
                if (topReview.getReviewMedia() != null && !topReview.getReviewMedia().isEmpty()) {
                    mediaList = topReview.getReviewMedia().stream()
                        .map(media -> ProductWithTopReviewResponse.ReviewMediaInfo.builder()
                            .id(media.getId())
                            .mediaType(media.getMediaType() != null ? media.getMediaType().toString() : null)
                            .mediaUrl(media.getMediaUrl())
                            .displayOrder(media.getDisplayOrder())
                            .build())
                        .sorted((m1, m2) -> Integer.compare(m1.getDisplayOrder(), m2.getDisplayOrder()))
                        .toList();
                }
                
                response.setTopReview(
                    ProductWithTopReviewResponse.TopReviewInfo.builder()
                        .id(topReview.getId())
                        .rating(topReview.getRating())
                        .comment(topReview.getComment())
                        .reviewTime(topReview.getReviewTime())
                        .user(topReview.getUser() != null ?
                            ProductWithTopReviewResponse.UserMinimalInfo.builder()
                                .userId(topReview.getUser().getId())
                                .fullName(topReview.getUser().getFullName())
                                .avatarUrl(topReview.getUser().getAvatarUrl())
                                .build()
                            : null)
                        .mediaList(mediaList)
                        .shopReply(null) // TODO: Thêm shopReply nếu cần
                        .build()
                );
            }
        }
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> filterProducts(com.greenconnect.greenconnect_api.dtos.request.FilterProductsRequest request) {
        log.info("🔍 Lọc sản phẩm - Page: {}, Size: {}, SortBy: {}, campaignSlug: {}, isNew: {}, isBestSeller: {}, isOnSale: {}, isFeatured: {}, isHighRating: {}, categoryIds: {}, supplierIds: {}, priceRange: {}, isActive: {}", 
                request.getPage(), request.getSize(), request.getSortBy(),
                request.getCampaignSlug(), request.getIsNew(), request.getIsBestSeller(), 
                request.getIsOnSale(), request.getIsFeatured(), request.getIsHighRating(),
                request.getCategoryIds(), request.getSupplierIds(), request.getPriceRange(),
                request.getIsActive());
        
        // ⭐ Build Specification kết hợp TẤT CẢ các filter
        org.springframework.data.jpa.domain.Specification<Product> spec = 
            com.greenconnect.greenconnect_api.specifications.FilterProductSpecification.buildSpecification(request);
        
        // ⭐ Build Sort từ sortBy enum (giống GET /products)
        Sort sort = buildSort(request.getSortBy());
        
        // ⭐ ĐẶC BIỆT: Nếu sort theo giá (PRICE_ASC/PRICE_DESC), cần xử lý riêng
        boolean sortByPrice = request.getSortBy() != null && 
                              (request.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_ASC ||
                               request.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_DESC);
        
        if (sortByPrice) {
            // Lấy tất cả kết quả không phân trang, sort trong memory theo giá, rồi tạo Page mới
            log.info("💰 Sort theo giá - Cần load tất cả để sort theo minValue của variants");
            List<Product> allProducts = productRepository.findAll(spec);
            
            // Convert sang ProductResponse và sort theo giá thấp nhất của variants
            List<ProductResponse> productResponses = allProducts.stream()
                .map(product -> {
                    List<ProductVariant> variants = productVariantRepository
                        .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
                    return buildProductResponse(product, variants);
                })
                .sorted((p1, p2) -> {
                    BigDecimal price1 = p1.getMinValue() != null ? p1.getMinValue() : BigDecimal.ZERO;
                    BigDecimal price2 = p2.getMinValue() != null ? p2.getMinValue() : BigDecimal.ZERO;
                    
                    if (request.getSortBy() == com.greenconnect.greenconnect_api.enums.ProductSortType.PRICE_ASC) {
                        return price1.compareTo(price2);
                    } else {
                        return price2.compareTo(price1);
                    }
                })
                .toList();
            
            // Tạo Page thủ công từ List đã sort
            int start = request.getPage() * request.getSize();
            int end = Math.min(start + request.getSize(), productResponses.size());
            List<ProductResponse> pageContent = start >= productResponses.size() ? 
                List.of() : productResponses.subList(start, end);
            
            return new org.springframework.data.domain.PageImpl<>(
                pageContent, 
                PageRequest.of(request.getPage(), request.getSize()), 
                productResponses.size()
            );
        }
        
        // ⭐ Xử lý Pageable và Sorting cho các trường hợp khác (không sort theo giá)
        Pageable effectivePageable;
        boolean needsManualPagination = false;
        
        // 1. campaignSlug - query trực tiếp, có thể paginate
        if (request.getCampaignSlug() != null && !request.getCampaignSlug().isEmpty()) {
            log.info("📦 Lọc theo campaign slug: {}", request.getCampaignSlug());
            effectivePageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        }
        // 2. isOnSale (FLASH_SALE) - query trực tiếp, có thể paginate
        else if (request.getIsOnSale() != null && request.getIsOnSale()) {
            log.info("⚡ Lọc sản phẩm khuyến mãi (FLASH_SALE)");
            effectivePageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        }
        // 3. isFeatured - query trực tiếp, có thể paginate
        else if (request.getIsFeatured() != null && request.getIsFeatured()) {
            log.info("⭐ Lọc sản phẩm nổi bật");
            effectivePageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        }
        // 4. isHighRating - lấy tất cả rồi sort theo rating giảm dần
        else if (request.getIsHighRating() != null && request.getIsHighRating()) {
            log.info("🌟 Lọc sản phẩm theo đánh giá (rating cao → thấp)");
            // ✅ FIX: Bỏ qua sortBy=NEWEST, chỉ áp dụng sort thủ công (PRICE_ASC, NAME_DESC...)
            Sort effectiveSort;
            if (request.getSortBy() != null && 
                request.getSortBy() != com.greenconnect.greenconnect_api.enums.ProductSortType.NEWEST) {
                // User chọn sort thủ công (không phải NEWEST) → Ưu tiên sort của user
                effectiveSort = sort;
                log.info("👤 User chọn sort: {} (override default averageRating DESC)", request.getSortBy());
            } else {
                // sortBy = null HOẶC sortBy = NEWEST → Dùng sort mặc định
                effectiveSort = Sort.by(Sort.Direction.DESC, "averageRating");
                log.info("⚙️ Dùng sort mặc định: averageRating DESC");
            }
            effectivePageable = PageRequest.of(0, Integer.MAX_VALUE, effectiveSort);
            needsManualPagination = true;
        }
        // 5. isNew - cần lấy TẤT CẢ để sort theo createdAt DESC
        else if (request.getIsNew() != null && request.getIsNew()) {
            log.info("🆕 Lọc sản phẩm mới nhất");
            // Mặc định createdAt DESC nếu không có sortBy
            Sort effectiveSort = request.getSortBy() != null 
                ? sort 
                : Sort.by(Sort.Direction.DESC, "createdAt");
            effectivePageable = PageRequest.of(0, Integer.MAX_VALUE, effectiveSort);
            needsManualPagination = true;
        }
        // 6. isBestSeller - lấy tất cả rồi sort theo sellNumber (số lượng đã bán)
        else if (request.getIsBestSeller() != null && request.getIsBestSeller()) {
            log.info("🔥 Lọc sản phẩm bán chạy nhất");
            // ✅ FIX: Bỏ qua sortBy=NEWEST, chỉ áp dụng sort thủ công (PRICE_ASC, NAME_DESC...)
            Sort effectiveSort;
            if (request.getSortBy() != null && 
                request.getSortBy() != com.greenconnect.greenconnect_api.enums.ProductSortType.NEWEST) {
                // User chọn sort thủ công (không phải NEWEST) → Ưu tiên sort của user
                effectiveSort = sort;
                log.info("👤 User chọn sort: {} (override default sellNumber DESC)", request.getSortBy());
            } else {
                // sortBy = null HOẶC sortBy = NEWEST → Dùng sort mặc định
                effectiveSort = Sort.by(Sort.Direction.DESC, "sellNumber");
                log.info("⚙️ Dùng sort mặc định: sellNumber DESC");
            }
            effectivePageable = PageRequest.of(0, Integer.MAX_VALUE, effectiveSort);
            needsManualPagination = true;
        }
        // 7. Trường hợp mặc định - lọc theo categoryIds/supplierIds/priceRange
        else {
            log.info("📋 Lọc sản phẩm thông thường");
            effectivePageable = PageRequest.of(request.getPage(), request.getSize(), sort);
        }
        
        // ⭐ Query với Specification (đã kết hợp tất cả filter)
        Page<Product> productPage = productRepository.findAll(spec, effectivePageable);
        log.info("📊 Tìm thấy {} sản phẩm sau khi lọc", productPage.getTotalElements());
        
        // Map Product → ProductResponse
        List<ProductResponse> productResponses = productPage.getContent().stream()
            .map(product -> {
                List<ProductVariant> variants = productVariantRepository
                    .findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(product.getId());
                return buildProductResponse(product, variants);
            })
            .toList();
        
        // ⭐ Manual pagination nếu cần (isNew, isBestSeller, isHighRating)
        if (needsManualPagination) {
            int start = request.getPage() * request.getSize();
            int end = Math.min(start + request.getSize(), productResponses.size());
            
            List<ProductResponse> pageContent = start >= productResponses.size() 
                ? List.of() 
                : productResponses.subList(start, end);
            
            return new org.springframework.data.domain.PageImpl<>(
                pageContent, 
                PageRequest.of(request.getPage(), request.getSize()), 
                productResponses.size()
            );
        }
        
        // ⭐ Trả về Page bình thường
        return new org.springframework.data.domain.PageImpl<>(
            productResponses, 
            PageRequest.of(request.getPage(), request.getSize()), 
            productPage.getTotalElements()
        );
    }
    
    /**
     * Sắp xếp danh sách sản phẩm theo ProductSortType
     */
    private List<ProductResponse> sortProducts(List<ProductResponse> products, com.greenconnect.greenconnect_api.enums.ProductSortType sortBy) {
        return switch (sortBy) {
            case PRICE_ASC -> products.stream()
                .sorted((p1, p2) -> {
                    BigDecimal price1 = p1.getMinValue() != null ? p1.getMinValue() : BigDecimal.ZERO;
                    BigDecimal price2 = p2.getMinValue() != null ? p2.getMinValue() : BigDecimal.ZERO;
                    return price1.compareTo(price2);
                })
                .toList();
            
            case PRICE_DESC -> products.stream()
                .sorted((p1, p2) -> {
                    BigDecimal price1 = p1.getMinValue() != null ? p1.getMinValue() : BigDecimal.ZERO;
                    BigDecimal price2 = p2.getMinValue() != null ? p2.getMinValue() : BigDecimal.ZERO;
                    return price2.compareTo(price1);
                })
                .toList();
            
            case NAME_ASC -> products.stream()
                .sorted((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()))
                .toList();
            
            case NAME_DESC -> products.stream()
                .sorted((p1, p2) -> p2.getName().compareToIgnoreCase(p1.getName()))
                .toList();
            
            case NEWEST -> products.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .toList();
            
            case OLDEST -> products.stream()
                .sorted((p1, p2) -> p1.getCreatedAt().compareTo(p2.getCreatedAt()))
                .toList();
            
            case RATING_ASC -> products.stream()
                .sorted((p1, p2) -> {
                    BigDecimal rating1 = p1.getAverageRating() != null ? p1.getAverageRating() : BigDecimal.ZERO;
                    BigDecimal rating2 = p2.getAverageRating() != null ? p2.getAverageRating() : BigDecimal.ZERO;
                    return rating1.compareTo(rating2);
                })
                .toList();
            
            case RATING_DESC -> products.stream()
                .sorted((p1, p2) -> {
                    BigDecimal rating1 = p1.getAverageRating() != null ? p1.getAverageRating() : BigDecimal.ZERO;
                    BigDecimal rating2 = p2.getAverageRating() != null ? p2.getAverageRating() : BigDecimal.ZERO;
                    return rating2.compareTo(rating1);
                })
                .toList();
        };
    }
    
    @Override
    @Transactional
    public BatchPriceUpdateResponse batchUpdatePrices(BatchPriceUpdateRequest request) {
        log.info("💰 Bắt đầu cập nhật giá hàng loạt cho {} biến thể", request.getUpdates().size());
        
        List<BatchPriceUpdateResponse.FailureDetail> failures = new ArrayList<>();
        int successCount = 0;
        
        for (BatchPriceUpdateRequest.VariantPriceUpdate update : request.getUpdates()) {
            try {
                // Validate: Ít nhất một trong price hoặc discountPercentage phải được truyền
                if (update.getPrice() == null && update.getDiscountPercentage() == null) {
                    failures.add(BatchPriceUpdateResponse.FailureDetail.builder()
                            .variantId(update.getVariantId())
                            .productId(update.getProductId())
                            .reason("Phải cung cấp ít nhất price hoặc discountPercentage")
                            .build());
                    continue;
                }
                
                // Tìm variant theo ID
                ProductVariant variant = productVariantRepository.findById(update.getVariantId())
                        .orElse(null);
                
                if (variant == null) {
                    failures.add(BatchPriceUpdateResponse.FailureDetail.builder()
                            .variantId(update.getVariantId())
                            .productId(update.getProductId())
                            .reason("Variant not found")
                            .build());
                    continue;
                }
                
                // Validate variant thuộc đúng product
                if (!variant.getProduct().getId().equals(update.getProductId())) {
                    failures.add(BatchPriceUpdateResponse.FailureDetail.builder()
                            .variantId(update.getVariantId())
                            .productId(update.getProductId())
                            .reason("Variant không thuộc product được chỉ định")
                            .build());
                    continue;
                }
                
                // Cập nhật price nếu có
                if (update.getPrice() != null) {
                    variant.setPrice(update.getPrice());
                    log.debug("   Cập nhật price cho variant {}: {}", update.getVariantId(), update.getPrice());
                }
                
                // Cập nhật discountPercentage nếu có
                if (update.getDiscountPercentage() != null) {
                    variant.setDiscountPercentage(update.getDiscountPercentage());
                    log.debug("   Cập nhật discountPercentage cho variant {}: {}%", 
                            update.getVariantId(), update.getDiscountPercentage());
                }
                
                // Lưu variant
                productVariantRepository.save(variant);
                successCount++;
                
            } catch (Exception e) {
                log.error("❌ Lỗi khi cập nhật variant {}: {}", update.getVariantId(), e.getMessage());
                failures.add(BatchPriceUpdateResponse.FailureDetail.builder()
                        .variantId(update.getVariantId())
                        .productId(update.getProductId())
                        .reason("Internal error: " + e.getMessage())
                        .build());
            }
        }
        
        log.info("✅ Hoàn tất cập nhật giá: {} thành công, {} thất bại", successCount, failures.size());
        
        return BatchPriceUpdateResponse.builder()
                .totalUpdated(request.getUpdates().size())
                .successCount(successCount)
                .failedCount(failures.size())
                .failures(failures.isEmpty() ? null : failures)
                .build();
    }
}
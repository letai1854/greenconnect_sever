package com.greenconnect.greenconnect_api.services;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.greenconnect.greenconnect_api.dtos.request.BatchPriceUpdateRequest;
import com.greenconnect.greenconnect_api.dtos.request.CreateProductRequest;
import com.greenconnect.greenconnect_api.dtos.request.FilterProductsRequest;
import com.greenconnect.greenconnect_api.dtos.request.ProductFilterRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRatingRequest;
import com.greenconnect.greenconnect_api.dtos.request.UpdateProductRequest;
import com.greenconnect.greenconnect_api.dtos.response.BatchPriceUpdateResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductResponse;
import com.greenconnect.greenconnect_api.dtos.response.ProductWithTopReviewResponse;

public interface ProductService {
    
    /**
     * Tạo sản phẩm mới với variants và images
     */
    ProductResponse createProduct(CreateProductRequest request);
    
    /**
     * Lấy chi tiết thông tin sản phẩm theo ID
     */
    ProductResponse getProductById(UUID productId);
    
    /**
     * Lấy chi tiết thông tin sản phẩm theo ID với trạng thái yêu thích của user
     * @param productId ID của sản phẩm
     * @param userId ID của user (có thể null nếu chưa đăng nhập)
     * @return ProductResponse với trường isFavorited
     */
    ProductResponse getProductByIdWithFavoriteStatus(UUID productId, UUID userId);
    
    /**
     * Lấy chi tiết sản phẩm active cho public
     */
    ProductResponse getActiveProductById(UUID productId);
    
    /**
     * Cập nhật thông tin sản phẩm (delegated to ProductUpdateService)
     */
    ProductResponse updateProduct(UUID productId, UpdateProductRequest request);
    
    /**
     * Cập nhật rating và review count của sản phẩm
     */
    ProductResponse updateProductRating(UUID productId, UpdateProductRatingRequest request);
    
    /**
     * Fix mainImageUrl for existing variants from their first image
     */
    int fixMainImageUrls();

    /**
     * Lấy danh sách sản phẩm với phân trang và lọc
     * @param filter Tiêu chí lọc sản phẩm
     * @param pageable Thông tin phân trang
     * @return Page chứa danh sách ProductResponse
     */
    Page<ProductResponse> searchProducts(ProductFilterRequest filter, Pageable pageable);
    
    /**
     * Lấy danh sách sản phẩm active với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<ProductResponse> getActiveProductsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy danh sách sản phẩm inactive với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<ProductResponse> getInactiveProductsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy danh sách sản phẩm featured với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<ProductResponse> getFeaturedProductsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Lấy tất cả sản phẩm với phân trang sắp xếp theo thời gian tạo (Admin)
     */
    Page<ProductResponse> getAllProductsPaginatedByCreatedAt(int page, int size);
    
    /**
     * Tìm kiếm sản phẩm với phân trang theo từ khóa và tab (active/inactive/featured/all)
     */
    Page<ProductResponse> searchProductsByTab(String keyword, String tab, int page, int size);
    
    /**
     * Lấy sản phẩm mới nhất (active) với phân trang - Customer
     */
    Page<ProductResponse> getLatestActiveProducts(int page, int size);
    
    /**
     * Lấy sản phẩm bán chạy (active) với phân trang - Customer
     */
    Page<ProductResponse> getBestSellingActiveProducts(int page, int size);
    
    /**
     * Lấy sản phẩm khuyến mãi (trong campaign FLASH_SALE active) với phân trang - Customer
     */
    Page<ProductResponse> getFlashSaleProducts(int page, int size);
    
    /**
     * Lấy sản phẩm nổi bật (active + featured) với phân trang - Customer
     */
    Page<ProductResponse> getFeaturedActiveProducts(int page, int size);
    
    /**
     * Lấy sản phẩm có đánh giá cao nhất (sorted by averageRating DESC)
     * Mỗi sản phẩm bao gồm đánh giá cao nhất (highest rated) có hình ảnh + thông tin người dùng
     * - Customer public endpoint
     */
    Page<ProductWithTopReviewResponse> getTopRatedProductsWithTopReview(int page, int size);
    
    /**
     * Lọc sản phẩm theo nhiều tiêu chí
     * - Nếu campaignSlug không null: lấy sản phẩm từ campaign đó
     * - Nếu isNew = true: lấy sản phẩm mới nhất
     * - Nếu isBestSeller = true: lấy sản phẩm bán chạy nhất
     * - Nếu isOnSale = true: lấy sản phẩm flash sale
     * - Nếu isFeatured = true: lấy sản phẩm nổi bật
     * - Nếu isHighRating = true: lấy sản phẩm đánh giá cao
     * - Nếu các category/supplier có giá trị: lọc theo đó
     * ✅ Nhận Pageable để tự động parse sort từ query params (đồng bộ với searchProducts)
     */
    Page<ProductResponse> filterProducts(FilterProductsRequest request);
    
    /**
     * Cập nhật giá và % giảm giá hàng loạt cho nhiều biến thể sản phẩm
     * 
     * @param request Danh sách các biến thể cần cập nhật với giá/% giảm giá mới
     * @return BatchPriceUpdateResponse chứa kết quả cập nhật (thành công/thất bại)
     */
    BatchPriceUpdateResponse batchUpdatePrices(BatchPriceUpdateRequest request);
}
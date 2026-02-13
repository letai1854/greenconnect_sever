package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.ProductVariant;

/**
 * Repository interface cho thực thể ProductVariant.
 * 
 * <p>Quản lý các phiên bản (biến thể) cụ thể của một sản phẩm. Đây là một repository
 * cốt lõi, chịu trách nhiệm truy vấn thông tin về giá, số lượng tồn kho và các thuộc tính
 * riêng của từng biến thể.</p>
 */
@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID>, JpaSpecificationExecutor<ProductVariant> {

    /**
     * Kiểm tra SKU đã tồn tại chưa
     */
    boolean existsBySku(String sku);
    
    /**
     * Lấy tất cả variants của một product
     */
    List<ProductVariant> findByProductIdOrderByIsDefaultDescNameAsc(UUID productId);
    
    /**
     * Lấy variants active của một product (cho public API)
     */
    List<ProductVariant> findByProductIdAndIsActiveTrueOrderByIsDefaultDescNameAsc(UUID productId);

    /**
     * Lấy ProductVariant theo ID với eager loading của Product và Product Images.
     * Sử dụng cho cart và order khi cần hiển thị ảnh sản phẩm.
     * <p>Index sử dụng: PRIMARY KEY (id), idx_variant_product_active (product_id, is_active),
     * idx_product_images_product (product_id)</p>
     * 
     * @param id UUID của ProductVariant cần lấy
     * @return Optional chứa ProductVariant với Product và Images đã được load
     */
    @Query("SELECT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.productImages " +
           "WHERE pv.id = :id")
    Optional<ProductVariant> findByIdWithProductAndImages(@Param("id") UUID id);

    /**
     * Lấy danh sách các biến thể có ID nằm trong một danh sách cho trước,
     * với eager loading của Product và Product Images.
     * <p>Cực kỳ hữu ích trong {@code OrderService}. Khi khách hàng đặt hàng, giỏ hàng sẽ
     * chứa một danh sách các {@code productVariantId}. Service sẽ dùng hàm này để lấy ra
     * tất cả thông tin biến thể tương ứng chỉ trong một câu lệnh truy vấn duy nhất,
     * thay vì phải gọi CSDL nhiều lần trong vòng lặp.</p>
     * <p>Index sử dụng: PRIMARY KEY (id), idx_product_images_product (product_id)</p>
     *
     * @param ids Danh sách các UUID của các biến thể cần lấy.
     * @return một danh sách (List) các đối tượng {@link ProductVariant} với Product và Images.
     */
    @Query("SELECT DISTINCT pv FROM ProductVariant pv " +
           "LEFT JOIN FETCH pv.product p " +
           "LEFT JOIN FETCH p.productImages " +
           "WHERE pv.id IN :ids")
    List<ProductVariant> findByIdInWithProductAndImages(@Param("ids") List<UUID> ids);
    
    //  * @return một danh sách (List) các biến thể đang hoạt động.
    //  */
    // List<ProductVariant> findByProductIdAndIsActive(UUID productId, boolean isActive);

    /**
     * Tìm kiếm một biến thể dựa trên mã SKU (Stock Keeping Unit) của nó.
     * <p>SKU là mã định danh duy nhất cho mỗi biến thể. Hàm này hữu ích cho việc quản lý
     * nội bộ, ví dụ như khi nhập hàng hoặc tích hợp với các hệ thống quản lý kho.</p>
     *
     * @param sku Mã SKU cần tìm.
     * @return một đối tượng {@link Optional} chứa {@link ProductVariant} nếu tìm thấy.
     */
    Optional<ProductVariant> findBySku(String sku);

    // /**
    //  * Lấy danh sách các biến thể có ID nằm trong một danh sách cho trước.
    //  * <p>Cực kỳ hữu ích trong {@code OrderService}. Khi khách hàng đặt hàng, giỏ hàng sẽ
    //  * chứa một danh sách các {@code productVariantId}. Service sẽ dùng hàm này để lấy ra
    //  * tất cả thông tin biến thể tương ứng chỉ trong một câu lệnh truy vấn duy nhất,
    //  * thay vì phải gọi CSDL nhiều lần trong vòng lặp.</p>
    //  *
    //  * @param ids Danh sách các UUID của các biến thể cần lấy.
    //  * @return một danh sách (List) các đối tượng {@link ProductVariant}.
    //  */
    // @Query("SELECT pv FROM ProductVariant pv WHERE pv.id IN :ids")
    // List<ProductVariant> findByIdIn(List<UUID> ids);
}
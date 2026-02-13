package com.greenconnect.greenconnect_api.repositories;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Category;

/**
 * Repository interface cho thực thể Category.
 * 
 * <p>Quản lý các danh mục sản phẩm (ví dụ: Rau củ, Trái cây, Thịt cá).
 * Cung cấp các phương thức CRUD cho Admin và các phương thức truy vấn để
 * hiển thị danh mục cho người dùng.</p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, JpaSpecificationExecutor<Category> {

    /**
     * Lấy danh sách tất cả các danh mục đang ở trạng thái hoạt động, sắp xếp theo displayOrder.
     */
    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    /**
     * Lấy danh sách tất cả các danh mục đang ở trạng thái hoạt động với phân trang.
     */
    Page<Category> findByIsActiveTrueOrderByDisplayOrderAsc(Pageable pageable);
    
    /**
     * Lấy danh sách tất cả các danh mục không hoạt động, sắp xếp theo displayOrder.
     */
    List<Category> findByIsActiveFalseOrderByDisplayOrderAsc();
    
    /**
     * Lấy danh sách tất cả các danh mục không hoạt động với phân trang.
     */
    Page<Category> findByIsActiveFalseOrderByDisplayOrderAsc(Pageable pageable);
    
    /**
     * Lấy tất cả danh mục sắp xếp theo displayOrder.
     */
    List<Category> findAllByOrderByDisplayOrderAsc();
    
    /**
     * Lấy tất cả danh mục với phân trang.
     */
    Page<Category> findAllByOrderByDisplayOrderAsc(Pageable pageable);
    
    /**
     * Lấy danh sách tất cả các danh mục đang ở trạng thái hoạt động, sắp xếp theo thời gian tạo giảm dần.
     */
    List<Category> findByIsActiveTrueOrderByCreatedAtDesc();
    
    /**
     * Lấy danh sách tất cả các danh mục không hoạt động, sắp xếp theo thời gian tạo giảm dần.
     */
    List<Category> findByIsActiveFalseOrderByCreatedAtDesc();
    
    /**
     * Lấy tất cả danh mục sắp xếp theo thời gian tạo giảm dần.
     */
    List<Category> findAllByOrderByCreatedAtDesc();
    
    /**
     * Lấy danh sách tất cả các danh mục đang ở trạng thái hoạt động với phân trang, sắp xếp theo thời gian tạo giảm dần.
     */
    Page<Category> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Lấy danh sách tất cả các danh mục không hoạt động với phân trang, sắp xếp theo thời gian tạo giảm dần.
     */
    Page<Category> findByIsActiveFalseOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Lấy tất cả danh mục với phân trang, sắp xếp theo thời gian tạo giảm dần.
     */
    Page<Category> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Tìm danh mục theo tên (chính xác)
     */
    Optional<Category> findByName(String name);
    
    /**
     * Kiểm tra tên danh mục đã tồn tại chưa (cho validation)
     */
    boolean existsByNameIgnoreCase(String name);
    
    /**
     * Kiểm tra tên danh mục đã tồn tại chưa (trừ category hiện tại khi update)
     */
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE LOWER(c.name) = LOWER(:name) AND c.id != :categoryId")
    boolean existsByNameIgnoreCaseAndIdNot(@Param("name") String name, @Param("categoryId") UUID categoryId);
    
    /**
     * Lấy tất cả danh mục với số lượng sản phẩm
     */
    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN c.products p GROUP BY c ORDER BY c.displayOrder ASC")
    List<Object[]> findAllCategoriesWithProductCount();
    
    /**
     * Lấy danh mục active với số lượng sản phẩm
     */
    @Query("SELECT c, COUNT(p) FROM Category c LEFT JOIN c.products p WHERE c.isActive = true GROUP BY c ORDER BY c.displayOrder ASC")
    List<Object[]> findActiveCategoriesWithProductCount();
    
    /**
     * Tìm danh mục theo displayOrder
     */
    Category findByDisplayOrder(Integer displayOrder);
    
    /**
     * Lấy displayOrder lớn nhất hiện tại
     */
    @Query("SELECT MAX(c.displayOrder) FROM Category c")
    Integer findMaxDisplayOrder();
    
    /**
     * Tìm kiếm danh mục active theo tên (tiếng Việt có dấu) với phân trang
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = true AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.displayOrder ASC")
    Page<Category> searchActiveCategoriesByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Tìm kiếm danh mục inactive theo tên (tiếng Việt có dấu) với phân trang
     */
    @Query("SELECT c FROM Category c WHERE c.isActive = false AND " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.displayOrder ASC")
    Page<Category> searchInactiveCategoriesByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * Tìm kiếm tất cả danh mục theo tên (tiếng Việt có dấu) với phân trang
     */
    @Query("SELECT c FROM Category c WHERE " +
           "(LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.displayOrder ASC")
    Page<Category> searchAllCategoriesByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
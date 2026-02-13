package com.greenconnect.greenconnect_api.repositories;

import com.greenconnect.greenconnect_api.entities.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierResponsitory2 extends JpaRepository<Supplier, UUID> {
	Optional<Supplier> findByName(String name);
	boolean existsByName(String name);

	@org.springframework.data.jpa.repository.Query("select s from Supplier s left join fetch s.supplierMedia left join fetch s.supplierCertifications where s.id = :id")
	Optional<Supplier> findByIdWithMediaAndCertifications(java.util.UUID id);
	
	/**
	 * Lấy tất cả nhà cung cấp đang hoạt động
	 */
	List<Supplier> findByIsActiveTrueOrderByCreatedAtDesc();
	
	/**
	 * Lấy tất cả nhà cung cấp không hoạt động
	 */
	List<Supplier> findByIsActiveFalseOrderByCreatedAtDesc();
	
	/**
	 * Lấy tất cả nhà cung cấp sắp xếp theo thời gian tạo
	 */
	List<Supplier> findAllByOrderByCreatedAtDesc();
	
	/**
	 * Lấy nhà cung cấp active với phân trang
	 */
	Page<Supplier> findByIsActiveTrueOrderByCreatedAtDesc(Pageable pageable);
	
	/**
	 * Lấy nhà cung cấp inactive với phân trang
	 */
	Page<Supplier> findByIsActiveFalseOrderByCreatedAtDesc(Pageable pageable);
	
	/**
	 * Lấy tất cả nhà cung cấp với phân trang
	 */
	Page<Supplier> findAllByOrderByCreatedAtDesc(Pageable pageable);
	
	/**
	 * Tìm kiếm nhà cung cấp active theo tên (tiếng Việt có dấu) với phân trang
	 */
	@Query("SELECT s FROM Supplier s WHERE s.isActive = true AND " +
	       "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
	       "ORDER BY s.createdAt DESC")
	Page<Supplier> searchActiveSuppliersbyKeyword(@Param("keyword") String keyword, Pageable pageable);
	
	/**
	 * Tìm kiếm nhà cung cấp inactive theo tên (tiếng Việt có dấu) với phân trang
	 */
	@Query("SELECT s FROM Supplier s WHERE s.isActive = false AND " +
	       "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
	       "ORDER BY s.createdAt DESC")
	Page<Supplier> searchInactiveSuppliersByKeyword(@Param("keyword") String keyword, Pageable pageable);
	
	/**
	 * Tìm kiếm tất cả nhà cung cấp theo tên (tiếng Việt có dấu) với phân trang
	 */
	@Query("SELECT s FROM Supplier s WHERE " +
	       "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
	       "LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
	       "ORDER BY s.createdAt DESC")
	Page<Supplier> searchAllSuppliersByKeyword(@Param("keyword") String keyword, Pageable pageable);
}

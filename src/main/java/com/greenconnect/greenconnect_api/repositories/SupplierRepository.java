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

import com.greenconnect.greenconnect_api.entities.Supplier;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, UUID>, JpaSpecificationExecutor<Supplier> {
    
    // Kiểm tra tồn tại theo tên
    boolean existsByName(String name);
    
    // Kiểm tra tồn tại theo email
    boolean existsByEmail(String email);
    
    boolean existsById(UUID id);
    // Tìm theo tên (tìm kiếm chính xác)
    Optional<Supplier> findByName(String name);
    
    // Tìm theo email
    Optional<Supplier> findByEmail(String email);

    Optional<Supplier> findById(UUID id);
    // Tìm tất cả nhà cung cấp đang hoạt động
    List<Supplier> findByIsActiveTrue();
    
    // Tìm nhà cung cấp theo trạng thái với phân trang
    Page<Supplier> findByIsActive(Boolean isActive, Pageable pageable);
    
    // Tìm kiếm theo tên chứa keyword (không phân biệt hoa thường)
    @Query("SELECT s FROM Supplier s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> findByNameContainingIgnoreCase(@Param("keyword") String keyword);
    
    // Tìm kiếm nhà cung cấp đang hoạt động theo tên
    @Query("SELECT s FROM Supplier s WHERE s.isActive = true AND LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Supplier> findActiveSuppliersByNameContaining(@Param("keyword") String keyword);
    
    // Đếm số nhà cung cấp đang hoạt động
    long countByIsActiveTrue();
    
    // Đếm số nhà cung cấp theo trạng thái
    long countByIsActive(Boolean isActive);
}
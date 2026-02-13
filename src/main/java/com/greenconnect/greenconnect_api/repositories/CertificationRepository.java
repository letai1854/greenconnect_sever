package com.greenconnect.greenconnect_api.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.greenconnect.greenconnect_api.entities.Certification;

/**
 * Repository interface cho thực thể Certification.
 * 
 * <p>Quản lý danh sách các loại chứng nhận (ví dụ: VietGAP, GlobalG.A.P., Organic)
 * mà các nhà cung cấp có thể sở hữu. Bảng này do Admin quản lý.</p>
 */
@Repository
public interface CertificationRepository extends JpaRepository<Certification, UUID> {

    /**
     * Tìm kiếm một chứng nhận dựa trên tên của nó.
     * <p>Hữu ích để kiểm tra xem một chứng nhận đã tồn tại hay chưa trước khi
     * Admin tạo mới, tránh việc tạo ra các bản ghi trùng lặp.</p>
     *
     * @param name Tên của chứng nhận (ví dụ: "VietGAP").
     * @return một đối tượng {@link Optional} chứa {@link Certification} nếu tìm thấy.
     */
    // Optional<Certification> findByNameIgnoreCase(String name);

}